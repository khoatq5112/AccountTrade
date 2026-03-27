CREATE DATABASE  IF NOT EXISTS `account_trading_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `account_trading_system`;
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
-- Table structure for table `admin_reviews`
--

DROP TABLE IF EXISTS `admin_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_reviews` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `action_notes` text,
  `action_taken` varchar(50) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `assigned_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `entity_id` bigint NOT NULL,
  `entity_type` varchar(50) NOT NULL,
  `findings` text,
  `issue_description` text NOT NULL,
  `priority` int DEFAULT NULL,
  `requires_approval` bit(1) DEFAULT NULL,
  `review_type` varchar(50) NOT NULL,
  `sla_deadline` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `system_flags` text,
  `updated_at` datetime(6) DEFAULT NULL,
  `approved_by_admin_id` int DEFAULT NULL,
  `assigned_admin_id` int DEFAULT NULL,
  `reviewed_by_admin_id` int DEFAULT NULL,
  PRIMARY KEY (`review_id`),
  KEY `FK8vod78o3rfdqfyi75aqx9qjbr` (`approved_by_admin_id`),
  KEY `FKr14vidpkwwprwv73i2ax8e3aa` (`assigned_admin_id`),
  KEY `FKatbhlxgc8iiragqbsqsct9ht4` (`reviewed_by_admin_id`),
  CONSTRAINT `FK8vod78o3rfdqfyi75aqx9qjbr` FOREIGN KEY (`approved_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKatbhlxgc8iiragqbsqsct9ht4` FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKr14vidpkwwprwv73i2ax8e3aa` FOREIGN KEY (`assigned_admin_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_reviews`
--

LOCK TABLES `admin_reviews` WRITE;
/*!40000 ALTER TABLE `admin_reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(100) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `entity_id` bigint DEFAULT NULL,
  `entity_type` varchar(50) DEFAULT NULL,
  `error_message` text,
  `event_type` varchar(50) NOT NULL,
  `http_method` varchar(10) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `metadata` text,
  `new_value` text,
  `old_value` text,
  `performer_role` varchar(20) DEFAULT NULL,
  `request_url` varchar(500) DEFAULT NULL,
  `success` bit(1) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `performed_by_id` int DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `FK6kkr1cx12j4vinn5ele5o88f4` (`performed_by_id`),
  CONSTRAINT `FK6kkr1cx12j4vinn5ele5o88f4` FOREIGN KEY (`performed_by_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `cart_id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`cart_id`),
  KEY `FKtmhqy3lekiruvnf9981odmu85` (`post_id`),
  KEY `FKb5o626f86h46m4s7ms6ginnop` (`user_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKtmhqy3lekiruvnf9981odmu85` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
/*!40000 ALTER TABLE `carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `display_order` int DEFAULT NULL,
  `category_icon` varchar(255) DEFAULT NULL,
  `category_name` varchar(255) NOT NULL,
  `commission_rate` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `UK41g4n0emuvcm3qyf1f6cn43c0` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,1,'ph-game-controller','Giải trí',NULL),(2,2,'ph-briefcase','Làm việc',NULL),(3,3,'ph-book-open','Học tập',NULL),(4,4,'ph-sim-card','eSIM du lịch',NULL),(5,5,'ph-camera','Edit Ảnh - Video',NULL),(6,6,'ph-desktop','Window Office',NULL),(7,7,'ph-cloud','Google Drive',NULL),(8,8,'ph-brain','Thế giới AI',NULL),(9,9,'ph-shield-check','VPN bảo mật mạng',NULL),(10,10,'ph-gift','Gift Card',NULL),(11,11,'ph-dots-three','Khác',NULL);
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `commission_configs`
--

DROP TABLE IF EXISTS `commission_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commission_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `global_rate_percent` decimal(5,2) NOT NULL,
  `max_rate_percent` decimal(5,2) NOT NULL,
  `min_rate_percent` decimal(5,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9owxwpvkq9hmp045cwsf7vt03` (`updated_by`),
  CONSTRAINT `FK9owxwpvkq9hmp045cwsf7vt03` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `commission_configs`
--

LOCK TABLES `commission_configs` WRITE;
/*!40000 ALTER TABLE `commission_configs` DISABLE KEYS */;
INSERT INTO `commission_configs` VALUES (1,5.00,100.00,0.00,'2026-03-26 20:22:58.140487',NULL);
/*!40000 ALTER TABLE `commission_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credential_access_logs`
--

DROP TABLE IF EXISTS `credential_access_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credential_access_logs` (
  `credential_id` int DEFAULT NULL,
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `viewed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `FKhlhju8rsl66imvcp6elrfa7fo` (`credential_id`),
  KEY `FKggh7r0uv46ga95d0ddaa2ogn8` (`user_id`),
  CONSTRAINT `FKggh7r0uv46ga95d0ddaa2ogn8` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKhlhju8rsl66imvcp6elrfa7fo` FOREIGN KEY (`credential_id`) REFERENCES `post_credentials` (`credential_id`)
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
-- Table structure for table `credential_assignments`
--

DROP TABLE IF EXISTS `credential_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credential_assignments` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NOT NULL,
  `assignment_status` varchar(30) NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `first_viewed_at` datetime(6) DEFAULT NULL,
  `last_view_ip` varchar(45) DEFAULT NULL,
  `replaced_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `status_reason` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `view_count` int DEFAULT NULL,
  `credential_id` int NOT NULL,
  `order_item_id` bigint NOT NULL,
  `replaced_by_assignment_id` bigint DEFAULT NULL,
  PRIMARY KEY (`assignment_id`),
  KEY `FK1t1assj4upu35j76w6spoot9x` (`credential_id`),
  KEY `FKqbv8vtb438v2sd0xgaykdr9sh` (`order_item_id`),
  KEY `FKntsv049lajqbgxlrms2vt6ir3` (`replaced_by_assignment_id`),
  CONSTRAINT `FK1t1assj4upu35j76w6spoot9x` FOREIGN KEY (`credential_id`) REFERENCES `post_credentials` (`credential_id`),
  CONSTRAINT `FKntsv049lajqbgxlrms2vt6ir3` FOREIGN KEY (`replaced_by_assignment_id`) REFERENCES `credential_assignments` (`assignment_id`),
  CONSTRAINT `FKqbv8vtb438v2sd0xgaykdr9sh` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credential_assignments`
--

LOCK TABLES `credential_assignments` WRITE;
/*!40000 ALTER TABLE `credential_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `credential_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credential_statuses`
--

DROP TABLE IF EXISTS `credential_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `credential_statuses` (
  `status_id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `status_name` varchar(255) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `UK4vc2kpro9lt6a7k5h2r6i94b2` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credential_statuses`
--

LOCK TABLES `credential_statuses` WRITE;
/*!40000 ALTER TABLE `credential_statuses` DISABLE KEYS */;
INSERT INTO `credential_statuses` VALUES (1,'Ready for sale','Available'),(2,'Held during transaction','Holding'),(3,'Sold to buyer','Sold'),(4,'Hidden from inventory','Hidden');
/*!40000 ALTER TABLE `credential_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dispute_messages`
--

DROP TABLE IF EXISTS `dispute_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dispute_messages` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `attachment_path` varchar(500) DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `has_attachment` bit(1) DEFAULT NULL,
  `is_internal` bit(1) DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `sender_ip` varchar(45) DEFAULT NULL,
  `sender_role` varchar(20) NOT NULL,
  `dispute_id` bigint NOT NULL,
  `sender_id` int NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `FK7w7adb3j48ka7vtvtevnghisp` (`dispute_id`),
  KEY `FKntkvplklxaikfb4l1r0e9o31m` (`sender_id`),
  CONSTRAINT `FK7w7adb3j48ka7vtvtevnghisp` FOREIGN KEY (`dispute_id`) REFERENCES `disputes` (`dispute_id`),
  CONSTRAINT `FKntkvplklxaikfb4l1r0e9o31m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dispute_messages`
--

LOCK TABLES `dispute_messages` WRITE;
/*!40000 ALTER TABLE `dispute_messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `dispute_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dispute_statuses`
--

DROP TABLE IF EXISTS `dispute_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dispute_statuses` (
  `status_id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `status_name` varchar(50) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `UKef24qcvug8gtp1c8h31jedt7w` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dispute_statuses`
--

LOCK TABLES `dispute_statuses` WRITE;
/*!40000 ALTER TABLE `dispute_statuses` DISABLE KEYS */;
INSERT INTO `dispute_statuses` VALUES (1,'Dispute has been opened','OPENED'),(2,'Dispute is being reviewed by admin','UNDER_REVIEW'),(3,'Dispute has been resolved','RESOLVED'),(4,'Dispute was cancelled','CANCELLED');
/*!40000 ALTER TABLE `dispute_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `disputes`
--

DROP TABLE IF EXISTS `disputes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disputes` (
  `dispute_id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `admin_review_started_at` datetime(6) DEFAULT NULL,
  `buyer_evidence` text,
  `buyer_escalated_at` datetime(6) DEFAULT NULL,
  `buyer_escalation_reason` text,
  `dispute_number` varchar(50) NOT NULL,
  `dispute_type` varchar(50) NOT NULL,
  `opened_at` datetime(6) NOT NULL,
  `reason` text NOT NULL,
  `resolution_notes` text,
  `resolution_type` varchar(50) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `seller_evidence` text,
  `seller_proposal_credential_id` int DEFAULT NULL,
  `seller_proposal_note` text,
  `seller_proposal_type` varchar(50) DEFAULT NULL,
  `seller_proposed_at` datetime(6) DEFAULT NULL,
  `seller_responded_at` datetime(6) DEFAULT NULL,
  `seller_response` text,
  `seller_response_deadline` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `assigned_admin_id` int DEFAULT NULL,
  `dispute_status_id` int NOT NULL,
  `opened_by_id` int NOT NULL,
  `order_id` bigint NOT NULL,
  `order_item_id` bigint DEFAULT NULL,
  `resolved_by_admin_id` int DEFAULT NULL,
  `respondent_id` int NOT NULL,
  PRIMARY KEY (`dispute_id`),
  UNIQUE KEY `UKig58ygrj3pcnlfmekbcyi1tr8` (`dispute_number`),
  KEY `FKb4n7vubxt33idhrop5mclb78m` (`assigned_admin_id`),
  KEY `FKi2qh1vleylai08mlbru9sak26` (`dispute_status_id`),
  KEY `FK7jqdb6madkggmxfc80ed4gyfj` (`opened_by_id`),
  KEY `FK7w9qai75udrw8yjppow8vqxa` (`order_id`),
  KEY `FKk5grvo8luhemt1e22hivoxlnf` (`order_item_id`),
  KEY `FKjf4xm3i12ko2rd0mfob35vx7` (`resolved_by_admin_id`),
  KEY `FK811tl08h8ud59n9jyxml4w5ke` (`respondent_id`),
  CONSTRAINT `FK7jqdb6madkggmxfc80ed4gyfj` FOREIGN KEY (`opened_by_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK7w9qai75udrw8yjppow8vqxa` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `FK811tl08h8ud59n9jyxml4w5ke` FOREIGN KEY (`respondent_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKb4n7vubxt33idhrop5mclb78m` FOREIGN KEY (`assigned_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKi2qh1vleylai08mlbru9sak26` FOREIGN KEY (`dispute_status_id`) REFERENCES `dispute_statuses` (`status_id`),
  CONSTRAINT `FKjf4xm3i12ko2rd0mfob35vx7` FOREIGN KEY (`resolved_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKk5grvo8luhemt1e22hivoxlnf` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `disputes`
--

LOCK TABLES `disputes` WRITE;
/*!40000 ALTER TABLE `disputes` DISABLE KEYS */;
/*!40000 ALTER TABLE `disputes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `escrow_statuses`
--

DROP TABLE IF EXISTS `escrow_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `escrow_statuses` (
  `status_id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `status_name` varchar(50) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `UKclinwr1526mw8ps3iqtmji7o8` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `escrow_statuses`
--

LOCK TABLES `escrow_statuses` WRITE;
/*!40000 ALTER TABLE `escrow_statuses` DISABLE KEYS */;
INSERT INTO `escrow_statuses` VALUES (1,'Escrow has not been created yet','NOT_CREATED'),(2,'Funds are being held in escrow','HOLDING'),(3,'Escrow is frozen due to dispute or review','FROZEN'),(4,'Funds have been released to seller','RELEASED'),(5,'Funds have been refunded to buyer','REFUNDED'),(6,'Funds have been partially refunded','PARTIALLY_REFUNDED');
/*!40000 ALTER TABLE `escrow_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `escrow_transactions`
--

DROP TABLE IF EXISTS `escrow_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `escrow_transactions` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(19,4) NOT NULL,
  `balance_after` decimal(19,4) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `initiator_ip` varchar(45) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `transaction_type` varchar(30) NOT NULL,
  `escrow_id` bigint NOT NULL,
  `initiated_by` int DEFAULT NULL,
  PRIMARY KEY (`transaction_id`),
  KEY `FKruf60fseaely9t6dswgvvqejf` (`escrow_id`),
  KEY `FK27k8413xvqd29xliloj1e16ns` (`initiated_by`),
  CONSTRAINT `FK27k8413xvqd29xliloj1e16ns` FOREIGN KEY (`initiated_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKruf60fseaely9t6dswgvvqejf` FOREIGN KEY (`escrow_id`) REFERENCES `escrows` (`escrow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `escrow_transactions`
--

LOCK TABLES `escrow_transactions` WRITE;
/*!40000 ALTER TABLE `escrow_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `escrow_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `escrows`
--

DROP TABLE IF EXISTS `escrows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `escrows` (
  `escrow_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(19,4) NOT NULL,
  `auto_release_deadline` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `frozen_at` datetime(6) DEFAULT NULL,
  `platform_fee` decimal(19,4) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `released_at` datetime(6) DEFAULT NULL,
  `seller_amount` decimal(19,4) DEFAULT NULL,
  `status_reason` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `escrow_status_id` int NOT NULL,
  `order_id` bigint NOT NULL,
  `processed_by_admin_id` int DEFAULT NULL,
  PRIMARY KEY (`escrow_id`),
  UNIQUE KEY `UKjnty8uthe0ac08vuiuscuxuiv` (`order_id`),
  KEY `FKtqn6lwgganvd1qa8vjmllbymw` (`escrow_status_id`),
  KEY `FK3iogo7irdqp7vsajyas1aia1c` (`processed_by_admin_id`),
  CONSTRAINT `FK3iogo7irdqp7vsajyas1aia1c` FOREIGN KEY (`processed_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK7lrymtvbnjgva1794jvyk41hf` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `FKtqn6lwgganvd1qa8vjmllbymw` FOREIGN KEY (`escrow_status_id`) REFERENCES `escrow_statuses` (`status_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `escrows`
--

LOCK TABLES `escrows` WRITE;
/*!40000 ALTER TABLE `escrows` DISABLE KEYS */;
/*!40000 ALTER TABLE `escrows` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_delivery_log`
--

DROP TABLE IF EXISTS `notification_delivery_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_delivery_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_count` int DEFAULT NULL,
  `attempted_at` datetime(6) NOT NULL,
  `channel` varchar(20) NOT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `error_message` text,
  `next_retry_at` datetime(6) DEFAULT NULL,
  `provider_response` text,
  `status` varchar(20) NOT NULL,
  `notification_id` bigint NOT NULL,
  PRIMARY KEY (`log_id`),
  KEY `FKcd3tjuve74nmv39xw3y3gph0h` (`notification_id`),
  CONSTRAINT `FKcd3tjuve74nmv39xw3y3gph0h` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`notification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_delivery_log`
--

LOCK TABLES `notification_delivery_log` WRITE;
/*!40000 ALTER TABLE `notification_delivery_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_delivery_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_preferences`
--

DROP TABLE IF EXISTS `notification_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_preferences` (
  `preference_id` bigint NOT NULL AUTO_INCREMENT,
  `category_preferences` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `digest_enabled` bit(1) DEFAULT NULL,
  `digest_frequency` varchar(20) DEFAULT NULL,
  `email_enabled` bit(1) DEFAULT NULL,
  `quiet_hours_enabled` bit(1) DEFAULT NULL,
  `quiet_hours_end` time DEFAULT NULL,
  `quiet_hours_start` time DEFAULT NULL,
  `quiet_hours_timezone` varchar(50) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`preference_id`),
  UNIQUE KEY `UKn2jopkbm16qv3xelbvoyjkd0g` (`user_id`),
  CONSTRAINT `FKt9qjvmcl36i14utm5uptyqg84` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_preferences`
--

LOCK TABLES `notification_preferences` WRITE;
/*!40000 ALTER TABLE `notification_preferences` DISABLE KEYS */;
INSERT INTO `notification_preferences` VALUES (1,'{\"POST\": {\"email\": false, \"inApp\": true}, \"ORDER\": {\"email\": true, \"inApp\": true}, \"PROMO\": {\"email\": false, \"inApp\": true}, \"ESCROW\": {\"email\": true, \"inApp\": true}, \"REVIEW\": {\"email\": false, \"inApp\": true}, \"SYSTEM\": {\"email\": true, \"inApp\": true}, \"WALLET\": {\"email\": true, \"inApp\": true}, \"DISPUTE\": {\"email\": true, \"inApp\": true}, \"PAYMENT\": {\"email\": true, \"inApp\": true}, \"CREDENTIAL\": {\"email\": true, \"inApp\": true}}','2026-03-27 00:31:41.530011',_binary '\0','INSTANT',_binary '',_binary '\0','08:00:00','22:00:00','Asia/Saigon','2026-03-27 00:31:41.530011',2),(2,'{\"POST\": {\"email\": true, \"inApp\": true}, \"ORDER\": {\"email\": true, \"inApp\": true}, \"PROMO\": {\"email\": false, \"inApp\": true}, \"ESCROW\": {\"email\": true, \"inApp\": true}, \"REVIEW\": {\"email\": false, \"inApp\": true}, \"SYSTEM\": {\"email\": true, \"inApp\": true}, \"WALLET\": {\"email\": true, \"inApp\": true}, \"DISPUTE\": {\"email\": true, \"inApp\": true}, \"PAYMENT\": {\"email\": true, \"inApp\": true}, \"CREDENTIAL\": {\"email\": true, \"inApp\": true}}','2026-03-27 00:31:57.430725',_binary '\0','INSTANT',_binary '',_binary '\0','08:00:00','22:00:00','Asia/Saigon','2026-03-27 00:31:57.430725',1),(3,'{\"POST\": {\"email\": true, \"inApp\": true}, \"USER\": {\"email\": true, \"inApp\": true}, \"ORDER\": {\"email\": true, \"inApp\": true}, \"PROMO\": {\"email\": false, \"inApp\": false}, \"ESCROW\": {\"email\": true, \"inApp\": true}, \"REVIEW\": {\"email\": true, \"inApp\": true}, \"SYSTEM\": {\"email\": true, \"inApp\": true}, \"WALLET\": {\"email\": true, \"inApp\": true}, \"DISPUTE\": {\"email\": true, \"inApp\": true}, \"PAYMENT\": {\"email\": true, \"inApp\": true}, \"CREDENTIAL\": {\"email\": true, \"inApp\": true}}','2026-03-27 00:33:38.102365',_binary '\0','INSTANT',_binary '',_binary '\0','08:00:00','22:00:00','Asia/Saigon','2026-03-27 00:33:38.102365',3);
/*!40000 ALTER TABLE `notification_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_templates`
--

DROP TABLE IF EXISTS `notification_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_templates` (
  `template_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `default_action_url_template` varchar(500) DEFAULT NULL,
  `default_priority` int DEFAULT NULL,
  `email_body_template` text,
  `email_subject_template` varchar(200) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `message_template` text NOT NULL,
  `notification_category` varchar(50) DEFAULT NULL,
  `notification_type` varchar(50) NOT NULL,
  `template_code` varchar(100) NOT NULL,
  `title_template` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `UK9lidgsja4g41dxgpy39fm29bg` (`template_code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_templates`
--

LOCK TABLES `notification_templates` WRITE;
/*!40000 ALTER TABLE `notification_templates` DISABLE KEYS */;
INSERT INTO `notification_templates` VALUES (1,'2026-03-27 00:25:40.763871','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Đơn hàng ${orderNumber} của bạn đã được tạo với tổng thanh toán ${totalAmount}.','ORDER','ORDER','ORDER_CREATED','Đơn hàng ${orderNumber} đã được tạo','2026-03-27 00:25:40.763871'),(2,'2026-03-27 00:25:40.801874','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Đơn hàng ${orderNumber} vừa được cập nhật trạng thái mới.','ORDER','ORDER','ORDER_STATUS_CHANGED','Đơn hàng ${orderNumber} đã cập nhật trạng thái','2026-03-27 00:25:40.801874'),(3,'2026-03-27 00:25:40.807873','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Hệ thống đã ghi nhận thanh toán ${amount} cho đơn ${orderNumber}.','PAYMENT','PAYMENT','PAYMENT_RECEIVED','Thanh toán cho ${orderNumber} đã thành công','2026-03-27 00:25:40.807873'),(4,'2026-03-27 00:25:40.811873','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Khoản thanh toán ${amount} của đơn ${orderNumber} đang được giữ trong escrow.','ESCROW','ESCROW','ESCROW_CREATED','Escrow đã được tạo cho ${orderNumber}','2026-03-27 00:25:40.811873'),(5,'2026-03-27 00:25:40.815879','/seller/orders',2,NULL,NULL,_binary '','Escrow của đơn ${orderNumber} đã được giải ngân thành công.','ESCROW','ESCROW','ESCROW_RELEASED','Escrow của ${orderNumber} đã được giải ngân','2026-03-27 00:25:40.816874'),(6,'2026-03-27 00:25:40.821875','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Khoản thanh toán của đơn ${orderNumber} đã được hoàn về cho bạn.','ESCROW','ESCROW','ESCROW_REFUNDED','Đơn ${orderNumber} đã được hoàn tiền','2026-03-27 00:25:40.821875'),(7,'2026-03-27 00:25:40.824876','/disputes/${disputeId}',2,NULL,NULL,_binary '','Tranh chấp ${disputeNumber} cho đơn ${orderNumber} đã được tạo với lý do: ${reason}.','DISPUTE','DISPUTE','DISPUTE_OPENED','Tranh chấp ${disputeNumber} đã được mở','2026-03-27 00:25:40.824876'),(8,'2026-03-27 00:25:40.829875','/disputes/${disputeId}',2,NULL,NULL,_binary '','Tranh chấp ${disputeNumber} của đơn ${orderNumber} đã có kết quả xử lý.','DISPUTE','DISPUTE','DISPUTE_RESOLVED','Tranh chấp ${disputeNumber} đã được xử lý','2026-03-27 00:25:40.829875'),(9,'2026-03-27 00:25:40.832877','/buyer/purchases?orderId=${orderId}',2,NULL,NULL,_binary '','Credential của đơn ${orderNumber} đã được bàn giao và sẵn sàng để bạn xem.','CREDENTIAL','CREDENTIAL','CREDENTIAL_ASSIGNED','Credential cho đơn ${orderNumber} đã sẵn sàng','2026-03-27 00:25:40.832877'),(10,'2026-03-27 00:25:40.837876','/seller/orders',2,NULL,NULL,_binary '','Một đơn hàng mới ${orderNumber} vừa được tạo cho sản phẩm của bạn.','ORDER','ORDER','NEW_SELLER_ORDER','Bạn có đơn hàng mới ${orderNumber}','2026-03-27 00:25:40.837876'),(11,'2026-03-27 00:25:40.840876','/buyer/wallet',2,NULL,NULL,_binary '','Giao dịch ${transactionId} đã cộng ${amount} vào ví. Số dư mới: ${balanceAfter}.','WALLET','WALLET','WALLET_CREDITED','Ví của bạn vừa được cộng ${amount}','2026-03-27 00:25:40.840876'),(12,'2026-03-27 00:25:40.844875','/',2,NULL,NULL,_binary '','${message}','SYSTEM','SYSTEM','SYSTEM_ALERT','${title}','2026-03-27 00:25:40.844875');
/*!40000 ALTER TABLE `notification_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `action_url` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email_sent` bit(1) DEFAULT NULL,
  `email_sent_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` text NOT NULL,
  `notification_type` varchar(50) NOT NULL,
  `priority` int DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `related_entity_id` bigint DEFAULT NULL,
  `related_entity_type` varchar(50) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` int NOT NULL,
  `archived_at` datetime(6) DEFAULT NULL,
  `email_error` text,
  `expires_at` datetime(6) DEFAULT NULL,
  `in_app_sent` bit(1) DEFAULT NULL,
  `in_app_sent_at` datetime(6) DEFAULT NULL,
  `is_archived` bit(1) DEFAULT NULL,
  `notification_category` varchar(50) DEFAULT NULL,
  `template_code` varchar(100) DEFAULT NULL,
  `template_variables` json DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  KEY `idx_user_unread` (`user_id`,`is_read`,`created_at`),
  KEY `idx_user_type` (`user_id`,`notification_type`),
  KEY `idx_entity` (`related_entity_type`,`related_entity_id`),
  KEY `idx_email_pending` (`email_sent`,`priority`,`created_at`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `order_item_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `credential_assigned_at` datetime(6) DEFAULT NULL,
  `item_status` varchar(50) NOT NULL,
  `platform_fee` decimal(19,4) DEFAULT NULL,
  `post_title_snapshot` varchar(255) NOT NULL,
  `refund_reason` varchar(500) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `seller_earnings` decimal(19,4) DEFAULT NULL,
  `unit_price` decimal(19,4) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `assigned_credential_id` int DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `post_id` int NOT NULL,
  `seller_id` int NOT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `FKsqa7ctn05a3q3jmv7acybd9w7` (`assigned_credential_id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKin76kdotxi07883jphw3fw8gd` (`post_id`),
  KEY `FKiteu7744jhts0njdk0g9cmew6` (`seller_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `FKin76kdotxi07883jphw3fw8gd` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `FKiteu7744jhts0njdk0g9cmew6` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKsqa7ctn05a3q3jmv7acybd9w7` FOREIGN KEY (`assigned_credential_id`) REFERENCES `post_credentials` (`credential_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_statuses`
--

DROP TABLE IF EXISTS `order_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_statuses` (
  `status_id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `status_name` varchar(50) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `UK8hx04g0c3ngcm6098i3m3nll2` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_statuses`
--

LOCK TABLES `order_statuses` WRITE;
/*!40000 ALTER TABLE `order_statuses` DISABLE KEYS */;
INSERT INTO `order_statuses` VALUES (1,'Order created but awaiting payment','PENDING'),(2,'Order is waiting for payment confirmation','AWAITING_PAYMENT'),(3,'Payment received, order is being processed','PAID'),(4,'Order is being processed, credentials being assigned','PROCESSING'),(5,'Credentials have been assigned to the order','CREDENTIAL_ASSIGNED'),(6,'Waiting for buyer to confirm credential validity','AWAITING_BUYER_CONFIRMATION'),(7,'Order is under dispute','DISPUTED'),(8,'Order completed successfully, escrow released','COMPLETED'),(9,'Order was cancelled','CANCELLED'),(10,'Order was refunded to buyer','REFUNDED'),(11,'Order expired due to payment timeout','PAYMENT_EXPIRED'),(12,'Payment failed for this order','PAYMENT_FAILED');
/*!40000 ALTER TABLE `order_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `buyer_ip_address` varchar(45) DEFAULT NULL,
  `buyer_notes` text,
  `buyer_user_agent` varchar(500) DEFAULT NULL,
  `cancellation_reason` varchar(500) DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `confirmation_deadline` datetime(6) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `credential_assigned_at` datetime(6) DEFAULT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `order_number` varchar(50) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `platform_fee` decimal(19,4) DEFAULT NULL,
  `subtotal` decimal(19,4) NOT NULL,
  `total_amount` decimal(19,4) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `buyer_id` int NOT NULL,
  `order_status_id` int NOT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `UKnthkiu7pgmnqnu86i2jyoe2v7` (`order_number`),
  KEY `FKhtx3insd5ge6w486omk4fnk54` (`buyer_id`),
  KEY `FKcbbqf26brulgfgvd0mf74rv4y` (`order_status_id`),
  CONSTRAINT `FKcbbqf26brulgfgvd0mf74rv4y` FOREIGN KEY (`order_status_id`) REFERENCES `order_statuses` (`status_id`),
  CONSTRAINT `FKhtx3insd5ge6w486omk4fnk54` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_callbacks`
--

DROP TABLE IF EXISTS `payment_callbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_callbacks` (
  `callback_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint DEFAULT NULL,
  `callback_type` varchar(20) NOT NULL,
  `computed_hash` varchar(256) DEFAULT NULL,
  `hash_valid` bit(1) DEFAULT NULL,
  `processed` bit(1) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `processing_result` varchar(500) DEFAULT NULL,
  `raw_query_string` text,
  `received_at` datetime(6) NOT NULL,
  `received_hash` varchar(256) DEFAULT NULL,
  `source_ip` varchar(45) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `vnpay_response_code` varchar(10) DEFAULT NULL,
  `vnpay_transaction_no` varchar(100) DEFAULT NULL,
  `vnpay_txn_ref` varchar(100) DEFAULT NULL,
  `payment_id` bigint DEFAULT NULL,
  PRIMARY KEY (`callback_id`),
  KEY `FKhbvgvd9xp3ivs8ife1e7pym2f` (`payment_id`),
  CONSTRAINT `FKhbvgvd9xp3ivs8ife1e7pym2f` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_callbacks`
--

LOCK TABLES `payment_callbacks` WRITE;
/*!40000 ALTER TABLE `payment_callbacks` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment_callbacks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_statuses`
--

DROP TABLE IF EXISTS `payment_statuses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_statuses` (
  `status_id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `status_name` varchar(50) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `UKo6las05xmph4if0d1cdd5nvsv` (`status_name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_statuses`
--

LOCK TABLES `payment_statuses` WRITE;
/*!40000 ALTER TABLE `payment_statuses` DISABLE KEYS */;
INSERT INTO `payment_statuses` VALUES (1,'Payment has been initiated, awaiting VNPAY redirect','INITIATED'),(2,'Payment is pending at VNPAY','PENDING'),(3,'Payment confirmed successful via IPN','PAID'),(4,'Payment failed','FAILED'),(5,'Payment callback data mismatch - requires manual review','CALLBACK_MISMATCH'),(6,'Payment has been refunded','REFUNDED');
/*!40000 ALTER TABLE `payment_statuses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(19,4) NOT NULL,
  `bank_code` varchar(50) DEFAULT NULL,
  `card_type` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(3) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `failed_at` datetime(6) DEFAULT NULL,
  `ipn_callback_count` int DEFAULT NULL,
  `ipn_processed` bit(1) DEFAULT NULL,
  `last_ipn_at` datetime(6) DEFAULT NULL,
  `order_info` varchar(500) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `raw_callback_data` text,
  `secure_hash` varchar(256) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `vnpay_ip_address` varchar(45) DEFAULT NULL,
  `vnpay_pay_date` datetime(6) DEFAULT NULL,
  `vnpay_response_code` varchar(10) DEFAULT NULL,
  `vnpay_transaction_no` varchar(100) DEFAULT NULL,
  `vnpay_txn_ref` varchar(100) NOT NULL,
  `order_id` bigint NOT NULL,
  `payment_status_id` int NOT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `UK2mdh41eu3qtjkug2s5fk73ho0` (`vnpay_txn_ref`),
  KEY `FK81gagumt0r8y3rmudcgpbk42l` (`order_id`),
  KEY `FK3ahqbxi8ygjekxaa1atyycpr0` (`payment_status_id`),
  CONSTRAINT `FK3ahqbxi8ygjekxaa1atyycpr0` FOREIGN KEY (`payment_status_id`) REFERENCES `payment_statuses` (`status_id`),
  CONSTRAINT `FK81gagumt0r8y3rmudcgpbk42l` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_earnings`
--

DROP TABLE IF EXISTS `platform_earnings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_earnings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `commission_amount` decimal(19,4) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `gross_amount` decimal(19,4) NOT NULL,
  `rate_applied` decimal(5,2) NOT NULL,
  `category_id` int DEFAULT NULL,
  `escrow_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1gns758pxc4b95ocxndfdkc7c` (`category_id`),
  KEY `FK2pfu7o6t1wo1k9r34uomjqv0a` (`escrow_id`),
  KEY `FK7eq6aq0hk2gn09v9belrn1crj` (`order_id`),
  CONSTRAINT `FK1gns758pxc4b95ocxndfdkc7c` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `FK2pfu7o6t1wo1k9r34uomjqv0a` FOREIGN KEY (`escrow_id`) REFERENCES `escrows` (`escrow_id`),
  CONSTRAINT `FK7eq6aq0hk2gn09v9belrn1crj` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_earnings`
--

LOCK TABLES `platform_earnings` WRITE;
/*!40000 ALTER TABLE `platform_earnings` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_earnings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_credentials`
--

DROP TABLE IF EXISTS `post_credentials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_credentials` (
  `credential_id` int NOT NULL AUTO_INCREMENT,
  `credential_status_id` int NOT NULL,
  `post_id` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `account_password` varchar(255) NOT NULL,
  `account_username` varchar(255) NOT NULL,
  `security_notes` text,
  PRIMARY KEY (`credential_id`),
  KEY `FKkwkgpvhwaudb3gghsx25xoctc` (`credential_status_id`),
  KEY `FKau0bm95cax58a80s31v4odqsy` (`post_id`),
  CONSTRAINT `FKau0bm95cax58a80s31v4odqsy` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `FKkwkgpvhwaudb3gghsx25xoctc` FOREIGN KEY (`credential_status_id`) REFERENCES `credential_statuses` (`status_id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_credentials`
--

LOCK TABLES `post_credentials` WRITE;
/*!40000 ALTER TABLE `post_credentials` DISABLE KEYS */;
INSERT INTO `post_credentials` VALUES (1,1,1,'2026-03-27 07:53:55.000000','NetFlix@2024#Vip','netflix.premium.vn1@gmail.com','Profile 1 - Tài khoản chính, hạn đến 15/04/2026'),(2,1,1,'2026-03-27 07:53:55.000000','NetFlix@2024#Vip','netflix.premium.vn2@gmail.com','Profile 2 - Hạn đến 15/04/2026'),(3,1,1,'2026-03-27 07:53:55.000000','NetFlix@2024#Vip','netflix.premium.vn3@gmail.com','Profile 3 - Hạn đến 15/04/2026'),(4,1,2,'2026-03-27 07:53:55.000000','Spotify@Premium2024','spotify.vip.user1@gmail.com','Tài khoản cá nhân, hạn đến 20/04/2026'),(5,1,2,'2026-03-27 07:53:55.000000','Spotify@Premium2024','spotify.vip.user2@gmail.com','Tài khoản cá nhân, hạn đến 20/04/2026'),(6,1,3,'2026-03-27 07:53:55.000000','YT@Premium2024!Vn','youtube.premium.vn1@gmail.com','Hạn đến 25/04/2026'),(7,1,3,'2026-03-27 07:53:55.000000','YT@Premium2024!Vn','youtube.premium.vn2@gmail.com','Hạn đến 25/04/2026'),(8,1,4,'2026-03-27 07:53:55.000000','Disney@Hotstar2024','disney.vip.family1@gmail.com','Profile 1 - Hạn đến 30/04/2026'),(9,1,4,'2026-03-27 07:53:55.000000','Disney@Hotstar2024','disney.vip.family2@gmail.com','Profile 2 - Hạn đến 30/04/2026'),(10,1,5,'2026-03-27 07:53:55.000000','HBO@Go2024!Asia','hbogo.premium.asia1@gmail.com','Hạn đến 10/05/2026'),(11,1,6,'2026-03-27 07:53:55.000000','MS365@Personal2024','ms365.personal.vn1@outlook.com','License key activated, hạn 1 năm từ ngày kích hoạt'),(12,1,6,'2026-03-27 07:53:55.000000','MS365@Personal2024','ms365.personal.vn2@outlook.com','License key activated, hạn 1 năm từ ngày kích hoạt'),(13,1,7,'2026-03-27 07:53:55.000000','Notion@Plus2024!Pro','notion.plus.user1@gmail.com','Hạn đến 01/03/2027'),(14,1,8,'2026-03-27 07:53:55.000000','Slack@Pro2024#Team','slack.pro.admin1@gmail.com','Admin workspace, hạn đến 15/04/2026'),(15,1,9,'2026-03-27 07:53:55.000000','Trello@Premium2024','trello.premium.work@gmail.com','Hạn đến 01/06/2027'),(16,1,10,'2026-03-27 07:53:55.000000','Zoom@Pro2024!Meet','zoom.pro.meeting1@gmail.com','Hạn đến 20/04/2026'),(17,1,10,'2026-03-27 07:53:55.000000','Zoom@Pro2024!Meet','zoom.pro.meeting2@gmail.com','Hạn đến 20/04/2026'),(18,1,11,'2026-03-27 07:53:55.000000','Duo@Max2024!Learn','duolingo.max.vn1@gmail.com','Hạn đến 01/03/2027'),(19,1,11,'2026-03-27 07:53:55.000000','Duo@Max2024!Learn','duolingo.max.vn2@gmail.com','Hạn đến 01/03/2027'),(20,1,12,'2026-03-27 07:53:55.000000','Coursera@Plus2024','coursera.plus.learner1@gmail.com','Hạn đến 15/06/2027'),(21,1,13,'2026-03-27 07:53:55.000000','Skill@Share2024!Art','skillshare.premium.art1@gmail.com','Hạn đến 01/04/2027'),(22,1,14,'2026-03-27 07:53:55.000000','Grammar@Ly2024!Write','grammarly.premium.write1@gmail.com','Hạn đến 01/05/2027'),(23,1,14,'2026-03-27 07:53:55.000000','Grammar@Ly2024!Write','grammarly.premium.write2@gmail.com','Hạn đến 01/05/2027'),(24,1,15,'2026-03-27 07:53:55.000000','Quill@Bot2024!Para','quillbot.premium.edu1@gmail.com','Hạn đến 01/06/2027'),(25,1,16,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-TH-2024-001','QR Code sẽ được gửi qua email sau khi mua'),(26,1,16,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-TH-2024-002','QR Code sẽ được gửi qua email sau khi mua'),(27,1,16,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-TH-2024-003','QR Code sẽ được gửi qua email sau khi mua'),(28,1,17,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-KR-2024-001','QR Code sẽ được gửi qua email sau khi mua'),(29,1,17,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-KR-2024-002','QR Code sẽ được gửi qua email sau khi mua'),(30,1,18,'2026-03-27 07:53:55.000000','Activation: QR Code via email','ESIM-JP-2024-001','QR Code sẽ được gửi qua email sau khi mua'),(31,1,19,'2026-03-27 07:53:55.000000','Adobe@CC2024!Creative','adobe.cc.premium1@gmail.com','Hạn đến 01/06/2027'),(32,1,19,'2026-03-27 07:53:55.000000','Adobe@CC2024!Creative','adobe.cc.premium2@gmail.com','Hạn đến 01/06/2027'),(33,1,20,'2026-03-27 07:53:55.000000','Canva@Pro2024!Design','canva.pro.design1@gmail.com','Hạn đến 01/04/2027'),(34,1,20,'2026-03-27 07:53:55.000000','Canva@Pro2024!Design','canva.pro.design2@gmail.com','Hạn đến 01/04/2027'),(35,1,20,'2026-03-27 07:53:55.000000','Canva@Pro2024!Design','canva.pro.design3@gmail.com','Hạn đến 01/04/2027'),(36,1,21,'2026-03-27 07:53:55.000000','CapCut@Pro2024!Edit','capcut.pro.editor1@gmail.com','Hạn đến 01/05/2027'),(37,1,21,'2026-03-27 07:53:55.000000','CapCut@Pro2024!Edit','capcut.pro.editor2@gmail.com','Hạn đến 01/05/2027'),(38,1,22,'2026-03-27 07:53:55.000000','Figma@Pro2024!UI','figma.pro.designer1@gmail.com','Hạn đến 01/06/2027'),(39,1,23,'2026-03-27 07:53:55.000000','N/A - License Key Only','VK7JG-NPHTM-C97JM-9MPGT-3V66T','Windows 11 Pro Retail Key - 1 PC'),(40,1,23,'2026-03-27 07:53:55.000000','N/A - License Key Only','W269N-WFGWX-YVC9B-4J6C9-T83GX','Windows 11 Pro Retail Key - 1 PC'),(41,1,24,'2026-03-27 07:53:55.000000','Key: VYBBJ-TRJPB-QFQRF-QFT4D-H3GVB','OFFICE2021-PRO-001','Office 2021 Pro Plus - 1 PC'),(42,1,24,'2026-03-27 07:53:55.000000','Key: XQNVK-8J9DB-PJ3P8-RRQJF-KM4HM','OFFICE2021-PRO-002','Office 2021 Pro Plus - 1 PC'),(43,1,25,'2026-03-27 07:53:55.000000','VS@Enterprise2024','vs.enterprise.dev1@outlook.com','Subscription active until 01/03/2027'),(44,1,26,'2026-03-27 07:53:55.000000','Google@One2024!200GB','googleone.storage1@gmail.com','Hạn đến 01/04/2027'),(45,1,26,'2026-03-27 07:53:55.000000','Google@One2024!200GB','googleone.storage2@gmail.com','Hạn đến 01/04/2027'),(46,1,27,'2026-03-27 07:53:55.000000','Google@One2024!2TB','googleone.premium1@gmail.com','Hạn đến 01/06/2027'),(47,1,28,'2026-03-27 07:53:55.000000','Dropbox@Plus2024!2TB','dropbox.plus.user1@gmail.com','Hạn đến 01/05/2027'),(48,1,29,'2026-03-27 07:53:55.000000','ChatGPT@Plus2024!AI','chatgpt.plus.user1@gmail.com','Hạn đến 15/04/2026'),(49,1,29,'2026-03-27 07:53:55.000000','ChatGPT@Plus2024!AI','chatgpt.plus.user2@gmail.com','Hạn đến 15/04/2026'),(50,1,30,'2026-03-27 07:53:55.000000','Claude@Pro2024!Anthropic','claude.pro.user1@gmail.com','Hạn đến 20/04/2026'),(51,1,31,'2026-03-27 07:53:55.000000','Mid@Journey2024!Art','midjourney.premium1@gmail.com','Discord account - Hạn đến 01/05/2026'),(52,1,32,'2026-03-27 07:53:55.000000','Perplexity@Pro2024!Search','perplexity.pro.search1@gmail.com','Hạn đến 01/03/2027'),(53,1,33,'2026-03-27 07:53:55.000000','Nord@VPN2024!Secure','nordvpn.premium1@gmail.com','Hạn đến 01/03/2028'),(54,1,33,'2026-03-27 07:53:55.000000','Nord@VPN2024!Secure','nordvpn.premium2@gmail.com','Hạn đến 01/03/2028'),(55,1,34,'2026-03-27 07:53:55.000000','Express@VPN2024!Fast','expressvpn.user1@gmail.com','Hạn đến 01/06/2027'),(56,1,35,'2026-03-27 07:53:55.000000','Surf@Shark2024!Unlimited','surfshark.unlimited1@gmail.com','Hạn đến 01/03/2028'),(57,1,35,'2026-03-27 07:53:55.000000','Surf@Shark2024!Unlimited','surfshark.unlimited2@gmail.com','Hạn đến 01/03/2028'),(58,1,36,'2026-03-27 07:53:55.000000','Code: A1B2C-D3E4F-G5H6I','STEAM-GC-US-001','Steam Wallet Code $20 US'),(59,1,36,'2026-03-27 07:53:55.000000','Code: J7K8L-M9N0P-Q1R2S','STEAM-GC-US-002','Steam Wallet Code $20 US'),(60,1,37,'2026-03-27 07:53:55.000000','Code: XXXXXX-XXXXXX-XXXXXX','ITUNES-GC-US-001','iTunes Code $25 US'),(61,1,38,'2026-03-27 07:53:55.000000','Code: PLAY-XXXX-XXXX-XXXX','GOOGLEPLAY-GC-001','Google Play Code $20 US'),(62,1,38,'2026-03-27 07:53:55.000000','Code: PLAY-YYYY-YYYY-YYYY','GOOGLEPLAY-GC-002','Google Play Code $20 US'),(63,1,39,'2026-03-27 07:53:55.000000','Code: SPOT-XXXX-XXXX-XXXX','SPOTIFY-GC-US-001','Spotify Gift Code $30 US'),(64,1,40,'2026-03-27 07:53:55.000000','Discord@Nitro2024!Boost','discord.nitro.user1@gmail.com','Hạn đến 20/04/2026'),(65,1,40,'2026-03-27 07:53:55.000000','Discord@Nitro2024!Boost','discord.nitro.user2@gmail.com','Hạn đến 20/04/2026'),(66,1,41,'2026-03-27 07:53:55.000000','Tinder@Gold2024!Match','tinder.gold.user1@gmail.com','Hạn đến 25/04/2026'),(67,1,42,'2026-03-27 07:53:55.000000','LinkedIn@Premium2024!Job','linkedin.premium.career1@gmail.com','Hạn đến 30/04/2026'),(68,1,43,'2026-03-27 07:53:55.000000','Ever@Note2024!Personal','evernote.personal.note1@gmail.com','Hạn đến 01/05/2027'),(69,1,44,'2026-03-27 07:53:55.000000','Last@Pass2024!Secure','lastpass.premium.secure1@gmail.com','Hạn đến 01/06/2027'),(70,1,44,'2026-03-27 07:53:55.000000','Last@Pass2024!Secure','lastpass.premium.secure2@gmail.com','Hạn đến 01/06/2027'),(71,1,45,'2026-03-27 07:53:55.000000','Crunchy@Roll2024!Anime','crunchyroll.premium.anime1@gmail.com','Hạn đến 01/04/2027');
/*!40000 ALTER TABLE `post_credentials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `category_id` int DEFAULT NULL,
  `post_id` int NOT NULL AUTO_INCREMENT,
  `price` decimal(15,2) NOT NULL,
  `seller_id` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `description` longtext,
  `stock_status` enum('IN_STOCK','OUT_OF_STOCK') DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  KEY `FKijnwr3brs8vaosl80jg9rp7uc` (`category_id`),
  KEY `FKrse09yns7yyoqmyio4bon3i2g` (`seller_id`),
  CONSTRAINT `FKijnwr3brs8vaosl80jg9rp7uc` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `FKrse09yns7yyoqmyio4bon3i2g` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
INSERT INTO `posts` VALUES (1,1,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/netflix-premium-4k.svg','Netflix Premium 4K - 1 Tháng','<p>Tài khoản Netflix Premium 4K chính chủ, xem được trên4 thiết bị cùng lúc.</p><p><strong>Tính năng:</strong></p><ul><li>Độ phân giải 4K Ultra HD</li><li>Xem trên4 thiết bị cùng lúc</li><li>Không quảng cáo</li><li>Truy cập toàn bộ thư viện phim</li></ul><p><em>Lưu ý: Không đổi mật khẩu, không thay đổi thông tin tài khoản.</em></p>','IN_STOCK'),(1,2,29000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/spotify-premium.svg','Spotify Premium Cá Nhân - 1 Tháng','<p>Tài khoản Spotify Premium cá nhân, nghe nhạc không giới hạn.</p><p><strong>Tính năng:</strong></p><ul><li>Nghe nhạc không quảng cáo</li><li>Tải xuống để nghe offline</li><li>Chất lượng âm thanh cao 320kbps</li><li>Chọn bài hát bất kỳ</li></ul>','IN_STOCK'),(1,3,39000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/youtube-premium.svg','YouTube Premium - 1 Tháng','<p>YouTube Premium không quảng cáo, chạy nền và YouTube Music.</p><p><strong>Tính năng:</strong></p><ul><li>Xem video không quảng cáo</li><li>Chạy nền khi tắt màn hình</li><li>YouTube Music Premium</li><li>Tải video xem offline</li></ul>','IN_STOCK'),(1,4,59000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/disney-hotstar.svg','Disney+ Hotstar Premium - 1 Tháng','<p>Disney+ Hotstar với đầy đủ phim Marvel, Star Wars, Pixar.</p><p><strong>Tính năng:</strong></p><ul><li>Toàn bộ thư viện Disney, Marvel, Star Wars</li><li>Chất lượng 4K HDR</li><li>Xem trên4 thiết bị</li><li>Phụ đề đa ngôn ngữ</li></ul>','IN_STOCK'),(1,5,69000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/hbo-go.svg','HBO GO Premium - 1 Tháng','<p>HBO GO với đầy đủ series và phim độc quyền.</p><p><strong>Tính năng:</strong></p><ul><li>Toàn bộ thư viện HBO</li><li>Phim mới nhất từ Hollywood</li><li>Series độc quyền</li><li>Chất lượng HD</li></ul>','IN_STOCK'),(2,6,89000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/microsoft-365.svg','Microsoft 365 Personal - 1 Năm','<p>Microsoft 365 Personal đầy đủ Word, Excel, PowerPoint, 1TB OneDrive.</p><p><strong>Bao gồm:</strong></p><ul><li>Word, Excel, PowerPoint</li><li>Outlook, OneNote</li><li>1TB OneDrive</li><li>Cài đặt trên1 PC/Mac</li></ul>','IN_STOCK'),(2,7,79000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/notion-plus.svg','Notion Plus - 1 Năm','<p>Notion Plus với không giới hạn file upload và advanced features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited file uploads</li><li>Unlimited blocks</li><li>30 day version history</li><li>Priority support</li></ul>','IN_STOCK'),(2,8,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/slack.svg','Slack Pro Workspace - 1 Tháng','<p>Slack Pro với đầy đủ tính năng cho team làm việc.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited message history</li><li>Unlimited integrations</li><li>Group video calls</li><li>Advanced security</li></ul>','IN_STOCK'),(2,9,69000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/trello.svg','Trello Premium - 1 Năm','<p>Trello Premium với advanced automation và views.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Power-Ups</li><li>Calendar View</li><li>Timeline View</li><li>Advanced automation</li></ul>','IN_STOCK'),(2,10,39000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/zoom.svg','Zoom Pro - 1 Tháng','<p>Zoom Pro với không giới hạn thời gian họp.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited group meetings</li><li>Meeting duration up to 30 hours</li><li>1GB cloud recording</li><li>Streaming to social media</li></ul>','IN_STOCK'),(3,11,299000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/duolingo-max.svg','Duolingo Max - 1 Năm','<p>Duolingo Max với AI-powered learning features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited hearts</li><li>Personalized practice</li><li>AI-powered conversations</li><li>Explain my answer feature</li></ul>','IN_STOCK'),(3,12,199000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/coursera.svg','Coursera Plus - 1 Năm','<p>Coursera Plus truy cập không giới hạn hơn 7000 khóa học.</p><p><strong>Tính năng:</strong></p><ul><li>Access to 7000+ courses</li><li>Unlimited certificates</li><li>Learn at your own pace</li><li>Projects and specializations</li></ul>','IN_STOCK'),(3,13,99000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/skillshare.svg','Skillshare Premium - 1 Năm','<p>Skillshare Premium với hàng ngàn khóa học creative.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited classes</li><li>Offline access</li><li>No ads</li><li>Access to community</li></ul>','IN_STOCK'),(3,14,79000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/grammarly.svg','Grammarly Premium - 1 Năm','<p>Grammarly Premium với AI writing assistant.</p><p><strong>Tính năng:</strong></p><ul><li>Advanced grammar check</li><li>Clarity suggestions</li><li>Tone detection</li><li>Plagiarism checker</li></ul>','IN_STOCK'),(3,15,59000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/quillbot.svg','QuillBot Premium - 1 Năm','<p>QuillBot Premium với AI paraphrasing tool.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited paraphrasing</li><li>All7 writing modes</li><li>Plagiarism checker</li><li>Grammar checker</li></ul>','IN_STOCK'),(4,16,89000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/esim-thailand.svg','eSIM Thái Lan 7 Ngày - 5GB','<p>eSIM Thái Lan data roaming, không cần đổi sim.</p><p><strong>Thông tin:</strong></p><ul><li>5GB data tốc độ cao</li><li>Hạn sử dụng 7 ngày</li><li>Không cần đổi SIM</li><li>Hoạt động ngay sau khi kích hoạt</li></ul>','IN_STOCK'),(4,17,129000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/esim-southkorea.svg','eSIM Hàn Quốc 10 Ngày - 8GB','<p>eSIM Hàn Quốc data roaming cho du lịch.</p><p><strong>Thông tin:</strong></p><ul><li>8GB data tốc độ cao</li><li>Hạn sử dụng 10 ngày</li><li>Hỗ trợ 4G/LTE</li><li>Không cần đăng ký</li></ul>','IN_STOCK'),(4,18,159000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/esim-japan.svg','eSIM Nhật Bản 15 Ngày - 10GB','<p>eSIM Nhật Bản data roaming chất lượng cao.</p><p><strong>Thông tin:</strong></p><ul><li>10GB data tốc độ cao</li><li>Hạn sử dụng 15 ngày</li><li>Coverage toàn Nhật Bản</li><li>4G/LTE tốc độ cao</li></ul>','IN_STOCK'),(5,19,199000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/adobe-cc.svg','Adobe Creative Cloud All Apps - 1 Năm','<p>Adobe Creative Cloud đầy đủ 20+ ứng dụng.</p><p><strong>Bao gồm:</strong></p><ul><li>Photoshop, Lightroom</li><li>Premiere Pro, After Effects</li><li>Illustrator, InDesign</li><li>100GB cloud storage</li></ul>','IN_STOCK'),(5,20,59000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/canva.svg','Canva Pro - 1 Năm','<p>Canva Pro với đầy đủ template và tính năng nâng cao.</p><p><strong>Tính năng:</strong></p><ul><li>Millions of premium templates</li><li>100GB cloud storage</li><li>Remove background</li><li>Brand Kit</li></ul>','IN_STOCK'),(5,21,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/capcut.svg','CapCut Pro - 1 Năm','<p>CapCut Pro với đầy đủ hiệu ứng và tính năng edit video.</p><p><strong>Tính năng:</strong></p><ul><li>Premium effects & filters</li><li>No watermark</li><li>4K export</li><li>Cloud storage</li></ul>','IN_STOCK'),(5,22,89000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/figma.svg','Figma Professional - 1 Năm','<p>Figma Professional với advanced design features.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Figma files</li><li>Unlimited version history</li><li>Sharing permissions</li><li>Team libraries</li></ul>','IN_STOCK'),(6,23,590000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/windows-11.svg','Windows 11 Pro License Key','<p>Windows 11 Pro License Key bản quyền vĩnh viễn.</p><p><strong>Tính năng:</strong></p><ul><li>License key vĩnh viễn</li><li>Kích hoạt online</li><li>Hỗ trợ cài đặt lại</li><li>Full features Pro</li></ul>','IN_STOCK'),(6,24,890000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/office-2021.svg','Microsoft Office 2021 Professional Plus','<p>Microsoft Office 2021 Professional Plus bản quyền vĩnh viễn.</p><p><strong>Bao gồm:</strong></p><ul><li>Word 2021</li><li>Excel 2021</li><li>PowerPoint 2021</li><li>Outlook, Access, Publisher</li></ul>','IN_STOCK'),(6,25,199000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/visual-studio.svg','Visual Studio Enterprise 2022 - 1 Năm','<p>Visual Studio Enterprise 2022 subscription.</p><p><strong>Tính năng:</strong></p><ul><li>Full IDE features</li><li>Azure DevOps</li><li>Cloud services</li><li>Technical support</li></ul>','IN_STOCK'),(7,26,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/google-one-200gb.svg','Google One 200GB - 1 Năm','<p>Google One 200GB lưu trữ đám mây.</p><p><strong>Tính năng:</strong></p><ul><li>200GB storage</li><li>Google Photos backup</li><li>Google Drive</li><li>Gmail storage</li></ul>','IN_STOCK'),(7,27,99000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/google-one-2tb.svg','Google One 2TB - 1 Năm','<p>Google One 2TB lưu trữ đám mây cao cấp.</p><p><strong>Tính năng:</strong></p><ul><li>2TB storage</li><li>VPN for Android</li><li>Priority support</li><li>Family sharing</li></ul>','IN_STOCK'),(7,28,79000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/dropbox.svg','Dropbox Plus 2TB - 1 Năm','<p>Dropbox Plus với 2TB lưu trữ.</p><p><strong>Tính năng:</strong></p><ul><li>2TB storage</li><li>File recovery</li><li>Offline access</li><li>Computer backup</li></ul>','IN_STOCK'),(8,29,99000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/chatgpt.svg','ChatGPT Plus - 1 Tháng','<p>ChatGPT Plus với GPT-4 và các tính năng nâng cao.</p><p><strong>Tính năng:</strong></p><ul><li>Access to GPT-4</li><li>Faster response times</li><li>Priority access to new features</li><li>Available during peak times</li></ul>','IN_STOCK'),(8,30,89000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/claude.svg','Claude Pro - 1 Tháng','<p>Claude Pro với Claude 3 Opus.</p><p><strong>Tính năng:</strong></p><ul><li>Access to Claude 3 Opus</li><li>Higher message limits</li><li>Priority access</li><li>Advanced reasoning</li></ul>','IN_STOCK'),(8,31,199000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/midjourney.svg','Midjourney Premium - 1 Tháng','<p>Midjourney Premium với unlimited image generation.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited image generations</li><li>Fast GPU time</li><li>Relax mode</li><li>Private generation</li></ul>','IN_STOCK'),(8,32,149000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/perplexity.svg','Perplexity Pro - 1 Năm','<p>Perplexity Pro với AI-powered search.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited Pro searches</li><li>GPT-4 access</li><li>Claude access</li><li>File upload analysis</li></ul>','IN_STOCK'),(9,33,89000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/nordvpn.svg','NordVPN Premium - 2 Năm','<p>NordVPN Premium với 5000+ servers worldwide.</p><p><strong>Tính năng:</strong></p><ul><li>5000+ servers in 60 countries</li><li>No logs policy</li><li>6 devices simultaneously</li><li>Threat protection</li></ul>','IN_STOCK'),(9,34,129000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/expressvpn.svg','ExpressVPN - 1 Năm','<p>ExpressVPN với tốc độ nhanh nhất.</p><p><strong>Tính năng:</strong></p><ul><li>Ultra-fast servers</li><li>160 locations in 94 countries</li><li>5 devices simultaneously</li><li>24/7 support</li></ul>','IN_STOCK'),(9,35,69000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/surfshark.svg','Surfshark VPN - 2 Năm','<p>Surfshark VPN với unlimited devices.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited devices</li><li>3200+ servers</li><li>CleanWeb ad blocker</li><li>NoBorders mode</li></ul>','IN_STOCK'),(10,36,490000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/steam.svg','Steam Gift Card $20 USD','<p>Steam Gift Card $20 USD - Region: US.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $20 USD</li><li>Region: US</li><li>Redeem immediately</li><li>No expiration</li></ul>','IN_STOCK'),(10,37,599000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/itunes.svg','iTunes Gift Card $25 USD','<p>iTunes Gift Card $25 USD - Region: US.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $25 USD</li><li>Region: US</li><li>For App Store, iTunes, Apple Music</li><li>No expiration</li></ul>','IN_STOCK'),(10,38,479000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/google-play.svg','Google Play Gift Card $20 USD','<p>Google Play Gift Card $20 USD.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $20 USD</li><li>Region: US</li><li>For apps, games, movies</li><li>No expiration</li></ul>','IN_STOCK'),(10,39,699000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/spotify-gift.svg','Spotify Gift Card $30 USD','<p>Spotify Gift Card $30 USD for Premium subscription.</p><p><strong>Thông tin:</strong></p><ul><li>Value: $30 USD</li><li>Region: US</li><li>3 months Premium</li><li>No expiration</li></ul>','IN_STOCK'),(11,40,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/discord.svg','Discord Nitro - 1 Tháng','<p>Discord Nitro với đầy đủ tính năng premium.</p><p><strong>Tính năng:</strong></p><ul><li>Custom emoji anywhere</li><li>Animated avatar</li><li>Server boost</li><li>HD streaming</li></ul>','IN_STOCK'),(11,41,79000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/tinder.svg','Tinder Gold - 1 Tháng','<p>Tinder Gold với các tính năng premium dating.</p><p><strong>Tính năng:</strong></p><ul><li>See who likes you</li><li>Unlimited likes</li><li>Passport to any location</li><li>Rewind last swipe</li></ul>','IN_STOCK'),(11,42,99000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/linkedin.svg','LinkedIn Premium Career - 1 Tháng','<p>LinkedIn Premium Career cho job seekers.</p><p><strong>Tính năng:</strong></p><ul><li>See who viewed your profile</li><li>InMail messages</li><li>Salary insights</li><li>Interview prep</li></ul>','IN_STOCK'),(11,43,69000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/evernote.svg','Evernote Personal - 1 Năm','<p>Evernote Personal với unlimited notes.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited notes</li><li>10GB uploads/month</li><li>Offline access</li><li>PDF search</li></ul>','IN_STOCK'),(11,44,49000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/lastpass.svg','LastPass Premium - 1 Năm','<p>LastPass Premium password manager.</p><p><strong>Tính năng:</strong></p><ul><li>Unlimited passwords</li><li>Cross-device sync</li><li>Dark web monitoring</li><li>1GB secure storage</li></ul>','IN_STOCK'),(11,45,199000.00,1,'2026-03-27 07:53:55.000000','2026-03-27 07:53:55.000000','https://res.cloudinary.com/dj5wpyfvh/image/upload/w_300,h_200,c_fill/v1773755979/account-trade/posts/crunchyroll.svg','Crunchyroll Premium - 1 Năm','<p>Crunchyroll Premium Mega Fan với anime không giới hạn.</p><p><strong>Tính năng:</strong></p><ul><li>Ad-free anime</li><li>Offline viewing</li><li>4 devices</li><li>New episodes 1 hour after Japan</li></ul>','IN_STOCK');
/*!40000 ALTER TABLE `posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refund_requests`
--

DROP TABLE IF EXISTS `refund_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refund_requests` (
  `refund_id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `approved_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `original_amount` decimal(19,4) NOT NULL,
  `platform_fee_retained` decimal(19,4) DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `reason` text NOT NULL,
  `refund_amount` decimal(19,4) NOT NULL,
  `refund_number` varchar(50) NOT NULL,
  `refund_type` varchar(30) NOT NULL,
  `rejection_reason` text,
  `requested_at` datetime(6) NOT NULL,
  `status` varchar(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  `vnpay_refund_txn_ref` varchar(100) DEFAULT NULL,
  `vnpay_response_code` varchar(10) DEFAULT NULL,
  `admin_review_id` bigint DEFAULT NULL,
  `approved_by_admin_id` int DEFAULT NULL,
  `dispute_id` bigint DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `processed_by_admin_id` int DEFAULT NULL,
  `requested_by_id` int NOT NULL,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `UKi5nou7ioxlogdp71mtr383j99` (`refund_number`),
  KEY `FKs5grm6xkmgl1n412rg3xy0gnx` (`admin_review_id`),
  KEY `FKa20vwpq1jm9la74aj1pn83qvv` (`approved_by_admin_id`),
  KEY `FKnjlt10a9q1qkssaet233e3by0` (`dispute_id`),
  KEY `FK452xm7hwgngbanwkdgs3601b1` (`order_id`),
  KEY `FKfsottrkircbtajrrq7qgfu9bm` (`processed_by_admin_id`),
  KEY `FKg1vs5jtjl9jwnmsbgrg57ws4t` (`requested_by_id`),
  CONSTRAINT `FK452xm7hwgngbanwkdgs3601b1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `FKa20vwpq1jm9la74aj1pn83qvv` FOREIGN KEY (`approved_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKfsottrkircbtajrrq7qgfu9bm` FOREIGN KEY (`processed_by_admin_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKg1vs5jtjl9jwnmsbgrg57ws4t` FOREIGN KEY (`requested_by_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKnjlt10a9q1qkssaet233e3by0` FOREIGN KEY (`dispute_id`) REFERENCES `disputes` (`dispute_id`),
  CONSTRAINT `FKs5grm6xkmgl1n412rg3xy0gnx` FOREIGN KEY (`admin_review_id`) REFERENCES `admin_reviews` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refund_requests`
--

LOCK TABLES `refund_requests` WRITE;
/*!40000 ALTER TABLE `refund_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `refund_requests` ENABLE KEYS */;
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
  UNIQUE KEY `UK716hgxp60ym1lifrdgp67xt5k` (`role_name`)
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
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `created_by` int DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `role_id` int DEFAULT NULL,
  `user_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FKibk1e3kaxy5sfyeekp8hbhnim` (`created_by`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  CONSTRAINT `FKibk1e3kaxy5sfyeekp8hbhnim` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (NULL,_binary '',2,1,'2026-03-17 09:15:44.048855',NULL,'sellerAccount','khoatqhe150834@gmail.com','$2a$10$F0UQ9FX6Y7pLxXOZwHIkyOXTVr/kGMD2wX94EJbXNZROeoLWjOX5u'),(NULL,_binary '',3,2,'2026-03-17 21:33:30.021786',NULL,'buyerAccount','quangkhoa5112@gmail.com','$2a$10$g4MSbareTZdn6E6NQw1FOOvn0zuHDJFsctQBaZdj246Jz4JpIXN8q'),(NULL,_binary '',1,3,'2026-03-17 21:33:30.021786',NULL,'adminAccount','quangkhoa.dev@gmail.com','$2a$10$g4MSbareTZdn6E6NQw1FOOvn0zuHDJFsctQBaZdj246Jz4JpIXN8q');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_topups`
--

DROP TABLE IF EXISTS `wallet_topups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_topups` (
  `top_up_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `pending_post_id` int DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `vnpay_transaction_no` varchar(100) DEFAULT NULL,
  `vnpay_txn_ref` varchar(100) NOT NULL,
  `wallet_id` int NOT NULL,
  `required_amount` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`top_up_id`),
  UNIQUE KEY `UK4sn393ml9ev64ubxteityskp5` (`vnpay_txn_ref`),
  KEY `FKgcabh1c5wnt2ot4j1jgfuvgh9` (`wallet_id`),
  CONSTRAINT `FKgcabh1c5wnt2ot4j1jgfuvgh9` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`wallet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_topups`
--

LOCK TABLES `wallet_topups` WRITE;
/*!40000 ALTER TABLE `wallet_topups` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_topups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_transactions`
--

DROP TABLE IF EXISTS `wallet_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transactions` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `balance_after` decimal(15,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `reference_id` varchar(100) DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `wallet_id` int NOT NULL,
  PRIMARY KEY (`transaction_id`),
  KEY `FK8seu7b87ifqi09ghhssusmb0x` (`wallet_id`),
  CONSTRAINT `FK8seu7b87ifqi09ghhssusmb0x` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`wallet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_transactions`
--

LOCK TABLES `wallet_transactions` WRITE;
/*!40000 ALTER TABLE `wallet_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallets`
--

DROP TABLE IF EXISTS `wallets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallets` (
  `balance` decimal(15,2) DEFAULT NULL,
  `frozen_balance` decimal(15,2) DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `wallet_id` int NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  CONSTRAINT `chk_wallets_non_negative_balance` CHECK ((`balance` >= 0) and (`frozen_balance` >= 0)),
  PRIMARY KEY (`wallet_id`),
  UNIQUE KEY `UKsswfdl9fq40xlkove1y5kc7kv` (`user_id`),
  CONSTRAINT `FKc1foyisidw7wqqrkamafuwn4e` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallets`
--

LOCK TABLES `wallets` WRITE;
/*!40000 ALTER TABLE `wallets` DISABLE KEYS */;
INSERT INTO `wallets` VALUES (0.00,0.00,1,1,'2026-03-27 07:53:42.000000',0),(0.00,0.00,2,2,'2026-03-27 07:53:42.000000',0),(0.00,0.00,3,3,'2026-03-27 07:53:42.000000',0);
/*!40000 ALTER TABLE `wallets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'account_trading_system'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-27  7:56:30
