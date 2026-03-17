-- Seed data for posts and post_credentials tables
-- Run this script to populate the database with realistic sample data
-- seller_id = 1, credential_status_id = 1 (Available)

-- Clear existing data (optional - uncomment if needed)
-- DELETE FROM post_credentials;
-- DELETE FROM posts;

-- ============================================
-- CATEGORY 1: Giải trí (Entertainment)
-- ============================================

-- Post 1: Netflix Premium 4K
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Netflix Premium 4K - 1 Tháng', 49000.00, 
'<p>Tài khoản Netflix Premium 4K chính chủ, xem được trên4 thiết bị cùng lúc.</p><p><strong>Tính năng:</strong></p><ul><li>Độ phân giải 4K Ultra HD</li><li>Xem trên4 thiết bị cùng lúc</li><li>Không quảng cáo</li><li>Truy cập toàn bộ thư viện phim</li></ul><p><em>Lưu ý: Không đổi mật khẩu, không thay đổi thông tin tài khoản.</em></p>',
NULL, 1, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES 
((SELECT post_id FROM posts WHERE title = 'Netflix Premium 4K - 1 Tháng'), 1, 'netflix.premium.vn1@gmail.com', 'NetFlix@2024#Vip', 'Profile 1 - Tài khoản chính, hạn đến 15/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Netflix Premium 4K - 1 Tháng'), 1, 'netflix.premium.vn2@gmail.com', 'NetFlix@2024#Vip', 'Profile 2 - Hạn đến 15/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Netflix Premium 4K - 1 Tháng'), 1, 'netflix.premium.vn3@gmail.com', 'NetFlix@2024#Vip', 'Profile 3 - Hạn đến 15/04/2026', NOW());

-- Post 2: Spotify Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Spotify Premium Cá Nhân - 1 Tháng', 29000.00,
'<p>Tài khoản Spotify Premium cá nhân, nghe nhạc không giới hạn.</p><p><strong>Tính năng:</strong></p><ul><li>Nghe nhạc không quảng cáo</li><li>Tải xuống để nghe offline</li><li>Chất lượng âm thanh cao 320kbps</li><li>Chọn bài hát bất kỳ</li></ul>',
NULL, 1, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Spotify Premium Cá Nhân - 1 Tháng'), 1, 'spotify.vip.user1@gmail.com', 'Spotify@Premium2024', 'Tài khoản cá nhân, hạn đến 20/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Spotify Premium Cá Nhân - 1 Tháng'), 1, 'spotify.vip.user2@gmail.com', 'Spotify@Premium2024', 'Tài khoản cá nhân, hạn đến 20/04/2026', NOW());

-- Post 3: YouTube Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'YouTube Premium - 1 Tháng', 39000.00,
'<p>YouTube Premium không quảng cáo, chạy nền và YouTube Music.</p><p><strong>Tính năng:</strong></p><ul><li>Xem video không quảng cáo</li><li>Chạy nền khi tắt màn hình</li><li>YouTube Music Premium</li><li>Tải video xem offline</li></ul>',
NULL, 1, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'YouTube Premium - 1 Tháng'), 1, 'youtube.premium.vn1@gmail.com', 'YT@Premium2024!Vn', 'Hạn đến 25/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'YouTube Premium - 1 Tháng'), 1, 'youtube.premium.vn2@gmail.com', 'YT@Premium2024!Vn', 'Hạn đến 25/04/2026', NOW());

-- Post 4: Disney+ Hotstar
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Disney+ Hotstar Premium - 1 Tháng', 59000.00,
'<p>Disney+ Hotstar với đầy đủ phim Marvel, Star Wars, Pixar.</p><p><strong>Tính năng:</strong></p><ul><li>Toàn bộ thư viện Disney, Marvel, Star Wars</li><li>Chất lượng 4K HDR</li><li>Xem trên4 thiết bị</li><li>Phụ đề đa ngôn ngữ</li></ul>',
NULL, 1, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Disney+ Hotstar Premium - 1 Tháng'), 1, 'disney.vip.family1@gmail.com', 'Disney@Hotstar2024', 'Profile 1 - Hạn đến 30/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Disney+ Hotstar Premium - 1 Tháng'), 1, 'disney.vip.family2@gmail.com', 'Disney@Hotstar2024', 'Profile 2 - Hạn đến 30/04/2026', NOW());

