package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.EscrowStatus;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CredentialAssignmentRepository;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.EscrowStatusRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import com.group3.accounttrade.repository.PostCredentialRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyTransactionMigrationService implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final EscrowRepository escrowRepository;
    private final CredentialAssignmentRepository credentialAssignmentRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final EscrowStatusRepository escrowStatusRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostCredentialRepository postCredentialRepository;

    @Value("${legacy.transaction-migration.enabled:true}")
    private boolean migrationEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!migrationEnabled) {
            log.info("Legacy transaction migration is disabled");
            return;
        }

        List<LegacyTransactionRecord> records = loadLegacyTransactions();
        migrate(records);
    }

    @Transactional
    void migrate(List<LegacyTransactionRecord> records) {
        records.stream()
                .sorted(Comparator.comparing(LegacyTransactionRecord::createdAt))
                .forEach(this::migrateRecord);
    }

    private void migrateRecord(LegacyTransactionRecord record) {
        String orderNumber = buildLegacyOrderNumber(record.transactionId());
        if (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            return;
        }

        User buyer = userRepository.findById(record.buyerId()).orElse(null);
        Post post = postRepository.findById(record.postId()).orElse(null);
        User seller = record.sellerId() != null ? userRepository.findById(record.sellerId()).orElse(null) : null;

        if (buyer == null || post == null || seller == null) {
            log.warn("Skipping legacy transaction {} because buyer/post/seller could not be resolved", record.transactionId());
            return;
        }

        StatusBundle statuses = resolveStatuses(record);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .orderStatus(statuses.orderStatus())
                .subtotal(record.amount())
                .platformFee(record.fee())
                .totalAmount(record.amount())
                .buyerNotes(record.adminNote())
                .paidAt(statuses.paymentCreated() ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .completedAt(OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .cancelledAt(OrderStatus.REFUNDED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .credentialAssignedAt(record.credentialId() != null ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .confirmedAt(OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .confirmationDeadline(statuses.escrowCreated() && !OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                        ? record.createdAt().plusHours(24) : null)
                .build();
        order = orderRepository.save(order);
        backfillOrderTimestamps(order, record, statuses);

        PostCredential credential = record.credentialId() != null
                ? postCredentialRepository.findById(record.credentialId()).orElse(null)
                : null;

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .post(post)
                .seller(seller)
                .assignedCredential(credential)
                .unitPrice(record.amount())
                .platformFee(record.fee())
                .sellerEarnings(record.amount().subtract(record.fee() != null ? record.fee() : BigDecimal.ZERO))
                .postTitleSnapshot(post.getTitle())
                .itemStatus(resolveItemStatus(statuses.orderStatus(), credential))
                .credentialAssignedAt(credential != null ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .confirmedAt(OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .completedAt(OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .refundedAt(OrderStatus.REFUNDED.equals(statuses.orderStatus().getStatusName())
                        ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                .refundReason(OrderStatus.REFUNDED.equals(statuses.orderStatus().getStatusName()) ? "Migrated legacy refund" : null)
                .build();
        orderItem = orderItemRepository.save(orderItem);
        backfillOrderItemTimestamps(orderItem, record, statuses);

        if (statuses.paymentCreated()) {
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentStatus(statuses.paymentStatus())
                    .vnpayTxnRef("LEGACY-PAY-" + record.transactionId())
                    .amount(record.amount())
                    .currency(order.getCurrency())
                    .orderInfo("Migrated from legacy transaction #" + record.transactionId())
                    .failedAt(PaymentStatus.FAILED.equals(statuses.paymentStatus().getStatusName())
                            ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                    .errorMessage(PaymentStatus.FAILED.equals(statuses.paymentStatus().getStatusName())
                            ? "Migrated legacy failed/refunded state" : null)
                    .build();
            payment.setPaidAt(PaymentStatus.PAID.equals(statuses.paymentStatus().getStatusName())
                    || PaymentStatus.REFUNDED.equals(statuses.paymentStatus().getStatusName())
                    ? safeTimestamp(record.processedAt(), record.createdAt()) : null);
            paymentRepository.save(payment);
            backfillPaymentTimestamps(payment, record);
        }

        if (statuses.escrowCreated()) {
            BigDecimal fee = record.fee() != null ? record.fee() : BigDecimal.ZERO;
            Escrow escrow = Escrow.builder()
                    .order(order)
                    .escrowStatus(statuses.escrowStatus())
                    .amount(record.amount())
                    .platformFee(fee)
                    .sellerAmount(record.amount().subtract(fee).max(BigDecimal.ZERO))
                    .autoReleaseDeadline(statuses.escrowStatus() != null
                            && EscrowStatus.HOLDING.equals(statuses.escrowStatus().getStatusName())
                            ? record.createdAt().plusHours(24) : null)
                    .releasedAt(EscrowStatus.RELEASED.equals(statuses.escrowStatus().getStatusName())
                            ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                    .refundedAt(EscrowStatus.REFUNDED.equals(statuses.escrowStatus().getStatusName())
                            ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                    .statusReason("Migrated from legacy transaction #" + record.transactionId())
                    .build();
            escrowRepository.save(escrow);
            backfillEscrowTimestamps(escrow, record, statuses);
        }

        if (credential != null) {
            CredentialAssignment assignment = CredentialAssignment.builder()
                    .orderItem(orderItem)
                    .credential(credential)
                    .assignmentStatus(resolveAssignmentStatus(statuses.orderStatus()))
                    .deliveredAt(safeTimestamp(record.processedAt(), record.createdAt()))
                    .confirmedAt(OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName())
                            ? safeTimestamp(record.processedAt(), record.createdAt()) : null)
                    .statusReason("Migrated from legacy transaction #" + record.transactionId())
                    .build();
            credentialAssignmentRepository.save(assignment);
            backfillAssignmentTimestamps(assignment, record, statuses);
        }
    }

    private List<LegacyTransactionRecord> loadLegacyTransactions() {
        String sql = """
                SELECT t.transaction_id,
                       t.post_id,
                       t.buyer_id,
                       t.seller_id,
                       t.amount,
                       t.fee,
                       ts.status_name,
                       t.created_at,
                       t.processed_at,
                       t.admin_note,
                       pc.credential_id
                FROM Transactions t
                LEFT JOIN Transaction_Statuses ts ON ts.status_id = t.status_id
                LEFT JOIN Post_Credentials pc ON pc.sold_to_order_id = t.transaction_id
                """;

        try {
            return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapLegacyTransaction(resultSet));
        } catch (Exception exception) {
            log.info("Skipping legacy transaction migration because legacy tables are unavailable: {}", exception.getMessage());
            return List.of();
        }
    }

    private LegacyTransactionRecord mapLegacyTransaction(ResultSet resultSet) throws SQLException {
        return LegacyTransactionRecord.builder()
                .transactionId(resultSet.getInt("transaction_id"))
                .postId(resultSet.getInt("post_id"))
                .buyerId(resultSet.getInt("buyer_id"))
                .sellerId(resultSet.getObject("seller_id", Integer.class))
                .amount(resultSet.getBigDecimal("amount"))
                .fee(Optional.ofNullable(resultSet.getBigDecimal("fee")).orElse(BigDecimal.ZERO))
                .statusName(resultSet.getString("status_name"))
                .createdAt(resultSet.getTimestamp("created_at").toLocalDateTime())
                .processedAt(resultSet.getTimestamp("processed_at") != null
                        ? resultSet.getTimestamp("processed_at").toLocalDateTime()
                        : null)
                .adminNote(resultSet.getString("admin_note"))
                .credentialId(resultSet.getObject("credential_id", Integer.class))
                .build();
    }

    private StatusBundle resolveStatuses(LegacyTransactionRecord record) {
        String legacyStatus = record.statusName() != null ? record.statusName().trim().toUpperCase() : "";
        boolean hasCredential = record.credentialId() != null;

        return switch (legacyStatus) {
            case "REFUNDED" -> new StatusBundle(
                    requiredOrderStatus(OrderStatus.REFUNDED),
                    requiredPaymentStatus(PaymentStatus.REFUNDED),
                    requiredEscrowStatus(EscrowStatus.REFUNDED),
                    true,
                    true
            );
            case "COMPLETED" -> new StatusBundle(
                    requiredOrderStatus(OrderStatus.COMPLETED),
                    requiredPaymentStatus(PaymentStatus.PAID),
                    requiredEscrowStatus(EscrowStatus.RELEASED),
                    true,
                    true
            );
            case "HOLDING" -> new StatusBundle(
                    requiredOrderStatus(OrderStatus.AWAITING_BUYER_CONFIRMATION),
                    requiredPaymentStatus(PaymentStatus.PAID),
                    requiredEscrowStatus(EscrowStatus.HOLDING),
                    true,
                    true
            );
            case "PENDING" -> hasCredential
                    ? new StatusBundle(
                            requiredOrderStatus(OrderStatus.AWAITING_BUYER_CONFIRMATION),
                            requiredPaymentStatus(PaymentStatus.PAID),
                            requiredEscrowStatus(EscrowStatus.HOLDING),
                            true,
                            true
                    )
                    : new StatusBundle(
                            requiredOrderStatus(OrderStatus.AWAITING_PAYMENT),
                            requiredPaymentStatus(PaymentStatus.INITIATED),
                            null,
                            true,
                            false
                    );
            default -> new StatusBundle(
                    requiredOrderStatus(OrderStatus.AWAITING_PAYMENT),
                    requiredPaymentStatus(PaymentStatus.INITIATED),
                    null,
                    true,
                    false
            );
        };
    }

    private String resolveItemStatus(OrderStatus status, PostCredential credential) {
        if (OrderStatus.REFUNDED.equals(status.getStatusName())) {
            return OrderItem.STATUS_REFUNDED;
        }
        if (OrderStatus.COMPLETED.equals(status.getStatusName())) {
            return OrderItem.STATUS_COMPLETED;
        }
        if (credential != null) {
            return OrderItem.STATUS_DELIVERED;
        }
        return OrderItem.STATUS_PENDING;
    }

    private String resolveAssignmentStatus(OrderStatus status) {
        if (OrderStatus.REFUNDED.equals(status.getStatusName())) {
            return CredentialAssignment.STATUS_REVOKED;
        }
        if (OrderStatus.COMPLETED.equals(status.getStatusName())) {
            return CredentialAssignment.STATUS_CONFIRMED;
        }
        return CredentialAssignment.STATUS_DELIVERED;
    }

    private String buildLegacyOrderNumber(Integer transactionId) {
        return "LEGACY-TRX-" + transactionId;
    }

    private OrderStatus requiredOrderStatus(String statusName) {
        return orderStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new IllegalStateException("Missing order status " + statusName));
    }

    private PaymentStatus requiredPaymentStatus(String statusName) {
        return paymentStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new IllegalStateException("Missing payment status " + statusName));
    }

    private EscrowStatus requiredEscrowStatus(String statusName) {
        return escrowStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new IllegalStateException("Missing escrow status " + statusName));
    }

    private LocalDateTime safeTimestamp(LocalDateTime preferred, LocalDateTime fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void backfillOrderTimestamps(Order order, LegacyTransactionRecord record, StatusBundle statuses) {
        if (order.getOrderId() == null) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE orders
                        SET created_at = ?,
                            updated_at = ?,
                            paid_at = ?,
                            completed_at = ?,
                            cancelled_at = ?,
                            confirmation_deadline = ?
                        WHERE order_id = ?
                        """,
                record.createdAt(),
                safeTimestamp(record.processedAt(), record.createdAt()),
                statuses.paymentCreated() ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                OrderStatus.REFUNDED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                statuses.escrowCreated() && !OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName()) ? record.createdAt().plusHours(24) : null,
                order.getOrderId());
    }

    private void backfillOrderItemTimestamps(OrderItem orderItem, LegacyTransactionRecord record, StatusBundle statuses) {
        if (orderItem.getOrderItemId() == null) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE order_items
                        SET created_at = ?,
                            updated_at = ?,
                            credential_assigned_at = ?,
                            confirmed_at = ?,
                            completed_at = ?,
                            refunded_at = ?
                        WHERE order_item_id = ?
                        """,
                record.createdAt(),
                safeTimestamp(record.processedAt(), record.createdAt()),
                orderItem.getAssignedCredential() != null ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                OrderStatus.REFUNDED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                orderItem.getOrderItemId());
    }

    private void backfillPaymentTimestamps(Payment payment, LegacyTransactionRecord record) {
        if (payment.getPaymentId() == null) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE payments
                        SET created_at = ?,
                            updated_at = ?,
                            paid_at = ?,
                            failed_at = ?
                        WHERE payment_id = ?
                        """,
                record.createdAt(),
                safeTimestamp(record.processedAt(), record.createdAt()),
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getPaymentId());
    }

    private void backfillEscrowTimestamps(Escrow escrow, LegacyTransactionRecord record, StatusBundle statuses) {
        if (escrow.getEscrowId() == null) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE escrows
                        SET created_at = ?,
                            updated_at = ?,
                            released_at = ?,
                            refunded_at = ?,
                            auto_release_deadline = ?
                        WHERE escrow_id = ?
                        """,
                record.createdAt(),
                safeTimestamp(record.processedAt(), record.createdAt()),
                EscrowStatus.RELEASED.equals(statuses.escrowStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                EscrowStatus.REFUNDED.equals(statuses.escrowStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                EscrowStatus.HOLDING.equals(statuses.escrowStatus().getStatusName()) ? record.createdAt().plusHours(24) : null,
                escrow.getEscrowId());
    }

    private void backfillAssignmentTimestamps(CredentialAssignment assignment, LegacyTransactionRecord record, StatusBundle statuses) {
        if (assignment.getAssignmentId() == null) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE credential_assignments
                        SET assigned_at = ?,
                            delivered_at = ?,
                            confirmed_at = ?
                        WHERE assignment_id = ?
                        """,
                record.createdAt(),
                safeTimestamp(record.processedAt(), record.createdAt()),
                OrderStatus.COMPLETED.equals(statuses.orderStatus().getStatusName()) ? safeTimestamp(record.processedAt(), record.createdAt()) : null,
                assignment.getAssignmentId());
    }

    @Builder
    record LegacyTransactionRecord(
            Integer transactionId,
            Integer postId,
            Integer buyerId,
            Integer sellerId,
            BigDecimal amount,
            BigDecimal fee,
            String statusName,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            String adminNote,
            Integer credentialId
    ) {
    }

    private record StatusBundle(
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            EscrowStatus escrowStatus,
            boolean paymentCreated,
            boolean escrowCreated
    ) {
    }
}
