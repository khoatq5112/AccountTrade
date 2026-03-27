package com.group3.accounttrade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.accounttrade.dto.DisputeDTO;
import com.group3.accounttrade.dto.DisputeDetailDTO;
import com.group3.accounttrade.dto.DisputeEventDTO;
import com.group3.accounttrade.dto.DisputeMessageDTO;
import com.group3.accounttrade.entity.AdminReview;
import com.group3.accounttrade.entity.AuditLog;
import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.DisputeMessage;
import com.group3.accounttrade.entity.DisputeStatus;
import com.group3.accounttrade.entity.Notification;
import com.group3.accounttrade.entity.NotificationPreference;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.RefundRequest;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.AdminReviewRepository;
import com.group3.accounttrade.repository.AuditLogRepository;
import com.group3.accounttrade.repository.CredentialAssignmentRepository;
import com.group3.accounttrade.repository.DisputeMessageRepository;
import com.group3.accounttrade.repository.DisputeRepository;
import com.group3.accounttrade.repository.DisputeStatusRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PostCredentialRepository;
import com.group3.accounttrade.repository.RefundRequestRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String LEGACY_IMAGE_MARKER = "[Bằng chứng hình ảnh]:";
    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository disputeMessageRepository;
    private final DisputeStatusRepository disputeStatusRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final EscrowService escrowService;
    private final CredentialService credentialService;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final AdminReviewRepository adminReviewRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final CredentialAssignmentRepository credentialAssignmentRepository;
    private final PostCredentialRepository postCredentialRepository;

    @Value("${dispute.sla-timeout-hours:48}")
    private long disputeSlaTimeoutHours;

    @Value("${escrow.verification-timeout-hours:24}")
    private long verificationTimeoutHours;

    public static final String STATUS_OPENED = "OPENED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String REASON_INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
    public static final String REASON_CREDENTIAL_CHANGED = "CREDENTIAL_CHANGED";
    public static final String REASON_NOT_AS_DESCRIBED = "NOT_AS_DESCRIBED";
    public static final String REASON_NO_CREDENTIAL_RECEIVED = "NO_CREDENTIAL_RECEIVED";
    public static final String REASON_OTHER = "OTHER";

    @Transactional
    public Dispute openDispute(Long orderId, Integer buyerId, String reason, String description) {
        return openDispute(orderId, buyerId, reason, description, List.of());
    }

    @Transactional
    public Dispute openDispute(Long orderId, Integer buyerId, String reason, String description, List<String> imageUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getBuyer().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can open a dispute for this order");
        }

        String orderStatus = order.getOrderStatus().getStatusName();
        if (!OrderStatus.CREDENTIAL_ASSIGNED.equals(orderStatus)
                && !OrderStatus.AWAITING_BUYER_CONFIRMATION.equals(orderStatus)) {
            throw new IllegalStateException("Order cannot be disputed in status: " + orderStatus);
        }

        if (!disputeRepository.findByOrder(order).isEmpty()) {
            throw new IllegalStateException("A dispute already exists for this order");
        }

        Dispute dispute = Dispute.builder()
                .disputeNumber(generateDisputeNumber())
                .order(order)
                .openedBy(order.getBuyer())
                .respondent(getSellerFromOrder(order))
                .disputeStatus(getDisputeStatus(STATUS_OPENED))
                .disputeType(reason)
                .reason(reason)
                .buyerEvidence(encodeEvidence(description, imageUrls))
                .sellerResponseDeadline(LocalDateTime.now().plusHours(disputeSlaTimeoutHours))
                .build();
        disputeRepository.save(dispute);

        order.setOrderStatus(getOrderStatus(OrderStatus.DISPUTED));
        orderRepository.save(order);

        try {
            escrowService.freezeEscrow(orderId, "Dispute opened: " + reason);
        } catch (IllegalArgumentException e) {
            log.warn("[DISPUTE] No escrow found for order {}, continue opening dispute: {}", orderId, e.getMessage());
        }

        try {
            credentialService.markCredentialsAsDisputed(order, reason);
        } catch (Exception e) {
            log.warn("[DISPUTE] Could not mark credentials as disputed for order {}: {}", orderId, e.getMessage());
        }

        createAuditLog(order.getBuyer(), "DISPUTE_OPENED", "Dispute", dispute.getDisputeId(),
                String.format("Dispute opened for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_BUYER);

        notifySeller(dispute, "Dispute Opened",
                String.format("Buyer opened dispute %s for order %s.", dispute.getDisputeNumber(), order.getOrderNumber()));
        notifyBuyer(dispute, "Dispute Opened",
                String.format("Your dispute %s has been created. Seller has %d hours to respond before TrustBridge can intervene.",
                        dispute.getDisputeNumber(), disputeSlaTimeoutHours));
        notifyAdmins(dispute, "Dispute Monitoring",
                String.format("Dispute %s was opened for order %s. Admin review becomes available after escalation or seller timeout.",
                        dispute.getDisputeNumber(), order.getOrderNumber()));

        return dispute;
    }

    @Transactional
    public DisputeMessage addMessage(Long disputeId, Integer userId, String message, String attachmentUrl) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        boolean isBuyer = dispute.getOpenedBy().getUserId().equals(userId);
        boolean isSeller = dispute.getRespondent().getUserId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new IllegalStateException("Only buyer or seller can add messages to this dispute");
        }

        DisputeMessage disputeMessage = DisputeMessage.builder()
                .dispute(dispute)
                .sender(getUserById(userId))
                .senderRole(isBuyer ? DisputeMessage.ROLE_BUYER : DisputeMessage.ROLE_SELLER)
                .content(message)
                .attachmentPath(attachmentUrl)
                .hasAttachment(attachmentUrl != null && !attachmentUrl.isBlank())
                .build();
        disputeMessageRepository.save(disputeMessage);

        dispute.setUpdatedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        return disputeMessage;
    }

    @Transactional
    public void escalateDispute(Long disputeId, Integer buyerId, String reason) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can escalate this dispute");
        }
        ensureOpened(dispute, "Only opened disputes can be escalated");

        String escalationReason = normalizeText(reason, "Buyer requested admin review");
        dispute.setBuyerEscalatedAt(LocalDateTime.now());
        dispute.setBuyerEscalationReason(escalationReason);
        disputeRepository.save(dispute);

        createAdminReviewRecord(disputeId, escalationReason);
        createAuditLog(dispute.getOpenedBy(), "DISPUTE_ESCALATED", "Dispute", disputeId,
                String.format("Buyer escalated dispute %s. Reason: %s", dispute.getDisputeNumber(), escalationReason),
                AuditLog.ROLE_BUYER);

        notifySeller(dispute, "Dispute Escalated",
                String.format("Buyer escalated dispute %s to admin review.", dispute.getDisputeNumber()));
        notifyAdmins(dispute, "Dispute Escalated",
                String.format("Dispute %s is ready for admin review.", dispute.getDisputeNumber()));
    }

    @Transactional
    public void resolveInBuyerFavor(Long disputeId, Integer adminId, String resolution, BigDecimal refundAmount) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        ensureUnderReview(dispute);

        User admin = requireUser(adminId, "Admin not found: " + adminId);
        Order order = requireOrder(dispute);

        dispute.setDisputeStatus(getDisputeStatus(STATUS_RESOLVED));
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionNotes(resolution);
        dispute.setResolutionType("BUYER_FAVOR");
        dispute.setResolvedByAdmin(admin);
        disputeRepository.save(dispute);

        escrowService.refundEscrow(order.getOrderId(), refundAmount, resolution, adminId);

        RefundRequest refundRequest = RefundRequest.builder()
                .refundNumber(generateRefundNumber())
                .order(order)
                .dispute(dispute)
                .requestedBy(order.getBuyer())
                .refundType(RefundRequest.TYPE_DISPUTE_RESOLUTION)
                .originalAmount(order.getTotalAmount())
                .refundAmount(refundAmount != null ? refundAmount : order.getTotalAmount())
                .reason(resolution)
                .status(RefundRequest.STATUS_APPROVED)
                .processedByAdmin(admin)
                .processedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedByAdmin(admin)
                .build();
        refundRequestRepository.save(refundRequest);

        order.setOrderStatus(getOrderStatus(OrderStatus.REFUNDED));
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason("Dispute resolved in buyer favor: " + resolution);
        orderRepository.save(order);

        credentialService.revokeCredentials(order, "Dispute resolved in buyer favor");

        createAuditLog(admin, "DISPUTE_RESOLVED_BUYER", "Dispute", disputeId,
                String.format("Dispute %s resolved in buyer favor. Refund: %s",
                        dispute.getDisputeNumber(), refundAmount != null ? refundAmount : order.getTotalAmount()),
                AuditLog.ROLE_ADMIN);

        notifyBuyer(dispute, "Dispute Resolved",
                String.format("Dispute %s was resolved in your favor. Refund has been processed.", dispute.getDisputeNumber()));
        notifySeller(dispute, "Dispute Resolved",
                String.format("Dispute %s was resolved in buyer's favor.", dispute.getDisputeNumber()));
    }

    @Transactional
    public void resolveInSellerFavor(Long disputeId, Integer adminId, String resolution) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        ensureUnderReview(dispute);

        User admin = requireUser(adminId, "Admin not found: " + adminId);
        Order order = requireOrder(dispute);

        dispute.setDisputeStatus(getDisputeStatus(STATUS_RESOLVED));
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionNotes(resolution);
        dispute.setResolutionType("SELLER_FAVOR");
        dispute.setResolvedByAdmin(admin);
        disputeRepository.save(dispute);

        escrowService.unfreezeEscrow(order.getOrderId(), "Dispute resolved in seller favor");
        escrowService.releaseEscrow(order.getOrderId(), "Dispute resolved in seller favor: " + resolution, adminId);

        createAuditLog(admin, "DISPUTE_RESOLVED_SELLER", "Dispute", disputeId,
                String.format("Dispute %s resolved in seller favor.", dispute.getDisputeNumber()),
                AuditLog.ROLE_ADMIN);

        notifyBuyer(dispute, "Dispute Resolved",
                String.format("Dispute %s was resolved in seller's favor.", dispute.getDisputeNumber()));
        notifySeller(dispute, "Dispute Resolved",
                String.format("Dispute %s was resolved in your favor.", dispute.getDisputeNumber()));
    }

    @Transactional
    public void cancelDispute(Long disputeId, Integer buyerId, String reason) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can cancel this dispute");
        }
        ensureOpened(dispute, "Only opened disputes can be cancelled");

        Order order = requireOrder(dispute);
        dispute.setDisputeStatus(getDisputeStatus(STATUS_CANCELLED));
        dispute.setResolutionNotes(reason);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionType("CANCELLED");
        disputeRepository.save(dispute);

        escrowService.unfreezeEscrow(order.getOrderId(), "Dispute cancelled by buyer");
        order.setOrderStatus(getOrderStatus(OrderStatus.AWAITING_BUYER_CONFIRMATION));
        order.setConfirmationDeadline(LocalDateTime.now().plusHours(verificationTimeoutHours));
        orderRepository.save(order);

        createAuditLog(dispute.getOpenedBy(), "DISPUTE_CANCELLED", "Dispute", disputeId,
                String.format("Dispute %s cancelled by buyer. Reason: %s", dispute.getDisputeNumber(), reason),
                AuditLog.ROLE_BUYER);

        notifySeller(dispute, "Dispute Cancelled",
                String.format("Buyer cancelled dispute %s.", dispute.getDisputeNumber()));
    }

    public List<Dispute> getDisputesByBuyer(Integer buyerId) {
        User buyer = getUserById(buyerId);
        return buyer == null ? List.of() : disputeRepository.findByOpenedByOrderByOpenedAtDesc(buyer);
    }

    public List<Dispute> getDisputesBySeller(Integer sellerId) {
        User seller = getUserById(sellerId);
        return seller == null ? List.of() : disputeRepository.findByRespondentOrderByOpenedAtDesc(seller);
    }

    public List<Dispute> getOpenDisputes() {
        List<Dispute> disputes = new ArrayList<>();
        disputeStatusRepository.findByStatusName(STATUS_OPENED)
                .ifPresent(status -> disputes.addAll(disputeRepository.findByDisputeStatus(status)));
        disputeStatusRepository.findByStatusName(STATUS_UNDER_REVIEW)
                .ifPresent(status -> disputes.addAll(disputeRepository.findByDisputeStatus(status)));
        return disputes;
    }

    public List<DisputeMessage> getDisputeMessages(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .map(disputeMessageRepository::findByDisputeOrderByCreatedAtAsc)
                .orElse(List.of());
    }

    public Optional<Dispute> getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId);
    }

    public Optional<Dispute> getDisputeByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> disputeRepository.findByOrder(order).stream().findFirst());
    }

    public Page<Dispute> getDisputesByBuyerPaginated(Integer buyerId, String status, Pageable pageable) {
        User buyer = getUserById(buyerId);
        return buyer == null ? Page.empty(pageable) : disputeRepository.findByBuyerWithStatus(buyer, status, pageable);
    }

    public Page<Dispute> getDisputesBySellerPaginated(Integer sellerId, String status, Pageable pageable) {
        User seller = getUserById(sellerId);
        return seller == null ? Page.empty(pageable) : disputeRepository.findBySellerWithStatus(seller, status, pageable);
    }

    public Page<DisputeDTO> getAllDisputesPaginated(String status, Pageable pageable) {
        return disputeRepository.findAllWithStatus(status, pageable).map(this::mapToDisputeDTO);
    }

    public DisputeDetailDTO getDisputeDetailDTO(Long disputeId) {
        return disputeRepository.findById(disputeId).map(this::mapToDetailDTO).orElse(null);
    }

    @Transactional
    public void submitSellerResponse(Long disputeId,
                                     Integer sellerId,
                                     String response,
                                     String evidenceText,
                                     List<String> evidenceImages,
                                     String proposalType,
                                     String proposalNote,
                                     Integer proposalCredentialId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getRespondent().getUserId().equals(sellerId)) {
            throw new IllegalStateException("Only the seller can respond to this dispute");
        }
        ensureOpened(dispute, "Dispute is no longer open for seller negotiation");

        String normalizedResponse = normalizeRequiredText(response, "Seller response is required");
        String normalizedProposalType = normalizeSellerProposalType(proposalType);
        String normalizedProposalNote = normalizeText(proposalNote, null);

        dispute.setSellerResponse(normalizedResponse);
        dispute.setSellerEvidence(encodeEvidence(evidenceText, evidenceImages));
        dispute.setSellerRespondedAt(LocalDateTime.now());
        dispute.setSellerProposalType(normalizedProposalType);
        dispute.setSellerProposalNote(normalizedProposalNote);
        dispute.setSellerProposedAt(normalizedProposalType != null ? LocalDateTime.now() : null);

        if (Dispute.PROPOSAL_REPLACEMENT.equals(normalizedProposalType)) {
            if (dispute.getSellerProposalCredentialId() != null
                    && !Objects.equals(dispute.getSellerProposalCredentialId(), proposalCredentialId)) {
                throw new IllegalStateException("Replacement credential has already been selected for this dispute");
            }
            if (dispute.getSellerProposalCredentialId() == null) {
                PostCredential replacementCredential = reserveReplacementCredential(dispute, sellerId, proposalCredentialId, normalizedProposalNote);
                dispute.setSellerProposalCredentialId(replacementCredential.getCredentialId());
            }
        } else {
            if (dispute.getSellerProposalCredentialId() != null) {
                throw new IllegalStateException("Replacement proposal has already been created and cannot be changed");
            }
            dispute.setSellerProposalCredentialId(null);
        }

        disputeRepository.save(dispute);

        createAuditLog(dispute.getRespondent(), "SELLER_RESPONDED", "Dispute", disputeId,
                String.format("Seller responded to dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_SELLER);

        notifyBuyer(dispute, "Seller Responded",
                String.format("Seller updated the response for dispute %s.", dispute.getDisputeNumber()));
        notifyAdmins(dispute, "Seller Responded",
                String.format("Seller updated the response for dispute %s.", dispute.getDisputeNumber()));
    }

    public List<PostCredential> getReplacementCandidates(Long disputeId, Integer sellerId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getRespondent().getUserId().equals(sellerId)) {
            throw new IllegalStateException("Only the seller can view replacement candidates");
        }
        if (!isReplacementEligible(dispute)) {
            return List.of();
        }

        Integer postId = getSingleOrderItem(dispute).getPost().getPostId();
        return postCredentialRepository.findAllForManagementByPostId(postId).stream()
                .filter(pc -> pc.getCredentialStatus() != null
                        && CredentialService.STATUS_AVAILABLE.equalsIgnoreCase(pc.getCredentialStatus().getStatusName()))
                .toList();
    }

    @Transactional
    public void acceptSellerRefund(Long disputeId, Integer buyerId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can accept this refund proposal");
        }
        ensureOpened(dispute, "Dispute must still be open to accept seller refund");
        if (!Dispute.PROPOSAL_REFUND.equals(dispute.getSellerProposalType())) {
            throw new IllegalStateException("Seller has not proposed a refund for this dispute");
        }

        Order order = requireOrder(dispute);
        String resolution = normalizeText(dispute.getSellerProposalNote(),
                "Buyer accepted seller refund proposal");

        dispute.setDisputeStatus(getDisputeStatus(STATUS_RESOLVED));
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionType("BUYER_FAVOR");
        dispute.setResolutionNotes(resolution);
        disputeRepository.save(dispute);

        escrowService.refundEscrow(order.getOrderId(), null, resolution, null);
        credentialService.revokeCredentials(order, "Buyer accepted seller refund proposal");

        createAuditLog(dispute.getOpenedBy(), "BUYER_ACCEPTED_REFUND", "Dispute", disputeId,
                String.format("Buyer accepted seller refund proposal for dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_BUYER);

        notifySeller(dispute, "Refund Proposal Accepted",
                String.format("Buyer accepted your refund proposal for dispute %s.", dispute.getDisputeNumber()));
        notifyBuyer(dispute, "Refund Processed",
                String.format("Dispute %s was settled by seller refund.", dispute.getDisputeNumber()));
    }

    @Transactional
    public void acceptSellerReplacement(Long disputeId, Integer buyerId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can accept this replacement proposal");
        }
        ensureOpened(dispute, "Dispute must still be open to accept seller replacement");
        if (!Dispute.PROPOSAL_REPLACEMENT.equals(dispute.getSellerProposalType())
                || dispute.getSellerProposalCredentialId() == null) {
            throw new IllegalStateException("Seller has not provided a valid replacement proposal");
        }

        Order order = requireOrder(dispute);
        dispute.setBuyerEscalatedAt(null);
        dispute.setBuyerEscalationReason(null);
        dispute.setAdminReviewStartedAt(null);
        dispute.setResolvedByAdmin(null);
        dispute.setResolutionType(Dispute.RESOLUTION_REPLACEMENT);
        dispute.setResolutionNotes("Buyer accepted seller replacement proposal and is re-checking the new credential");
        dispute.setResolvedAt(null);
        disputeRepository.save(dispute);

        escrowService.unfreezeEscrow(order.getOrderId(), "Buyer accepted seller replacement proposal");
        order.setOrderStatus(getOrderStatus(OrderStatus.AWAITING_BUYER_CONFIRMATION));
        order.setConfirmationDeadline(LocalDateTime.now().plusHours(verificationTimeoutHours));
        orderRepository.save(order);
        addDisputeActivityMessage(dispute, dispute.getOpenedBy(), DisputeMessage.ROLE_BUYER,
                "Buyer accepted the seller replacement proposal and is verifying the new credential.");

        createAuditLog(dispute.getOpenedBy(), "BUYER_ACCEPTED_REPLACEMENT", "Dispute", disputeId,
                String.format("Buyer accepted seller replacement proposal for dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_BUYER);

        notifySeller(dispute, "Replacement Accepted",
                String.format("Buyer accepted your replacement proposal for dispute %s.", dispute.getDisputeNumber()));
        notifyBuyer(dispute, "Replacement Accepted",
                String.format("Replacement was accepted for dispute %s. Please verify the new credential before confirming receipt.",
                        dispute.getDisputeNumber()));
    }

    @Transactional
    public void reportReplacementFailure(Long disputeId, Integer buyerId, String reason) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can report replacement failure");
        }
        ensureOpened(dispute, "Dispute is no longer open for replacement follow-up");
        if (!Dispute.PROPOSAL_REPLACEMENT.equals(dispute.getSellerProposalType())
                || dispute.getSellerProposalCredentialId() == null) {
            throw new IllegalStateException("Seller has not provided a valid replacement proposal");
        }

        Order order = requireOrder(dispute);
        String escalationReason = normalizeText(reason,
                "Replacement credential still does not work. Buyer requested admin refund review.");

        dispute.setBuyerEscalatedAt(LocalDateTime.now());
        dispute.setBuyerEscalationReason(escalationReason);
        dispute.setDisputeStatus(getDisputeStatus(STATUS_UNDER_REVIEW));
        dispute.setAdminReviewStartedAt(LocalDateTime.now());
        dispute.setResolutionNotes(escalationReason);
        disputeRepository.save(dispute);

        order.setOrderStatus(getOrderStatus(OrderStatus.DISPUTED));
        order.setConfirmationDeadline(null);
        orderRepository.save(order);

        if (hasOrderStatus(order, OrderStatus.AWAITING_BUYER_CONFIRMATION)) {
            escrowService.freezeEscrow(order.getOrderId(), "Replacement credential failed. Escalated for admin refund review");
        }
        credentialService.markCredentialsAsDisputed(order, escalationReason);
        createAdminReviewRecord(disputeId, escalationReason);
        addDisputeActivityMessage(dispute, dispute.getOpenedBy(), DisputeMessage.ROLE_BUYER, escalationReason);

        createAuditLog(dispute.getOpenedBy(), "REPLACEMENT_FAILED_ESCALATED", "Dispute", disputeId,
                String.format("Buyer reported replacement failure for dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_BUYER);

        notifyBuyer(dispute, "Replacement Escalated",
                String.format("Dispute %s was moved to admin review after the replacement failed.", dispute.getDisputeNumber()));
        notifySeller(dispute, "Replacement Escalated",
                String.format("Buyer reported that the replacement still failed for dispute %s. Admin review has started.",
                        dispute.getDisputeNumber()));
        notifyAdmins(dispute, "Replacement Failure Review",
                String.format("Dispute %s requires admin refund review because the replacement credential still failed.",
                        dispute.getDisputeNumber()));
    }

    @Transactional
    public void assignAdmin(Long disputeId, Integer adminId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        User admin = requireUser(adminId, "Admin not found: " + adminId);
        dispute.setAssignedAdmin(admin);
        disputeRepository.save(dispute);

        createAuditLog(admin, "ADMIN_ASSIGNED", "Dispute", disputeId,
                String.format("Admin %s assigned to dispute %s", admin.getUsername(), dispute.getDisputeNumber()),
                AuditLog.ROLE_ADMIN);
    }

    @Transactional
    public void startReview(Long disputeId, Integer adminId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        ensureOpened(dispute, "Dispute must be OPENED to start review");

        String reviewBlockReason = getReviewBlockReason(dispute);
        if (reviewBlockReason != null) {
            throw new IllegalStateException(reviewBlockReason);
        }

        User admin = getUserById(adminId);
        dispute.setDisputeStatus(getDisputeStatus(STATUS_UNDER_REVIEW));
        dispute.setAdminReviewStartedAt(LocalDateTime.now());
        if (admin != null) {
            dispute.setAssignedAdmin(admin);
        }
        disputeRepository.save(dispute);

        createAdminReviewRecord(disputeId, buildReviewTriggerReason(dispute));
        createAuditLog(admin, "REVIEW_STARTED", "Dispute", disputeId,
                String.format("Admin review started for dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_ADMIN);

        notifyBuyer(dispute, "Dispute Under Review",
                String.format("Dispute %s is now under admin review.", dispute.getDisputeNumber()));
        notifySeller(dispute, "Dispute Under Review",
                String.format("Dispute %s is now under admin review.", dispute.getDisputeNumber()));
    }

    @Transactional
    public void addAdminNote(Long disputeId, Integer adminId, String notes) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        User admin = getUserById(adminId);

        String existingNotes = dispute.getAdminNotes();
        String newNotes = existingNotes != null && !existingNotes.isBlank()
                ? existingNotes + "\n[" + LocalDateTime.now() + "] " + notes
                : "[" + LocalDateTime.now() + "] " + notes;
        dispute.setAdminNotes(newNotes);
        disputeRepository.save(dispute);

        createAuditLog(admin, "ADMIN_NOTE_ADDED", "Dispute", disputeId,
                String.format("Admin note added to dispute %s", dispute.getDisputeNumber()),
                AuditLog.ROLE_ADMIN);
    }

    public List<DisputeEventDTO> getDisputeTimeline(Long disputeId) {
        List<DisputeEventDTO> timeline = new ArrayList<>();
        Dispute dispute = disputeRepository.findById(disputeId).orElse(null);
        if (dispute == null) {
            return timeline;
        }

        long eventId = 1L;
        timeline.add(new DisputeEventDTO(
                eventId++,
                DisputeEventDTO.TYPE_DISPUTE_OPENED,
                String.format("Buyer opened dispute. Reason code: %s", dispute.getReason()),
                AuditLog.ROLE_BUYER,
                dispute.getOpenedBy().getUsername(),
                dispute.getOpenedAt()
        ));

        if (dispute.getSellerRespondedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_SELLER_RESPONDED,
                    truncate("Seller response: " + dispute.getSellerResponse(), 120),
                    AuditLog.ROLE_SELLER,
                    dispute.getRespondent().getUsername(),
                    dispute.getSellerRespondedAt()
            ));
        }

        if (dispute.getSellerProposedAt() != null && dispute.getSellerProposalType() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_SELLER_RESPONDED,
                    String.format("Seller proposed %s", dispute.getSellerProposalType()),
                    AuditLog.ROLE_SELLER,
                    dispute.getRespondent().getUsername(),
                    dispute.getSellerProposedAt()
            ));
        }

        if (dispute.getBuyerEscalatedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_BUYER_MESSAGE,
                    "Buyer escalated dispute to admin",
                    AuditLog.ROLE_BUYER,
                    dispute.getOpenedBy().getUsername(),
                    dispute.getBuyerEscalatedAt()
            ));
        }

        if (dispute.getAssignedAdmin() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_ADMIN_ASSIGNED,
                    String.format("Admin %s assigned", dispute.getAssignedAdmin().getUsername()),
                    AuditLog.ROLE_ADMIN,
                    dispute.getAssignedAdmin().getUsername(),
                    dispute.getAdminReviewStartedAt() != null ? dispute.getAdminReviewStartedAt() : dispute.getOpenedAt()
            ));
        }

        if (dispute.getAdminReviewStartedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_REVIEW_STARTED,
                    "Admin review started",
                    AuditLog.ROLE_ADMIN,
                    dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getUsername() : "System",
                    dispute.getAdminReviewStartedAt()
            ));
        }

        if (dispute.getResolvedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_DISPUTE_RESOLVED,
                    truncate(String.format("Resolved: %s - %s", dispute.getResolutionType(), dispute.getResolutionNotes()), 120),
                    dispute.getResolvedByAdmin() != null ? AuditLog.ROLE_ADMIN : AuditLog.ROLE_BUYER,
                    dispute.getResolvedByAdmin() != null ? dispute.getResolvedByAdmin().getUsername() : dispute.getOpenedBy().getUsername(),
                    dispute.getResolvedAt()
            ));
        }

        List<DisputeMessage> messages = disputeMessageRepository.findByDisputeOrderByCreatedAtAsc(dispute);
        for (DisputeMessage msg : messages) {
            String eventType = DisputeMessage.ROLE_BUYER.equals(msg.getSenderRole())
                    ? DisputeEventDTO.TYPE_BUYER_MESSAGE
                    : DisputeMessage.ROLE_SELLER.equals(msg.getSenderRole())
                            ? DisputeEventDTO.TYPE_SELLER_MESSAGE
                            : DisputeEventDTO.TYPE_ADMIN_NOTE;
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    eventType,
                    truncate(msg.getContent(), 100),
                    msg.getSenderRole(),
                    msg.getSender().getUsername(),
                    msg.getCreatedAt()
            ));
        }

        timeline.sort(Comparator.comparing(DisputeEventDTO::timestamp));
        return timeline;
    }

    public DisputeStats getDisputeStats() {
        long total = disputeRepository.count();
        long opened = disputeRepository.countByStatusNameIn(List.of(STATUS_OPENED));
        long underReview = disputeRepository.countByStatusNameIn(List.of(STATUS_UNDER_REVIEW));
        long resolved = disputeRepository.countByStatusNameIn(List.of(STATUS_RESOLVED));
        return new DisputeStats(total, opened, underReview, resolved);
    }

    private DisputeDTO mapToDisputeDTO(Dispute dispute) {
        Order order = dispute.getOrder();
        return new DisputeDTO(
                dispute.getDisputeId(),
                dispute.getDisputeNumber(),
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getTotalAmount() : BigDecimal.ZERO,
                dispute.getReason(),
                dispute.getOpenedBy() != null ? dispute.getOpenedBy().getUsername() : "N/A",
                dispute.getRespondent() != null ? dispute.getRespondent().getUsername() : "N/A",
                dispute.getOpenedAt(),
                dispute.getDisputeStatus() != null ? dispute.getDisputeStatus().getStatusName() : "N/A",
                dispute.getDisputeType()
        );
    }

    private DisputeDetailDTO mapToDetailDTO(Dispute dispute) {
        Order order = requireOrder(dispute);
        EvidencePayload buyerEvidence = decodeEvidence(dispute.getBuyerEvidence());
        EvidencePayload sellerEvidence = decodeEvidence(dispute.getSellerEvidence());
        List<DisputeMessage> messages = disputeMessageRepository.findByDisputeOrderByCreatedAtAsc(dispute);

        List<DisputeMessageDTO> messageDTOs = messages.stream()
                .map(msg -> new DisputeMessageDTO(
                        msg.getMessageId(),
                        dispute.getDisputeId(),
                        msg.getSender().getUserId(),
                        msg.getSender().getUsername(),
                        msg.getSenderRole(),
                        msg.getContent(),
                        msg.getHasAttachment(),
                        msg.getAttachmentPath(),
                        msg.getCreatedAt(),
                        msg.getReadAt()
                ))
                .toList();

        return new DisputeDetailDTO(
                dispute.getDisputeId(),
                dispute.getDisputeNumber(),
                dispute.getDisputeStatus().getStatusName(),
                dispute.getDisputeType(),
                dispute.getReason(),
                dispute.getBuyerEvidence(),
                buyerEvidence.text(),
                buyerEvidence.images() == null ? List.of() : buyerEvidence.images(),
                dispute.getSellerResponse(),
                dispute.getSellerEvidence(),
                sellerEvidence.text(),
                sellerEvidence.images() == null ? List.of() : sellerEvidence.images(),
                dispute.getSellerProposalType(),
                dispute.getSellerProposalNote(),
                dispute.getSellerProposalCredentialId(),
                dispute.getSellerProposedAt(),
                dispute.getResolutionType(),
                dispute.getResolutionNotes(),
                dispute.getAdminNotes(),
                dispute.getOpenedAt(),
                dispute.getSellerRespondedAt(),
                dispute.getSellerResponseDeadline(),
                dispute.getAdminReviewStartedAt(),
                dispute.getResolvedAt(),
                canAdminReview(dispute),
                getReviewBlockReason(dispute),
                new DisputeDetailDTO.OrderInfo(
                        order.getOrderId(),
                        order.getOrderNumber(),
                        order.getTotalAmount(),
                        order.getOrderStatus().getStatusName(),
                        order.getCreatedAt()
                ),
                new DisputeDetailDTO.UserInfo(
                        dispute.getOpenedBy().getUserId(),
                        dispute.getOpenedBy().getUsername(),
                        dispute.getOpenedBy().getEmail()
                ),
                new DisputeDetailDTO.UserInfo(
                        dispute.getRespondent().getUserId(),
                        dispute.getRespondent().getUsername(),
                        dispute.getRespondent().getEmail()
                ),
                dispute.getAssignedAdmin() != null
                        ? new DisputeDetailDTO.UserSummary(dispute.getAssignedAdmin().getUserId(), dispute.getAssignedAdmin().getUsername())
                        : null,
                dispute.getResolvedByAdmin() != null
                        ? new DisputeDetailDTO.UserSummary(dispute.getResolvedByAdmin().getUserId(), dispute.getResolvedByAdmin().getUsername())
                        : null,
                messageDTOs,
                getDisputeTimeline(dispute.getDisputeId())
        );
    }

    private boolean canAdminReview(Dispute dispute) {
        return getReviewBlockReason(dispute) == null;
    }

    private String getReviewBlockReason(Dispute dispute) {
        if (!STATUS_OPENED.equals(getStatusName(dispute))) {
            return "Dispute is no longer in negotiation phase";
        }
        if (dispute.getBuyerEscalatedAt() != null) {
            return null;
        }
        if (dispute.getSellerProposalType() != null && !dispute.getSellerProposalType().isBlank()) {
            return null;
        }
        if (dispute.getSellerResponseDeadline() != null && dispute.getSellerResponseDeadline().isBefore(LocalDateTime.now())) {
            return null;
        }
        return "Admin review is blocked until buyer escalates, seller proposes a resolution, or seller response deadline expires";
    }

    private String buildReviewTriggerReason(Dispute dispute) {
        if (dispute.getBuyerEscalationReason() != null && !dispute.getBuyerEscalationReason().isBlank()) {
            return dispute.getBuyerEscalationReason();
        }
        if (dispute.getSellerProposalType() != null && !dispute.getSellerProposalType().isBlank()) {
            return "Seller proposed resolution: " + dispute.getSellerProposalType();
        }
        return "Seller response deadline exceeded";
    }

    private void addDisputeActivityMessage(Dispute dispute, User sender, String senderRole, String content) {
        disputeMessageRepository.save(DisputeMessage.builder()
                .dispute(dispute)
                .sender(sender)
                .senderRole(senderRole)
                .content(content)
                .hasAttachment(false)
                .build());
    }

    private boolean hasOrderStatus(Order order, String statusName) {
        return order.getOrderStatus() != null
                && statusName.equalsIgnoreCase(order.getOrderStatus().getStatusName());
    }

    private PostCredential reserveReplacementCredential(Dispute dispute,
                                                        Integer sellerId,
                                                        Integer replacementCredentialId,
                                                        String proposalNote) {
        if (!isReplacementEligible(dispute)) {
            throw new IllegalStateException("Replacement is only supported for single-item credential disputes");
        }
        if (replacementCredentialId == null) {
            throw new IllegalArgumentException("Replacement credential is required");
        }

        OrderItem orderItem = getSingleOrderItem(dispute);
        PostCredential replacementCredential = postCredentialRepository.findById(replacementCredentialId)
                .orElseThrow(() -> new IllegalArgumentException("Replacement credential not found: " + replacementCredentialId));

        if (!replacementCredential.getPost().getSeller().getUserId().equals(sellerId)
                || !replacementCredential.getPost().getPostId().equals(orderItem.getPost().getPostId())) {
            throw new IllegalStateException("Replacement credential must belong to the same seller and post");
        }
        if (replacementCredential.getCredentialStatus() == null
                || !CredentialService.STATUS_AVAILABLE.equalsIgnoreCase(replacementCredential.getCredentialStatus().getStatusName())) {
            throw new IllegalStateException("Replacement credential must be available");
        }

        CredentialAssignment activeAssignment = getActiveAssignment(orderItem);
        credentialService.provideReplacementCredential(activeAssignment.getAssignmentId(), replacementCredential,
                normalizeText(proposalNote, "Seller proposed replacement during dispute"));
        return replacementCredential;
    }

    private boolean isReplacementEligible(Dispute dispute) {
        if (!REASON_INVALID_CREDENTIAL.equals(dispute.getReason())
                && !REASON_CREDENTIAL_CHANGED.equals(dispute.getReason())) {
            return false;
        }
        Order order = dispute.getOrder();
        return order != null && order.getOrderItems() != null && order.getOrderItems().size() == 1;
    }

    private OrderItem getSingleOrderItem(Dispute dispute) {
        Order order = requireOrder(dispute);
        if (order.getOrderItems() == null || order.getOrderItems().size() != 1) {
            throw new IllegalStateException("Replacement is only supported for single-item orders");
        }
        return order.getOrderItems().get(0);
    }

    private CredentialAssignment getActiveAssignment(OrderItem orderItem) {
        return credentialAssignmentRepository.findByOrderItemOrderByAssignedAtDesc(orderItem).stream()
                .filter(assignment -> !CredentialService.ASSIGNMENT_REPLACED.equalsIgnoreCase(assignment.getAssignmentStatus()))
                .filter(assignment -> !CredentialService.ASSIGNMENT_REVOKED.equalsIgnoreCase(assignment.getAssignmentStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active credential assignment not found for replacement"));
    }

    private Dispute getDisputeOrThrow(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));
    }

    private User getUserById(Integer userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    private User requireUser(Integer userId, String message) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException(message);
        }
        return user;
    }

    private Order requireOrder(Dispute dispute) {
        if (dispute.getOrder() == null) {
            throw new IllegalStateException("Dispute has no associated order");
        }
        return dispute.getOrder();
    }

    private User getSellerFromOrder(Order order) {
        return order.getOrderItems().stream()
                .findFirst()
                .map(item -> item.getPost().getSeller())
                .orElseThrow(() -> new IllegalStateException("Order has no items"));
    }

    private DisputeStatus getDisputeStatus(String statusName) {
        return disputeStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new IllegalStateException(statusName + " status not found"));
    }

    private OrderStatus getOrderStatus(String statusName) {
        return orderStatusRepository.findByStatusName(statusName)
                .orElseThrow(() -> new IllegalStateException(statusName + " status not found"));
    }

    private void ensureOpened(Dispute dispute, String message) {
        if (!STATUS_OPENED.equals(getStatusName(dispute))) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureUnderReview(Dispute dispute) {
        if (!STATUS_UNDER_REVIEW.equals(getStatusName(dispute))) {
            throw new IllegalStateException("Dispute must be under admin review before resolution");
        }
    }

    private String getStatusName(Dispute dispute) {
        return dispute.getDisputeStatus() != null ? dispute.getDisputeStatus().getStatusName() : null;
    }

    private void createAuditLog(User user, String action, String entityType,
                                Long entityId, String description, String performerRole) {
        AuditLog auditLog = AuditLog.builder()
                .eventType(AuditLog.EVENT_DISPUTE)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(user)
                .performerRole(performerRole)
                .description(description)
                .success(true)
                .build();
        auditLogRepository.save(auditLog);
    }

    private void notifyBuyer(Dispute dispute, String title, String message) {
        notificationService.createNotification(
                dispute.getOpenedBy(),
                Notification.TYPE_DISPUTE,
                NotificationPreference.CATEGORY_DISPUTE,
                title,
                message,
                Notification.PRIORITY_HIGH,
                "DISPUTE",
                dispute.getDisputeId(),
                "/buyer/disputes/" + dispute.getDisputeId()
        );
    }

    private void notifySeller(Dispute dispute, String title, String message) {
        notificationService.createNotification(
                dispute.getRespondent(),
                Notification.TYPE_DISPUTE,
                NotificationPreference.CATEGORY_DISPUTE,
                title,
                message,
                Notification.PRIORITY_HIGH,
                "DISPUTE",
                dispute.getDisputeId(),
                "/seller/disputes/" + dispute.getDisputeId()
        );
    }

    private void notifyAdmins(Dispute dispute, String title, String message) {
        userRepository.findByRole_RoleNameIgnoreCase("Admin")
                .forEach(admin -> notificationService.createNotification(
                        admin,
                        Notification.TYPE_SYSTEM,
                        NotificationPreference.CATEGORY_DISPUTE,
                        title,
                        message,
                        Notification.PRIORITY_HIGH,
                        "DISPUTE",
                        dispute != null ? dispute.getDisputeId() : null,
                        dispute != null ? "/admin/disputes/" + dispute.getDisputeId() : "/admin/disputes"
                ));
    }

    private void createAdminReviewRecord(Long disputeId, String reason) {
        AdminReview adminReview = AdminReview.builder()
                .reviewType("DISPUTE")
                .entityType("Dispute")
                .entityId(disputeId)
                .issueDescription(reason)
                .status("PENDING")
                .build();
        adminReviewRepository.save(adminReview);
    }

    private String encodeEvidence(String text, List<String> images) {
        String normalizedText = normalizeText(text, "");
        List<String> normalizedImages = images == null ? List.of() : images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();

        if (normalizedText.isBlank() && normalizedImages.isEmpty()) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", normalizedText);
        payload.put("images", normalizedImages);
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode dispute evidence", e);
        }
    }

    private EvidencePayload decodeEvidence(String raw) {
        if (raw == null || raw.isBlank()) {
            return new EvidencePayload("", List.of());
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                Map<String, Object> payload = OBJECT_MAPPER.readValue(trimmed, new TypeReference<>() {});
                String text = payload.get("text") instanceof String value ? value : "";
                List<String> images = payload.get("images") instanceof List<?> values
                        ? values.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                        : List.of();
                List<String> normalizedImages = !images.isEmpty() ? images : extractImageUrls(trimmed);
                return new EvidencePayload(cleanEvidenceText(text), normalizedImages);
            } catch (Exception e) {
                log.warn("[DISPUTE] Failed to parse JSON evidence, falling back to legacy parser: {}", e.getMessage());
            }
        }

        String[] sections = trimmed.split("\\Q" + LEGACY_IMAGE_MARKER + "\\E", 2);
        String text = cleanEvidenceText(sections[0]);
        List<String> images = new ArrayList<>();
        if (sections.length > 1) {
            images.addAll(extractImageUrls(sections[1]));
        }
        if (images.isEmpty()) {
            images.addAll(extractImageUrls(trimmed));
        }

        if (images.isEmpty() && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            images.add(trimmed);
            text = "";
        }
        return new EvidencePayload(cleanEvidenceText(text), images.stream().distinct().toList());
    }

    private List<String> extractImageUrls(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("https?://[^\\s\\]\\[\\\"']+")
                .matcher(raw);

        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            String url = matcher.group().trim();
            while (!url.isEmpty() && ",.;)".indexOf(url.charAt(url.length() - 1)) >= 0) {
                url = url.substring(0, url.length() - 1);
            }
            if (!url.isBlank()) {
                urls.add(url);
            }
        }
        return urls.stream().distinct().toList();
    }

    private String cleanEvidenceText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text.replace(LEGACY_IMAGE_MARKER, " ")
                .replaceAll("https?://[^\\s]+", " ")
                .replaceAll("[\\r\\n]{3,}", "\n\n")
                .trim();
        return cleaned;
    }

    private String normalizeSellerProposalType(String proposalType) {
        if (proposalType == null || proposalType.isBlank()) {
            return null;
        }
        String normalized = proposalType.trim().toUpperCase();
        if (!List.of(Dispute.PROPOSAL_REFUND, Dispute.PROPOSAL_REPLACEMENT, Dispute.PROPOSAL_DENY).contains(normalized)) {
            throw new IllegalArgumentException("Invalid seller proposal type");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeText(value, null);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    private String generateDisputeNumber() {
        String datePart = NUMBER_DATE_FORMAT.format(java.time.LocalDate.now());
        long count = disputeRepository.count() + 1;
        return String.format("DSP-%s-%05d", datePart, count);
    }

    private String generateRefundNumber() {
        String datePart = NUMBER_DATE_FORMAT.format(java.time.LocalDate.now());
        long count = refundRequestRepository.count() + 1;
        return String.format("RFD-%s-%05d", datePart, count);
    }

    public record DisputeStats(long total, long opened, long underReview, long resolved) {}

    private record EvidencePayload(String text, List<String> images) {}
}