-- Post 5: HBO GO
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'HBO GO Premium - 1 Tháng', 69000.00,
'<p>HBO GO với đầy đủ series và phim độc quyền.</p><p><strong>Tính năng:</strong></p><ul><li>Toàn bộ thư viện HBO</li><li>Phim mới nhất từ Hollywood</li><li>Series độc quyền</li><li>Chất lượng HD</li></ul>',
NULL, 1, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'HBO GO Premium - 1 Tháng'), 1, 'hbogo.premium.asia1@gmail.com', 'HBO@Go2024!Asia', 'Hạn đến 10/05/2026', NOW());

-- ============================================
-- CATEGORY 2: Làm việc (Work/Productivity)
-- ============================================

-- Post 6: Microsoft 365 Personal
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Microsoft 365 Personal - 1 Năm', 89000.00,
'<p>Microsoft 365 Personal đầy đủ Word, Excel, PowerPoint, 1TB OneDrive.</p><p><strong>Bao gồm:</strong></p><ul><li>Word, Excel, PowerPoint</li><li>Outlook, OneNote</li><li>1TB OneDrive</li><li>Cài đặt trên1 PC/Mac</li></ul>',
NULL, 2, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Microsoft 365 Personal - 1 Năm'), 1, 'ms365.personal.vn1@outlook.com', 'MS365@Personal2024', 'License key activated, hạn 1 năm từ ngày kích hoạt', NOW()),
((SELECT post_id FROM posts WHERE title = 'Microsoft 365 Personal - 1 Năm'), 1, 'ms365.personal.vn2@outlook.com', 'MS365@Personal2024', 'License key activated, hạn 1 năm từ ngày kích hoạt', NOW());

-- Post 7: Notion Plus
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Notion Plus - 1 Năm', 79000.00,
'<p>Notion Plus với không giới hạn file upload và advanced features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited file uploads</li><li>Unlimited blocks</li><li>30 day version history</li><li>Priority support</li></ul>',
NULL, 2, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Notion Plus - 1 Năm'), 1, 'notion.plus.user1@gmail.com', 'Notion@Plus2024!Pro', 'Hạn đến 01/03/2027', NOW());

-- Post 8: Slack Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Slack Pro Workspace - 1 Tháng', 49000.00,
'<p>Slack Pro với đầy đủ tính năng cho team làm việc.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited message history</li><li>Unlimited integrations</li><li>Group video calls</li><li>Advanced security</li></ul>',
NULL, 2, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Slack Pro Workspace - 1 Tháng'), 1, 'slack.pro.admin1@gmail.com', 'Slack@Pro2024#Team', 'Admin workspace, hạn đến 15/04/2026', NOW());

-- Post 9: Trello Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Trello Premium - 1 Năm', 69000.00,
'<p>Trello Premium với advanced automation và views.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Power-Ups</li><li>Calendar View</li><li>Timeline View</li><li>Advanced automation</li></ul>',
NULL, 2, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Trello Premium - 1 Năm'), 1, 'trello.premium.work@gmail.com', 'Trello@Premium2024', 'Hạn đến 01/06/2027', NOW());

-- Post 10: Zoom Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Zoom Pro - 1 Tháng', 39000.00,
'<p>Zoom Pro với không giới hạn thời gian họp.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited group meetings</li><li>Meeting duration up to 30 hours</li><li>1GB cloud recording</li><li>Streaming to social media</li></ul>',
NULL, 2, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Zoom Pro - 1 Tháng'), 1, 'zoom.pro.meeting1@gmail.com', 'Zoom@Pro2024!Meet', 'Hạn đến 20/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Zoom Pro - 1 Tháng'), 1, 'zoom.pro.meeting2@gmail.com', 'Zoom@Pro2024!Meet', 'Hạn đến 20/04/2026', NOW());

