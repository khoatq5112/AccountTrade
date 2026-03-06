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

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initPostStatuses();
        initTransactionStatuses();
        initCategories();
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

        // Parent categories
        Category entertainment = categoryRepository.save(Category.builder().categoryName("Giải trí").categoryIcon("ph-game-controller").displayOrder(1).build());
        Category work = categoryRepository.save(Category.builder().categoryName("Làm việc").categoryIcon("ph-briefcase").displayOrder(2).build());
        Category learning = categoryRepository.save(Category.builder().categoryName("Học tập").categoryIcon("ph-book-open").displayOrder(3).build());
        Category esim = categoryRepository.save(Category.builder().categoryName("eSIM Du lịch").categoryIcon("ph-sim-card").displayOrder(4).build());
        Category editMedia = categoryRepository.save(Category.builder().categoryName("Edit Ảnh - Video").categoryIcon("ph-camera").displayOrder(5).build());
        Category windowsOffice = categoryRepository.save(Category.builder().categoryName("Windows, Office").categoryIcon("ph-desktop").displayOrder(6).build());
        Category googleDrive = categoryRepository.save(Category.builder().categoryName("Google Drive").categoryIcon("ph-cloud").displayOrder(7).build());
        Category aiWorld = categoryRepository.save(Category.builder().categoryName("Thế giới AI").categoryIcon("ph-robot").displayOrder(8).build());
        Category vpn = categoryRepository.save(Category.builder().categoryName("VPN, Bảo mật mạng").categoryIcon("ph-shield-check").displayOrder(9).build());
        Category giftCard = categoryRepository.save(Category.builder().categoryName("Gift Card").categoryIcon("ph-ticket").displayOrder(10).build());

        // Entertainment subcategories
        categoryRepository.save(Category.builder().categoryName("Game").categoryIcon("ph-game-controller").parent(entertainment).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Netflix").categoryIcon("ph-play-circle").parent(entertainment).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Spotify").categoryIcon("ph-music-notes").parent(entertainment).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("YouTube Premium").categoryIcon("ph-youtube-logo").parent(entertainment).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Disney+").categoryIcon("ph-film-slate").parent(entertainment).displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("Apple Music").categoryIcon("ph-apple-logo").parent(entertainment).displayOrder(6).build());

        // Work subcategories
        categoryRepository.save(Category.builder().categoryName("Slack").categoryIcon("ph-chat-centered-text").parent(work).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Zoom").categoryIcon("ph-video-camera").parent(work).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Microsoft Teams").categoryIcon("ph-users-three").parent(work).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("Notion").categoryIcon("ph-notebook").parent(work).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Figma").categoryIcon("ph-paint-brush-broad").parent(work).displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("GitHub").categoryIcon("ph-github-logo").parent(work).displayOrder(6).build());

        // Learning subcategories
        categoryRepository.save(Category.builder().categoryName("Coursera").categoryIcon("ph-certificate").parent(learning).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Udemy").categoryIcon("ph-play-circle").parent(learning).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Skillshare").categoryIcon("ph-lightning").parent(learning).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("Duolingo").categoryIcon("ph-globe").parent(learning).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("LinkedIn Learning").categoryIcon("ph-linkedin-logo").parent(learning).displayOrder(5).build());

        // eSIM subcategories
        categoryRepository.save(Category.builder().categoryName("Airalo").categoryIcon("ph-globe").parent(esim).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Holafly").categoryIcon("ph-airplane").parent(esim).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("GigSky").categoryIcon("ph-wifi-high").parent(esim).displayOrder(3).build());

        // Edit Media subcategories
        categoryRepository.save(Category.builder().categoryName("Adobe Creative Cloud").categoryIcon("ph-palette").parent(editMedia).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("CapCut Pro").categoryIcon("ph-video").parent(editMedia).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("DaVinci Resolve").categoryIcon("ph-film-slate").parent(editMedia).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("Canva Pro").categoryIcon("ph-shapes").parent(editMedia).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Lightroom").categoryIcon("ph-image").parent(editMedia).displayOrder(5).build());

        // Windows/Office subcategories
        categoryRepository.save(Category.builder().categoryName("Microsoft 365").categoryIcon("ph-microsoft-logo").parent(windowsOffice).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Windows 11 Pro").categoryIcon("ph-windows-logo").parent(windowsOffice).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Office 2021").categoryIcon("ph-file-doc").parent(windowsOffice).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("Visio").categoryIcon("ph-chart-pie-slice").parent(windowsOffice).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Project").categoryIcon("ph-kanban").parent(windowsOffice).displayOrder(5).build());

        // Google Drive subcategories
        categoryRepository.save(Category.builder().categoryName("Google One").categoryIcon("ph-google-logo").parent(googleDrive).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Google Workspace").categoryIcon("ph-google-logo").parent(googleDrive).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("YouTube Music Premium").categoryIcon("ph-youtube-logo").parent(googleDrive).displayOrder(3).build());

        // AI World subcategories
        categoryRepository.save(Category.builder().categoryName("ChatGPT Plus").categoryIcon("ph-chat-circle-dots").parent(aiWorld).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("Claude Pro").categoryIcon("ph-brain").parent(aiWorld).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Midjourney").categoryIcon("ph-image-square").parent(aiWorld).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("DALL-E").categoryIcon("ph-paint-brush").parent(aiWorld).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Perplexity Pro").categoryIcon("ph-magnifying-glass").parent(aiWorld).displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("Copilot Pro").categoryIcon("ph-copilot-logo").parent(aiWorld).displayOrder(6).build());
        categoryRepository.save(Category.builder().categoryName("Jasper AI").categoryIcon("ph-writing").parent(aiWorld).displayOrder(7).build());

        // VPN subcategories
        categoryRepository.save(Category.builder().categoryName("NordVPN").categoryIcon("ph-shield-check").parent(vpn).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("ExpressVPN").categoryIcon("ph-lock-key").parent(vpn).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Surfshark").categoryIcon("ph-wifi-high").parent(vpn).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("1Password").categoryIcon("ph-key").parent(vpn).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("LastPass").categoryIcon("ph-key").parent(vpn).displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("Dashlane").categoryIcon("ph-lock-locker").parent(vpn).displayOrder(6).build());

        // Gift Card subcategories
        categoryRepository.save(Category.builder().categoryName("Steam Wallet").categoryIcon("ph-game-controller").parent(giftCard).displayOrder(1).build());
        categoryRepository.save(Category.builder().categoryName("PlayStation Store").categoryIcon("ph-playstation-logo").parent(giftCard).displayOrder(2).build());
        categoryRepository.save(Category.builder().categoryName("Xbox Gift Card").categoryIcon("ph-xbox-logo").parent(giftCard).displayOrder(3).build());
        categoryRepository.save(Category.builder().categoryName("iTunes/App Store").categoryIcon("ph-app-store-logo").parent(giftCard).displayOrder(4).build());
        categoryRepository.save(Category.builder().categoryName("Amazon Gift Card").categoryIcon("ph-shopping-bag").parent(giftCard).displayOrder(5).build());
        categoryRepository.save(Category.builder().categoryName("Google Play Gift Card").categoryIcon("ph-google-play-logo").parent(giftCard).displayOrder(6).build());

        log.info("Initialized {} categories", categoryRepository.count());
    }
}
