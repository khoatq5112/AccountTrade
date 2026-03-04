-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: account_trading_system
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `complaint_statuses`
--

DROP TABLE IF EXISTS `complaint_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint_statuses` (
                                      `status_id` int NOT NULL AUTO_INCREMENT,
                                      `status_name` varchar(50) NOT NULL,
                                      PRIMARY KEY (`status_id`),
                                      UNIQUE KEY `status_name` (`status_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `complaint_statuses`
--

LOCK TABLES `complaint_statuses` WRITE;
/*!40000 ALTER TABLE `complaint_statuses` DISABLE KEYS */;
/*!40000 ALTER TABLE `complaint_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `complaints`
--

DROP TABLE IF EXISTS `complaints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaints` (
                              `complaint_id` int NOT NULL AUTO_INCREMENT,
                              `transaction_id` int DEFAULT NULL,
                              `sender_id` int DEFAULT NULL,
                              `reason` text NOT NULL,
                              `evidence_url` varchar(500) DEFAULT NULL,
                              `status_id` int DEFAULT NULL,
                              `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`complaint_id`),
                              KEY `transaction_id` (`transaction_id`),
                              KEY `sender_id` (`sender_id`),
                              KEY `status_id` (`status_id`),
                              CONSTRAINT `complaints_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`transaction_id`),
                              CONSTRAINT `complaints_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
                              CONSTRAINT `complaints_ibfk_3` FOREIGN KEY (`status_id`) REFERENCES `complaint_statuses` (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `complaints`
--

LOCK TABLES `complaints` WRITE;
/*!40000 ALTER TABLE `complaints` DISABLE KEYS */;
/*!40000 ALTER TABLE `complaints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credential_access_logs`
--

DROP TABLE IF EXISTS `credential_access_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credential_access_logs` (
                                          `log_id` int NOT NULL AUTO_INCREMENT,
                                          `credential_id` int DEFAULT NULL,
                                          `user_id` int DEFAULT NULL,
                                          `viewed_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`log_id`),
                                          KEY `credential_id` (`credential_id`),
                                          KEY `user_id` (`user_id`),
                                          CONSTRAINT `credential_access_logs_ibfk_1` FOREIGN KEY (`credential_id`) REFERENCES `post_credentials` (`credential_id`),
                                          CONSTRAINT `credential_access_logs_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credential_access_logs`
--

LOCK TABLES `credential_access_logs` WRITE;
/*!40000 ALTER TABLE `credential_access_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `credential_access_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_credentials`
--

DROP TABLE IF EXISTS `post_credentials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_credentials` (
                                    `credential_id` int NOT NULL AUTO_INCREMENT,
                                    `post_id` int DEFAULT NULL,
                                    `account_username` varchar(255) NOT NULL,
                                    `account_password` varchar(255) NOT NULL,
                                    `security_notes` text,
                                    PRIMARY KEY (`credential_id`),
                                    UNIQUE KEY `post_id` (`post_id`),
                                    CONSTRAINT `post_credentials_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_credentials`
--

LOCK TABLES `post_credentials` WRITE;
/*!40000 ALTER TABLE `post_credentials` DISABLE KEYS */;
/*!40000 ALTER TABLE `post_credentials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_statuses`
--

DROP TABLE IF EXISTS `post_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_statuses` (
                                 `status_id` int NOT NULL AUTO_INCREMENT,
                                 `status_name` varchar(50) NOT NULL,
                                 `description` varchar(255) DEFAULT NULL,
                                 PRIMARY KEY (`status_id`),
                                 UNIQUE KEY `status_name` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_statuses`
--

LOCK TABLES `post_statuses` WRITE;
/*!40000 ALTER TABLE `post_statuses` DISABLE KEYS */;
INSERT INTO `post_statuses` VALUES (1,'Available',NULL),(2,'Holding',NULL),(3,'Sold',NULL),(4,'Hidden',NULL);
/*!40000 ALTER TABLE `post_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
                         `post_id` int NOT NULL AUTO_INCREMENT,
                         `seller_id` int DEFAULT NULL,
                         `title` varchar(255) NOT NULL,
                         `price` decimal(15,2) NOT NULL,
                         `description` longtext,
                         `thumbnail_url` varchar(500) DEFAULT NULL,
                         `category` varchar(100) DEFAULT NULL,
                         `status_id` int DEFAULT NULL,
                         `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`post_id`),
                         KEY `seller_id` (`seller_id`),
                         KEY `status_id` (`status_id`),
                         CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`),
                         CONSTRAINT `posts_ibfk_2` FOREIGN KEY (`status_id`) REFERENCES `post_statuses` (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
/*!40000 ALTER TABLE `posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
                         `role_id` int NOT NULL AUTO_INCREMENT,
                         `role_name` varchar(50) NOT NULL,
                         `permissions` text,
                         PRIMARY KEY (`role_id`),
                         UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'Admin',NULL),(2,'Seller',NULL),(3,'Buyer',NULL);
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction_statuses`
--

DROP TABLE IF EXISTS `transaction_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction_statuses` (
                                        `status_id` int NOT NULL AUTO_INCREMENT,
                                        `status_name` varchar(50) NOT NULL,
                                        PRIMARY KEY (`status_id`),
                                        UNIQUE KEY `status_name` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction_statuses`
--

LOCK TABLES `transaction_statuses` WRITE;
/*!40000 ALTER TABLE `transaction_statuses` DISABLE KEYS */;
INSERT INTO `transaction_statuses` VALUES (3,'Completed'),(2,'Holding'),(1,'Pending'),(4,'Refunded');
/*!40000 ALTER TABLE `transaction_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
                                `transaction_id` int NOT NULL AUTO_INCREMENT,
                                `post_id` int DEFAULT NULL,
                                `buyer_id` int DEFAULT NULL,
                                `seller_id` int DEFAULT NULL,
                                `amount` decimal(15,2) NOT NULL,
                                `fee` decimal(15,2) DEFAULT '0.00',
                                `status_id` int DEFAULT NULL,
                                `processor_id` int DEFAULT NULL,
                                `processed_at` datetime DEFAULT NULL,
                                `admin_note` text,
                                `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`transaction_id`),
                                KEY `post_id` (`post_id`),
                                KEY `buyer_id` (`buyer_id`),
                                KEY `seller_id` (`seller_id`),
                                KEY `status_id` (`status_id`),
                                KEY `processor_id` (`processor_id`),
                                CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
                                CONSTRAINT `transactions_ibfk_2` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`user_id`),
                                CONSTRAINT `transactions_ibfk_3` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`),
                                CONSTRAINT `transactions_ibfk_4` FOREIGN KEY (`status_id`) REFERENCES `transaction_statuses` (`status_id`),
                                CONSTRAINT `transactions_ibfk_5` FOREIGN KEY (`processor_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `user_id` int NOT NULL AUTO_INCREMENT,
                         `username` varchar(50) NOT NULL,
                         `email` varchar(100) NOT NULL,
                         `password_hash` varchar(255) NOT NULL,
                         `role_id` int DEFAULT NULL,
                         `created_by` int DEFAULT NULL,
                         `is_active` tinyint(1) DEFAULT '1',
                         `last_login` datetime DEFAULT NULL,
                         `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`user_id`),
                         UNIQUE KEY `username` (`username`),
                         UNIQUE KEY `email` (`email`),
                         KEY `role_id` (`role_id`),
                         KEY `created_by` (`created_by`),
                         CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
                         CONSTRAINT `users_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'khoa5112k','quangkhoa5112@gmail.com','$2a$10$3GK5.CFRrKwSXdz21BYSMeUrphlusiN8aOzpzZogmEjhQ5hl9ccT6',3,NULL,1,NULL,'2026-02-25 12:27:27'),(4,'admin','khoatqhe150834@gmail.com','$2a$10$1VbLOBwgjtd1BQ3R9uwH4.wXU9A.v2hu23nafRR6XPakkJW7eqob.',3,NULL,1,NULL,'2026-03-02 14:16:55');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;  

-- email: khoatqhe150834@gmail.com -- password: khoa123456789

UNLOCK TABLES;

--
-- Table structure for table `wallets`
--

DROP TABLE IF EXISTS `wallets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallets` (
                           `wallet_id` int NOT NULL AUTO_INCREMENT,
                           `user_id` int DEFAULT NULL,
                           `balance` decimal(15,2) DEFAULT '0.00',
                           `frozen_balance` decimal(15,2) DEFAULT '0.00',
                           `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`wallet_id`),
                           UNIQUE KEY `user_id` (`user_id`),
                           CONSTRAINT `wallets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallets`
--

LOCK TABLES `wallets` WRITE;
/*!40000 ALTER TABLE `wallets` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallets` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-04 20:53:35