-- ============================================
-- CATEGORY 3: Học tập (Education)
-- ============================================

-- Post 11: Duolingo Max
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Duolingo Max - 1 Năm', 299000.00,
'<p>Duolingo Max với AI-powered learning features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited hearts</li><li>Personalized practice</li><li>AI-powered conversations</li><li>Explain my answer feature</li></ul>',
NULL, 3, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Duolingo Max - 1 Năm'), 1, 'duolingo.max.vn1@gmail.com', 'Duo@Max2024!Learn', 'Hạn đến 01/03/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Duolingo Max - 1 Năm'), 1, 'duolingo.max.vn2@gmail.com', 'Duo@Max2024!Learn', 'Hạn đến 01/03/2027', NOW());

-- Post 12: Coursera Plus
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Coursera Plus - 1 Năm', 199000.00,
'<p>Coursera Plus truy cập không giới hạn hơn 7000 khóa học.</p><p><strong>Tính năng:</strong></p><ul><li>Access to 7000+ courses</li><li>Unlimited certificates</li><li>Learn at your own pace</li><li>Projects and specializations</li></ul>',
NULL, 3, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Coursera Plus - 1 Năm'), 1, 'coursera.plus.learner1@gmail.com', 'Coursera@Plus2024', 'Hạn đến 15/06/2027', NOW());

-- Post 13: Skillshare Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Skillshare Premium - 1 Năm', 99000.00,
'<p>Skillshare Premium với hàng ngàn khóa học creative.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited classes</li><li>Offline access</li><li>No ads</li><li>Access to community</li></ul>',
NULL, 3, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Skillshare Premium - 1 Năm'), 1, 'skillshare.premium.art1@gmail.com', 'Skill@Share2024!Art', 'Hạn đến 01/04/2027', NOW());

-- Post 14: Grammarly Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Grammarly Premium - 1 Năm', 79000.00,
'<p>Grammarly Premium với AI writing assistant.</p><p><strong>Tính năng:</strong></p><ul><li>Advanced grammar check</li><li>Clarity suggestions</li><li>Tone detection</li><li>Plagiarism checker</li></ul>',
NULL, 3, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Grammarly Premium - 1 Năm'), 1, 'grammarly.premium.write1@gmail.com', 'Grammar@Ly2024!Write', 'Hạn đến 01/05/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Grammarly Premium - 1 Năm'), 1, 'grammarly.premium.write2@gmail.com', 'Grammar@Ly2024!Write', 'Hạn đến 01/05/2027', NOW());

-- Post 15: QuillBot Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'QuillBot Premium - 1 Năm', 59000.00,
'<p>QuillBot Premium với AI paraphrasing tool.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited paraphrasing</li><li>All7 writing modes</li><li>Plagiarism checker</li><li>Grammar checker</li></ul>',
NULL, 3, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'QuillBot Premium - 1 Năm'), 1, 'quillbot.premium.edu1@gmail.com', 'Quill@Bot2024!Para', 'Hạn đến 01/06/2027', NOW());

-- ============================================
-- CATEGORY 4: eSIM du lịch (Travel eSIM)
-- ============================================

-- Post 16: eSIM Thái Lan
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'eSIM Thái Lan 7 Ngày - 5GB', 89000.00,
'<p>eSIM Thái Lan data roaming, không cần đổi sim.</p><p><strong>Thông tin:</strong></p><ul><li>5GB data tốc độ cao</li><li>Hạn sử dụng 7 ngày</li><li>Không cần đổi SIM</li><li>Hoạt động ngay sau khi kích hoạt</li></ul>',
NULL, 4, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'eSIM Thái Lan 7 Ngày - 5GB'), 1, 'ESIM-TH-2024-001', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW()),
((SELECT post_id FROM posts WHERE title = 'eSIM Thái Lan 7 Ngày - 5GB'), 1, 'ESIM-TH-2024-002', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW()),
((SELECT post_id FROM posts WHERE title = 'eSIM Thái Lan 7 Ngày - 5GB'), 1, 'ESIM-TH-2024-003', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW());

