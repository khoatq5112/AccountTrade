-- 1. Bảng Vai trò
CREATE TABLE Roles (
                      role_id INT AUTO_INCREMENT PRIMARY KEY,
                      role_name VARCHAR(50) NOT NULL UNIQUE,
                      permissions TEXT
);

-- 2. Bảng Trạng thái bài đăng
CREATE TABLE Post_Statuses (
                              status_id INT AUTO_INCREMENT PRIMARY KEY,
                              status_name VARCHAR(50) NOT NULL UNIQUE,
                              description VARCHAR(255)
);

-- 3. Bảng Trạng thái giao dịch
CREATE TABLE Transaction_Statuses (
                                     status_id INT AUTO_INCREMENT PRIMARY KEY,
                                     status_name VARCHAR(50) NOT NULL UNIQUE
);

-- 4. Bảng Trạng thái khiếu nại
CREATE TABLE Complaint_Statuses (
                                   status_id INT AUTO_INCREMENT PRIMARY KEY,
                                   status_name VARCHAR(50) NOT NULL UNIQUE
);

-- 5. Bảng Danh mục tài khoản
CREATE TABLE Categories (
                          category_id INT AUTO_INCREMENT PRIMARY KEY,
                          category_name VARCHAR(100) NOT NULL UNIQUE,
                          category_icon VARCHAR(50),
                          parent_id INT,
                          display_order INT DEFAULT 0,
                          FOREIGN KEY (parent_id) REFERENCES Categories(category_id)
);

-- 6. Bảng Người dùng
CREATE TABLE Users (
                      user_id INT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      email VARCHAR(100) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      role_id INT,
                      created_by INT,
                      is_active BOOLEAN DEFAULT TRUE,
                      last_login DATETIME,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (role_id) REFERENCES Roles(role_id),
                      FOREIGN KEY (created_by) REFERENCES Users(user_id)
);

