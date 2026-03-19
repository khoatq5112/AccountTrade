package com.group3.accounttrade.config;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final CredentialStatusRepository credentialStatusRepository;
    
    // New status repositories for transaction workflow
    private final OrderStatusRepository orderStatusRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final EscrowStatusRepository escrowStatusRepository;
    private final DisputeStatusRepository disputeStatusRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initCategories();
        initCredentialStatuses();
        
        // Initialize new status tables for transaction workflow
        initOrderStatuses();
        initPaymentStatuses();
        initEscrowStatuses();
        initDisputeStatuses();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().roleName("Admin").build());
            roleRepository.save(Role.builder().roleName("Seller").build());
            roleRepository.save(Role.builder().roleName("Buyer").build());
            log.info("Initialized roles");
        }
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        // Flat category list (11 categories)
        categoryRepository.save(Category.builder().categoryName("Giải trí").categoryIcon("ph-game-controller").displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Làm việc").categoryIcon("ph-briefcase").displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Học tập").categoryIcon("ph-book-open").displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("eSIM du lịch").categoryIcon("ph-sim-card").displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Edit Ảnh - Video").categoryIcon("ph-camera").displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("Window Office").categoryIcon("ph-desktop").displayOrder(6).build());
        categoryRepository.save(Category.builder().categoryName("Google Drive").categoryIcon("ph-cloud").displayOrder(7).build());
        categoryRepository.save(Category.builder().categoryName("Thế giới AI").categoryIcon("ph-brain").displayOrder(8).build());
        categoryRepository.save(Category.builder().categoryName("VPN bảo mật mạng").categoryIcon("ph-shield-check").displayOrder(9).build());
        categoryRepository.save(Category.builder().categoryName("Gift Card").categoryIcon("ph-gift").displayOrder(10).build());
        categoryRepository.save(Category.builder().categoryName("Khác").categoryIcon("ph-dots-three").displayOrder(11).build());

        log.info("Initialized {} categories", categoryRepository.count());
    }

    private void initCredentialStatuses() {
        if (credentialStatusRepository.count() == 0) {
            credentialStatusRepository.save(CredentialStatus.builder()
                .statusName(CredentialStatus.AVAILABLE).description("Ready for sale").build());
            credentialStatusRepository.save(CredentialStatus.builder()
                .statusName(CredentialStatus.HOLDING).description("Held during transaction").build());
            credentialStatusRepository.save(CredentialStatus.builder()
                .statusName(CredentialStatus.SOLD).description("Sold to buyer").build());
            credentialStatusRepository.save(CredentialStatus.builder()
                .statusName(CredentialStatus.HIDDEN).description("Hidden from inventory").build());
            log.info("Initialized credential statuses");
        }
    }

    /**
     * Initialize order statuses for the transaction workflow.
     * Covers the complete lifecycle from cart to completion.
     */
    private void initOrderStatuses() {
        if (orderStatusRepository.count() == 0) {
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.PENDING)
                .description("Order created but awaiting payment")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.AWAITING_PAYMENT)
                .description("Order is waiting for payment confirmation")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.PAID)
                .description("Payment received, order is being processed")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.PROCESSING)
                .description("Order is being processed, credentials being assigned")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.CREDENTIAL_ASSIGNED)
                .description("Credentials have been assigned to the order")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.AWAITING_BUYER_CONFIRMATION)
                .description("Waiting for buyer to confirm credential validity")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.DISPUTED)
                .description("Order is under dispute")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.COMPLETED)
                .description("Order completed successfully, escrow released")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.CANCELLED)
                .description("Order was cancelled")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.REFUNDED)
                .description("Order was refunded to buyer")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.PAYMENT_EXPIRED)
                .description("Order expired due to payment timeout")
                .build());
            orderStatusRepository.save(OrderStatus.builder()
                .statusName(OrderStatus.PAYMENT_FAILED)
                .description("Order failed due to payment failure")
                .build());
            log.info("Initialized {} order statuses", orderStatusRepository.count());
        }
    }

    /**
     * Initialize payment statuses for VNPAY integration.
     */
    private void initPaymentStatuses() {
        if (paymentStatusRepository.count() == 0) {
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.INITIATED)
                .description("Payment has been initiated, awaiting VNPAY redirect")
                .build());
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.PENDING)
                .description("Payment is pending at VNPAY")
                .build());
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.PAID)
                .description("Payment confirmed successful via IPN")
                .build());
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.FAILED)
                .description("Payment failed")
                .build());
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.CALLBACK_MISMATCH)
                .description("Payment callback data mismatch - requires manual review")
                .build());
            paymentStatusRepository.save(PaymentStatus.builder()
                .statusName(PaymentStatus.REFUNDED)
                .description("Payment has been refunded")
                .build());
            log.info("Initialized {} payment statuses", paymentStatusRepository.count());
        }
    }

    /**
     * Initialize escrow statuses for fund holding.
     */
    private void initEscrowStatuses() {
        if (escrowStatusRepository.count() == 0) {
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.NOT_CREATED)
                .description("Escrow has not been created yet")
                .build());
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.HOLDING)
                .description("Funds are being held in escrow")
                .build());
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.FROZEN)
                .description("Escrow is frozen due to dispute or review")
                .build());
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.RELEASED)
                .description("Funds have been released to seller")
                .build());
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.REFUNDED)
                .description("Funds have been refunded to buyer")
                .build());
            escrowStatusRepository.save(EscrowStatus.builder()
                .statusName(EscrowStatus.PARTIALLY_REFUNDED)
                .description("Funds have been partially refunded")
                .build());
            log.info("Initialized {} escrow statuses", escrowStatusRepository.count());
        }
    }

    /**
     * Initialize dispute statuses for conflict resolution.
     */
    private void initDisputeStatuses() {
        if (disputeStatusRepository.count() == 0) {
            disputeStatusRepository.save(DisputeStatus.builder()
                .statusName(DisputeStatus.OPENED)
                .description("Dispute has been opened")
                .build());
            disputeStatusRepository.save(DisputeStatus.builder()
                .statusName(DisputeStatus.UNDER_REVIEW)
                .description("Dispute is being reviewed by admin")
                .build());
            disputeStatusRepository.save(DisputeStatus.builder()
                .statusName(DisputeStatus.RESOLVED)
                .description("Dispute has been resolved")
                .build());
            disputeStatusRepository.save(DisputeStatus.builder()
                .statusName(DisputeStatus.CANCELLED)
                .description("Dispute was cancelled")
                .build());
            log.info("Initialized {} dispute statuses", disputeStatusRepository.count());
        }
    }
}