-- Post 17: eSIM Hàn Quốc
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'eSIM Hàn Quốc 10 Ngày - 8GB', 129000.00,
'<p>eSIM Hàn Quốc data roaming cho du lịch.</p><p><strong>Thông tin:</strong></p><ul><li>8GB data tốc độ cao</li><li>Hạn sử dụng 10 ngày</li><li>Hỗ trợ 4G/LTE</li><li>Không cần đăng ký</li></ul>',
NULL, 4, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'eSIM Hàn Quốc 10 Ngày - 8GB'), 1, 'ESIM-KR-2024-001', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW()),
((SELECT post_id FROM posts WHERE title = 'eSIM Hàn Quốc 10 Ngày - 8GB'), 1, 'ESIM-KR-2024-002', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW());

-- Post 18: eSIM Nhật Bản
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'eSIM Nhật Bản 15 Ngày - 10GB', 159000.00,
'<p>eSIM Nhật Bản data roaming chất lượng cao.</p><p><strong>Thông tin:</strong></p><ul><li>10GB data tốc độ cao</li><li>Hạn sử dụng 15 ngày</li><li>Coverage toàn Nhật Bản</li><li>4G/LTE tốc độ cao</li></ul>',
NULL, 4, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'eSIM Nhật Bản 15 Ngày - 10GB'), 1, 'ESIM-JP-2024-001', 'Activation: QR Code via email', 'QR Code sẽ được gửi qua email sau khi mua', NOW());

-- ============================================
-- CATEGORY 5: EditẢnh - Video (Photo/Video Editing)
-- ============================================

-- Post 19: Adobe Creative Cloud
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Adobe Creative Cloud All Apps - 1 Năm', 199000.00,
'<p>Adobe Creative Cloud đầy đủ 20+ ứng dụng.</p><p><strong>Bao gồm:</strong></p><ul><li>Photoshop, Lightroom</li><li>Premiere Pro, After Effects</li><li>Illustrator, InDesign</li><li>100GB cloud storage</li></ul>',
NULL, 5, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Adobe Creative Cloud All Apps - 1 Năm'), 1, 'adobe.cc.premium1@gmail.com', 'Adobe@CC2024!Creative', 'Hạn đến 01/06/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Adobe Creative Cloud All Apps - 1 Năm'), 1, 'adobe.cc.premium2@gmail.com', 'Adobe@CC2024!Creative', 'Hạn đến 01/06/2027', NOW());

-- Post 20: Canva Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Canva Pro - 1 Năm', 59000.00,
'<p>Canva Pro với đầy đủ template và tính năng nâng cao.</p><p><strong>Tính năng:</strong></p><ul><li>Millions of premium templates</li><li>100GB cloud storage</li><li>Remove background</li><li>Brand Kit</li></ul>',
NULL, 5, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Canva Pro - 1 Năm'), 1, 'canva.pro.design1@gmail.com', 'Canva@Pro2024!Design', 'Hạn đến 01/04/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Canva Pro - 1 Năm'), 1, 'canva.pro.design2@gmail.com', 'Canva@Pro2024!Design', 'Hạn đến 01/04/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Canva Pro - 1 Năm'), 1, 'canva.pro.design3@gmail.com', 'Canva@Pro2024!Design', 'Hạn đến 01/04/2027', NOW());

-- Post 21: CapCut Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'CapCut Pro - 1 Năm', 49000.00,
'<p>CapCut Pro với đầy đủ hiệu ứng và tính năng edit video.</p><p><strong>Tính năng:</strong></p><ul><li>Premium effects & filters</li><li>No watermark</li><li>4K export</li><li>Cloud storage</li></ul>',
NULL, 5, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'CapCut Pro - 1 Năm'), 1, 'capcut.pro.editor1@gmail.com', 'CapCut@Pro2024!Edit', 'Hạn đến 01/05/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'CapCut Pro - 1 Năm'), 1, 'capcut.pro.editor2@gmail.com', 'CapCut@Pro2024!Edit', 'Hạn đến 01/05/2027', NOW());