-- 7. Bảng Ví điện tử
CREATE TABLE Wallets (
                        wallet_id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT UNIQUE,
                        balance DECIMAL(15, 2) DEFAULT 0.00,
                        frozen_balance DECIMAL(15, 2) DEFAULT 0.00,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 8. Bảng Bài đăng (Posts)
CREATE TABLE Posts (
                      post_id INT AUTO_INCREMENT PRIMARY KEY,
                      seller_id INT,
                      title VARCHAR(255) NOT NULL,
                      price DECIMAL(15, 2) NOT NULL,
                      description LONGTEXT,
                      thumbnail_url VARCHAR(500),
                      category_id INT,
                      status_id INT,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      FOREIGN KEY (seller_id) REFERENCES Users(user_id),
                      FOREIGN KEY (category_id) REFERENCES Categories(category_id),
                      FOREIGN KEY (status_id) REFERENCES Post_Statuses(status_id)
);


-- 9. Bảng Thông tin tài khoản ẩn
CREATE TABLE Post_Credentials (
                                  credential_id INT AUTO_INCREMENT PRIMARY KEY,
                                  post_id INT UNIQUE,
                                  account_username VARCHAR(255) NOT NULL,
                                  account_password VARCHAR(255) NOT NULL,
                                  security_notes TEXT,
                                  FOREIGN KEY (post_id) REFERENCES Posts(post_id)
);

-- 10. Nhật ký xem thông tin ẩn
CREATE TABLE Credential_Access_Logs (
                                      log_id INT AUTO_INCREMENT PRIMARY KEY,
                                      credential_id INT,
                                      user_id INT,
                                      viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      FOREIGN KEY (credential_id) REFERENCES Post_Credentials(credential_id),
                                      FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 11. Bảng Giao dịch trung gian
CREATE TABLE Transactions (
                              transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                              post_id INT,
                              buyer_id INT,
                              seller_id INT,
                              amount DECIMAL(15, 2) NOT NULL,
                              fee DECIMAL(15, 2) DEFAULT 0.00,
                              status_id INT,
                              processor_id INT,
                              processed_at DATETIME,
                              admin_note TEXT,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (post_id) REFERENCES Posts(post_id),
                              FOREIGN KEY (buyer_id) REFERENCES Users(user_id),
                              FOREIGN KEY (seller_id) REFERENCES Users(user_id),
                              FOREIGN KEY (status_id) REFERENCES Transaction_Statuses(status_id),
                              FOREIGN KEY (processor_id) REFERENCES Users(user_id)
);

-- 12. Bảng Khiếu nại
CREATE TABLE Complaints (
                            complaint_id INT AUTO_INCREMENT PRIMARY KEY,
                            transaction_id INT,
                            sender_id INT,
                            reason TEXT NOT NULL,
                            evidence_url VARCHAR(500),
                            status_id INT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (transaction_id) REFERENCES Transactions(transaction_id),
                            FOREIGN KEY (sender_id) REFERENCES Users(user_id),
                            FOREIGN KEY (status_id) REFERENCES Complaint_Statuses(status_id)
);


-- ============ SEED DATA ============

-- Thêm các vai trò
INSERT INTO Roles (role_name) VALUES ('Admin'), ('Seller'), ('Buyer');

-- Thêm trạng thái bài đăng
INSERT INTO Post_Statuses (status_name) VALUES ('Available'), ('Holding'), ('Sold'), ('Hidden');

-- Thêm trạng thái giao dịch
INSERT INTO Transaction_Statuses (status_name) VALUES ('Pending'), ('Holding'), ('Completed'), ('Refunded');

-- Thêm trạng thái khiếu nại
INSERT INTO Complaint_Statuses (status_name) VALUES ('Open'), ('In Progress'), ('Resolved'), ('Rejected');

-- Thêm danh mục tài khoản
INSERT INTO Categories (category_name, category_icon, parent_id, display_order) VALUES
-- Giải trí (Entertainment)
('Giải trí', 'ph-game-controller', NULL, 1),
('Game', 'ph-game-controller', 1, 1),
('Netflix', 'ph-fill ph-play-circle', 1, 2),
('Spotify', 'ph-fill ph-music-notes', 1, 3),
('YouTube Premium', 'ph-fill ph-youtube-logo', 1, 4),
('Disney+', 'ph-fill ph-film-slate', 1, 5),
('Apple Music', 'ph-fill ph-apple-logo', 1, 6),

-- Làm việc (Work)
('Làm việc', 'ph-briefcase', NULL, 2),
('Slack', 'ph-fill ph-chat-centered-text', 2, 1),
('Zoom', 'ph-fill ph-video-camera', 2, 2),
('Microsoft Teams', 'ph-fill ph-users-three', 2, 3),
('Notion', 'ph-fill ph-notebook', 2, 4),
('Figma', 'ph-fill ph-paint-brush-broad', 2, 5),
('GitHub', 'ph-fill ph-github-logo', 2, 6),

-- Học tập (Learning)
('Học tập', 'ph-book-open', NULL, 3),
('Coursera', 'ph-fill ph-certificate', 3, 1),
('Udemy', 'ph-fill ph-play-circle', 3, 2),
('Skillshare', 'ph-fill ph-lightning', 3, 3),
('Duolingo', 'ph-fill ph-globe', 3, 4),
('LinkedIn Learning', 'ph-fill ph-linkedin-logo', 3, 5),
('Pluralsight', 'ph-fill ph-code', 3, 6),

-- eSIM du lịch
('eSIM Du lịch', 'ph-sim-card', NULL, 4),
('Airalo', 'ph-fill ph-globe', 4, 1),
('Holafly', 'ph-fill ph-airplane', 4, 2),
('GigSky', 'ph-fill ph-wifi-high', 4, 3),

-- Edit Ảnh - Video
('Edit Ảnh - Video', 'ph-camera', NULL, 5),
('Adobe Creative Cloud', 'ph-fill ph-palette', 5, 1),
('CapCut Pro', 'ph-fill ph-video', 5, 2),
('DaVinci Resolve', 'ph-fill ph-film-slate', 5, 3),
('Canva Pro', 'ph-fill ph-shapes', 5, 4),
('Lightroom', 'ph-fill ph-image', 5, 5),

-- Windows, Office
('Windows, Office', 'ph-desktop', NULL, 6),
('Microsoft 365', 'ph-fill ph-microsoft-logo', 6, 1),
('Windows 11 Pro', 'ph-fill ph-windows-logo', 6, 2),
('Office 2021', 'ph-fill ph-file-doc', 6, 3),
('Visio', 'ph-fill ph-chart-pie-slice', 6, 4),
('Project', 'ph-fill ph-kanban', 6, 5),

-- Google Drive
('Google Drive', 'ph-cloud', NULL, 7),
('Google One', 'ph-fill ph-google-logo', 7, 1),
('Google Workspace', 'ph-fill ph-google-logo', 7, 2),
('YouTube Music Premium', 'ph-fill ph-youtube-logo', 7, 3),

-- Thế giới AI
('Thế giới AI', 'ph-robot', NULL, 8),
('ChatGPT Plus', 'ph-fill ph-chat-circle-dots', 8, 1),
('Claude Pro', 'ph-fill ph-brain', 8, 2),
('Midjourney', 'ph-fill ph-image-square', 8, 3),
('DALL-E', 'ph-fill ph-paint-brush', 8, 4),
('Perplexity Pro', 'ph-fill ph-magnifying-glass', 8, 5),
('Copilot Pro', 'ph-fill ph-copilot-logo', 8, 6),
('Jasper AI', 'ph-fill ph-writing', 8, 7),

-- VPN, bảo mật mạng
('VPN, Bảo mật mạng', 'ph-shield-check', NULL, 9),
('NordVPN', 'ph-fill ph-shield-check', 9, 1),
('ExpressVPN', 'ph-fill ph-lock-key', 9, 2),
('Surfshark', 'ph-fill ph-wifi-high', 9, 3),
('1Password', 'ph-fill ph-key', 9, 4),
('LastPass', 'ph-fill ph-key', 9, 5),
('Dashlane', 'ph-fill ph-lock-locker', 9, 6),

-- Gift Card
('Gift Card', 'ph-ticket', NULL, 10),
('Steam Wallet', 'ph-fill ph-game-controller', 10, 1),
('PlayStation Store', 'ph-fill ph-playstation-logo', 10, 2),
('Xbox Gift Card', 'ph-fill ph-xbox-logo', 10, 3),
('iTunes/App Store', 'ph-fill ph-app-store-logo', 10, 4),
('Amazon Gift Card', 'ph-fill ph-shopping-bag', 10, 5),
('Google Play Gift Card', 'ph-fill ph-google-play-logo', 10, 6);
