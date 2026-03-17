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
    private final PostStatusRepository postStatusRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final CategoryRepository categoryRepository;
    private final CredentialStatusRepository credentialStatusRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initPostStatuses();
        initTransactionStatuses();
        initCategories();
        initCredentialStatuses();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().roleName("Admin").build());
            roleRepository.save(Role.builder().roleName("Seller").build());
            roleRepository.save(Role.builder().roleName("Buyer").build());
            log.info("Initialized roles");
        }
    }

    private void initPostStatuses() {
        if (postStatusRepository.count() == 0) {
            postStatusRepository.save(PostStatus.builder().statusName("Available").description("Post is available for purchase").build());
            postStatusRepository.save(PostStatus.builder().statusName("Holding").description("Holding for escrow").build());
            postStatusRepository.save(PostStatus.builder().statusName("Sold").description("Post has been sold").build());
            postStatusRepository.save(PostStatus.builder().statusName("Hidden").description("Post is hidden").build());
            log.info("Initialized post statuses");
        }
    }

    private void initTransactionStatuses() {
        if (transactionStatusRepository.count() == 0) {
            transactionStatusRepository.save(TransactionStatus.builder().statusName("Pending").build());
            transactionStatusRepository.save(TransactionStatus.builder().statusName("Holding").build());
            transactionStatusRepository.save(TransactionStatus.builder().statusName("Completed").build());
            transactionStatusRepository.save(TransactionStatus.builder().statusName("Refunded").build());
            log.info("Initialized transaction statuses");
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
}