-- Post 22: Figma Professional
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Figma Professional - 1 Năm', 89000.00,
'<p>Figma Professional với advanced design features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Figma files</li><li>Unlimited version history</li><li>Sharing permissions</li><li>Team libraries</li></ul>',
NULL, 5, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Figma Professional - 1 Năm'), 1, 'figma.pro.designer1@gmail.com', 'Figma@Pro2024!UI', 'Hạn đến 01/06/2027', NOW());

-- ============================================
-- CATEGORY 6: Window Office (Windows/Office)
-- ============================================

-- Post 23: Windows 11 Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Windows 11 Pro License Key', 590000.00,
'<p>Windows 11 Pro License Key bản quyền vĩnh viễn.</p><p><strong>Tính năng:</strong></p><ul><li>License key vĩnh viễn</li><li>Kích hoạt online</li><li>Hỗ trợ cài đặt lại</li><li>Full features Pro</li></ul>',
NULL, 6, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Windows 11 Pro License Key'), 1, 'VK7JG-NPHTM-C97JM-9MPGT-3V66T', 'N/A - License Key Only', 'Windows 11 Pro Retail Key - 1 PC', NOW()),
((SELECT post_id FROM posts WHERE title = 'Windows 11 Pro License Key'), 1, 'W269N-WFGWX-YVC9B-4J6C9-T83GX', 'N/A - License Key Only', 'Windows 11 Pro Retail Key - 1 PC', NOW());

-- Post 24: Office 2021 Professional
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Microsoft Office 2021 Professional Plus', 890000.00,
'<p>Microsoft Office 2021 Professional Plus bản quyền vĩnh viễn.</p><p><strong>Bao gồm:</strong></p><ul><li>Word 2021</li><li>Excel 2021</li><li>PowerPoint 2021</li><li>Outlook, Access, Publisher</li></ul>',
NULL, 6, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Microsoft Office 2021 Professional Plus'), 1, 'OFFICE2021-PRO-001', 'Key: VYBBJ-TRJPB-QFQRF-QFT4D-H3GVB', 'Office 2021 Pro Plus - 1 PC', NOW()),
((SELECT post_id FROM posts WHERE title = 'Microsoft Office 2021 Professional Plus'), 1, 'OFFICE2021-PRO-002', 'Key: XQNVK-8J9DB-PJ3P8-RRQJF-KM4HM', 'Office 2021 Pro Plus - 1 PC', NOW());

-- Post 25: Visual Studio Enterprise
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Visual Studio Enterprise 2022 - 1 Năm', 199000.00,
'<p>Visual Studio Enterprise 2022 subscription.</p><p><strong>Tính năng:</strong></p><ul><li>Full IDE features</li><li>Azure DevOps</li><li>Cloud services</li><li>Technical support</li></ul>',
NULL, 6, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Visual Studio Enterprise 2022 - 1 Năm'), 1, 'vs.enterprise.dev1@outlook.com', 'VS@Enterprise2024', 'Subscription active until 01/03/2027', NOW());

-- ============================================
-- CATEGORY 7: Google Drive (Cloud Storage)
-- ============================================

-- Post 26: Google One 200GB
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Google One 200GB - 1 Năm', 49000.00,
'<p>Google One 200GB lưu trữ đám mây.</p><p><strong>Tính năng:</strong></p><ul><li>200GB storage</li><li>Google Photos backup</li><li>Google Drive</li><li>Gmail storage</li></ul>',
NULL, 7, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Google One 200GB - 1 Năm'), 1, 'googleone.storage1@gmail.com', 'Google@One2024!200GB', 'Hạn đến 01/04/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'Google One 200GB - 1 Năm'), 1, 'googleone.storage2@gmail.com', 'Google@One2024!200GB', 'Hạn đến 01/04/2027', NOW());

-- Post 27: Google One 2TB
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Google One 2TB - 1 Năm', 99000.00,
'<p>Google One 2TB lưu trữ đám mây cao cấp.</p><p><strong>Tính năng:</strong></p><ul><li>2TB storage</li><li>VPN for Android</li><li>Priority support</li><li>Family sharing</li></ul>',
NULL, 7, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Google One 2TB - 1 Năm'), 1, 'googleone.premium1@gmail.com', 'Google@One2024!2TB', 'Hạn đến 01/06/2027', NOW());

