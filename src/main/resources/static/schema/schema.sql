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


-- 5. Bảng Người dùng
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

-- 6. Bảng Ví điện tử
CREATE TABLE Wallets (
                         wallet_id INT AUTO_INCREMENT PRIMARY KEY,
                         user_id INT UNIQUE,
                         balance DECIMAL(15, 2) DEFAULT 0.00,
                         frozen_balance DECIMAL(15, 2) DEFAULT 0.00,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 7. Bảng Bài đăng (Posts)
CREATE TABLE Posts (
                       post_id INT AUTO_INCREMENT PRIMARY KEY,
                       seller_id INT,
                       title VARCHAR(255) NOT NULL,
                       price DECIMAL(15, 2) NOT NULL,
                       description LONGTEXT, -- Lưu trữ HTML từ Rich Text Editor
                       thumbnail_url VARCHAR(500),
                       category VARCHAR(100),
                       status_id INT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       FOREIGN KEY (seller_id) REFERENCES Users(user_id),
                       FOREIGN KEY (status_id) REFERENCES Post_Statuses(status_id)
);


-- 8. Bảng Thông tin tài khoản ẩn
CREATE TABLE Post_Credentials (
                                  credential_id INT AUTO_INCREMENT PRIMARY KEY,
                                  post_id INT UNIQUE,
                                  account_username VARCHAR(255) NOT NULL,
                                  account_password VARCHAR(255) NOT NULL,
                                  security_notes TEXT,
                                  FOREIGN KEY (post_id) REFERENCES Posts(post_id)
);

-- 9. Nhật ký xem thông tin ẩn
CREATE TABLE Credential_Access_Logs (
                                        log_id INT AUTO_INCREMENT PRIMARY KEY,
                                        credential_id INT,
                                        user_id INT,
                                        viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (credential_id) REFERENCES Post_Credentials(credential_id),
                                        FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 10. Bảng Giao dịch trung gian
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

-- 11. Bảng Khiếu nại
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



-- Thêm các vai trò
INSERT INTO Roles (role_name) VALUES ('Admin'), ('Seller'), ('Buyer');

-- Thêm trạng thái bài đăng
INSERT INTO Post_Statuses (status_name) VALUES ('Available'), ('Holding'), ('Sold'), ('Hidden');

-- Thêm trạng thái giao dịch
INSERT INTO Transaction_Statuses (status_name) VALUES ('Pending'), ('Holding'), ('Completed'), ('Refunded');