-- Post 28: Dropbox Plus
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Dropbox Plus 2TB - 1 Năm', 79000.00,
'<p>Dropbox Plus với 2TB lưu trữ.</p><p><strong>Tính năng:</strong></p><ul><li>2TB storage</li><li>File recovery</li><li>Offline access</li><li>Computer backup</li></ul>',
NULL, 7, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Dropbox Plus 2TB - 1 Năm'), 1, 'dropbox.plus.user1@gmail.com', 'Dropbox@Plus2024!2TB', 'Hạn đến 01/05/2027', NOW());

-- ============================================
-- CATEGORY 8: Thế giới AI (AI World)
-- ============================================

-- Post 29: ChatGPT Plus
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'ChatGPT Plus - 1 Tháng', 99000.00,
'<p>ChatGPT Plus với GPT-4 và các tính năng nâng cao.</p><p><strong>Tính năng:</strong></p><ul><li>Access to GPT-4</li><li>Faster response times</li><li>Priority access to new features</li><li>Available during peak times</li></ul>',
NULL, 8, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'ChatGPT Plus - 1 Tháng'), 1, 'chatgpt.plus.user1@gmail.com', 'ChatGPT@Plus2024!AI', 'Hạn đến 15/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'ChatGPT Plus - 1 Tháng'), 1, 'chatgpt.plus.user2@gmail.com', 'ChatGPT@Plus2024!AI', 'Hạn đến 15/04/2026', NOW());

-- Post 30: Claude Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Claude Pro - 1 Tháng', 89000.00,
'<p>Claude Pro với Claude 3 Opus.</p><p><strong>Tính năng:</strong></p><ul><li>Access to Claude 3 Opus</li><li>Higher message limits</li><li>Priority access</li><li>Advanced reasoning</li></ul>',
NULL, 8, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Claude Pro - 1 Tháng'), 1, 'claude.pro.user1@gmail.com', 'Claude@Pro2024!Anthropic', 'Hạn đến 20/04/2026', NOW());

-- Post 31: Midjourney Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Midjourney Premium - 1 Tháng', 199000.00,
'<p>Midjourney Premium với unlimited image generation.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited image generations</li><li>Fast GPU time</li><li>Relax mode</li><li>Private generation</li></ul>',
NULL, 8, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Midjourney Premium - 1 Tháng'), 1, 'midjourney.premium1@gmail.com', 'Mid@Journey2024!Art', 'Discord account - Hạn đến 01/05/2026', NOW());

-- Post 32: Perplexity Pro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Perplexity Pro - 1 Năm', 149000.00,
'<p>Perplexity Pro với AI-powered search.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Pro searches</li><li>GPT-4 access</li><li>Claude access</li><li>File upload analysis</li></ul>',
NULL, 8, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Perplexity Pro - 1 Năm'), 1, 'perplexity.pro.search1@gmail.com', 'Perplexity@Pro2024!Search', 'Hạn đến 01/03/2027', NOW());

-- ============================================
-- CATEGORY 9: VPN bảo mật mạng (VPN Security)
-- ============================================

-- Post 33: NordVPN 2 Năm
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'NordVPN Premium - 2 Năm', 89000.00,
'<p>NordVPN Premium với 5000+ servers worldwide.</p><p><strong>Tính năng:</strong></p><ul><li>5000+ servers in 60 countries</li><li>No logs policy</li><li>6 devices simultaneously</li><li>Threat protection</li></ul>',
NULL, 9, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'NordVPN Premium - 2 Năm'), 1, 'nordvpn.premium1@gmail.com', 'Nord@VPN2024!Secure', 'Hạn đến 01/03/2028', NOW()),
((SELECT post_id FROM posts WHERE title = 'NordVPN Premium - 2 Năm'), 1, 'nordvpn.premium2@gmail.com', 'Nord@VPN2024!Secure', 'Hạn đến 01/03/2028', NOW());

-- Post 34: ExpressVPN 1 Năm
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'ExpressVPN - 1 Năm', 129000.00,
'<p>ExpressVPN với tốc độ nhanh nhất.</p><p><strong>Tính năng:</strong></p><ul><li>Ultra-fast servers</li><li>160 locations in 94 countries</li><li>5 devices simultaneously</li><li>24/7 support</li></ul>',
NULL, 9, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'ExpressVPN - 1 Năm'), 1, 'expressvpn.user1@gmail.com', 'Express@VPN2024!Fast', 'Hạn đến 01/06/2027', NOW());

-- Post 35: Surfshark VPN
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Surfshark VPN - 2 Năm', 69000.00,
'<p>Surfshark VPN với unlimited devices.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited devices</li><li>3200+ servers</li><li>CleanWeb ad blocker</li><li>NoBorders mode</li></ul>',
NULL, 9, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Surfshark VPN - 2 Năm'), 1, 'surfshark.unlimited1@gmail.com', 'Surf@Shark2024!Unlimited', 'Hạn đến 01/03/2028', NOW()),
((SELECT post_id FROM posts WHERE title = 'Surfshark VPN - 2 Năm'), 1, 'surfshark.unlimited2@gmail.com', 'Surf@Shark2024!Unlimited', 'Hạn đến 01/03/2028', NOW());

-- ============================================
-- CATEGORY 10: Gift Card
-- ============================================

-- Post 36: Steam Gift Card $20
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Steam Gift Card $20 USD', 490000.00,
'<p>Steam Gift Card $20 USD - Region: US.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $20 USD</li><li>Region: US</li><li>Redeem immediately</li><li>No expiration</li></ul>',
NULL, 10, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Steam Gift Card $20 USD'), 1, 'STEAM-GC-US-001', 'Code: A1B2C-D3E4F-G5H6I', 'Steam Wallet Code $20 US', NOW()),
((SELECT post_id FROM posts WHERE title = 'Steam Gift Card $20 USD'), 1, 'STEAM-GC-US-002', 'Code: J7K8L-M9N0P-Q1R2S', 'Steam Wallet Code $20 US', NOW());

-- Post 37: iTunes Gift Card $25
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'iTunes Gift Card $25 USD', 599000.00,
'<p>iTunes Gift Card $25 USD - Region: US.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $25 USD</li><li>Region: US</li><li>For App Store, iTunes, Apple Music</li><li>No expiration</li></ul>',
NULL, 10, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'iTunes Gift Card $25 USD'), 1, 'ITUNES-GC-US-001', 'Code: XXXXXX-XXXXXX-XXXXXX', 'iTunes Code $25 US', NOW());

-- Post 38: Google Play Gift Card $20
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Google Play Gift Card $20 USD', 479000.00,
'<p>Google Play Gift Card $20 USD.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $20 USD</li><li>Region: US</li><li>For apps, games, movies</li><li>No expiration</li></ul>',
NULL, 10, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Google Play Gift Card $20 USD'), 1, 'GOOGLEPLAY-GC-001', 'Code: PLAY-XXXX-XXXX-XXXX', 'Google Play Code $20 US', NOW()),
((SELECT post_id FROM posts WHERE title = 'Google Play Gift Card $20 USD'), 1, 'GOOGLEPLAY-GC-002', 'Code: PLAY-YYYY-YYYY-YYYY', 'Google Play Code $20 US', NOW());

-- Post 39: Spotify Gift Card $30
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Spotify Gift Card $30 USD', 699000.00,
'<p>Spotify Gift Card $30 USD for Premium subscription.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $30 USD</li><li>Region: US</li><li>3 months Premium</li><li>No expiration</li></ul>',
NULL, 10, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Spotify Gift Card $30 USD'), 1, 'SPOTIFY-GC-US-001', 'Code: SPOT-XXXX-XXXX-XXXX', 'Spotify Gift Code $30 US', NOW());

-- ============================================
-- CATEGORY 11: Khác (Other)
-- ============================================

-- Post 40: Discord Nitro
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Discord Nitro - 1 Tháng', 49000.00,
'<p>Discord Nitro với đầy đủ tính năng premium.</p><p><strong>Tính năng:</strong></p><ul><li>Custom emoji anywhere</li><li>Animated avatar</li><li>Server boost</li><li>HD streaming</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Discord Nitro - 1 Tháng'), 1, 'discord.nitro.user1@gmail.com', 'Discord@Nitro2024!Boost', 'Hạn đến 20/04/2026', NOW()),
((SELECT post_id FROM posts WHERE title = 'Discord Nitro - 1 Tháng'), 1, 'discord.nitro.user2@gmail.com', 'Discord@Nitro2024!Boost', 'Hạn đến 20/04/2026', NOW());

-- Post 41: Tinder Gold
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Tinder Gold - 1 Tháng', 79000.00,
'<p>Tinder Gold với các tính năng premium dating.</p><p><strong>Tính năng:</strong></p><ul><li>See who likes you</li><li>Unlimited likes</li><li>Passport to any location</li><li>Rewind last swipe</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Tinder Gold - 1 Tháng'), 1, 'tinder.gold.user1@gmail.com', 'Tinder@Gold2024!Match', 'Hạn đến 25/04/2026', NOW());

-- Post 42: LinkedIn Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'LinkedIn Premium Career - 1 Tháng', 99000.00,
'<p>LinkedIn Premium Career cho job seekers.</p><p><strong>Tính năng:</strong></p><ul><li>See who viewed your profile</li><li>InMail messages</li><li>Salary insights</li><li>Interview prep</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'LinkedIn Premium Career - 1 Tháng'), 1, 'linkedin.premium.career1@gmail.com', 'LinkedIn@Premium2024!Job', 'Hạn đến 30/04/2026', NOW());

-- Post 43: Evernote Personal
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Evernote Personal - 1 Năm', 69000.00,
'<p>Evernote Personal với unlimited notes.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited notes</li><li>10GB uploads/month</li><li>Offline access</li><li>PDF search</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Evernote Personal - 1 Năm'), 1, 'evernote.personal.note1@gmail.com', 'Ever@Note2024!Personal', 'Hạn đến 01/05/2027', NOW());

-- Post 44: LastPass Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'LastPass Premium - 1 Năm', 49000.00,
'<p>LastPass Premium password manager.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited passwords</li><li>Cross-device sync</li><li>Dark web monitoring</li><li>1GB secure storage</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'LastPass Premium - 1 Năm'), 1, 'lastpass.premium.secure1@gmail.com', 'Last@Pass2024!Secure', 'Hạn đến 01/06/2027', NOW()),
((SELECT post_id FROM posts WHERE title = 'LastPass Premium - 1 Năm'), 1, 'lastpass.premium.secure2@gmail.com', 'Last@Pass2024!Secure', 'Hạn đến 01/06/2027', NOW());

-- Post 45: Crunchyroll Premium
INSERT INTO posts (seller_id, title, price, description, thumbnail_url, category_id, stock_status, created_at, updated_at)
VALUES (1, 'Crunchyroll Premium - 1 Năm', 199000.00,
'<p>Crunchyroll Premium Mega Fan với anime không giới hạn.</p><p><strong>Tính năng:</strong></p><ul><li>Ad-free anime</li><li>Offline viewing</li><li>4 devices</li><li>New episodes 1 hour after Japan</li></ul>',
NULL, 11, 'IN_STOCK', NOW(), NOW());

INSERT INTO post_credentials (post_id, credential_status_id, account_username, account_password, security_notes, created_at)
VALUES
((SELECT post_id FROM posts WHERE title = 'Crunchyroll Premium - 1 Năm'), 1, 'crunchyroll.premium.anime1@gmail.com', 'Crunchy@Roll2024!Anime', 'Hạn đến 01/04/2027', NOW());

-- Verify the data
SELECT 'Posts created:' as info, COUNT(*) as count FROM posts;
SELECT 'Credentials created:' as info, COUNT(*) as count FROM post_credentials;
