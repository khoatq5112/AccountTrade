# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**TrustBridge Market** - A Spring Boot account trading marketplace platform acting as a trusted intermediary between buyers and sellers. The admin acts as escrow for secure transactions.

## Tech Stack

- **Framework**: Spring Boot 4.0.3-SNAPSHOT
- **Language**: Java 17
- **Build**: Maven (mvnw wrapper available)
- **Frontend**: Thymeleaf + HTML/CSS
- **Database**: MySQL (localhost:3306/account_trading_system)
- **Security**: Spring Security with role-based access
- **Email**: Gmail SMTP for OTP and notifications

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AccountTradeApplicationTests

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

The server runs on port **8081**.

## Architecture

### Entities (src/main/java/com/group3/accounttrade/entity/)
- **User** - Base user with roles (BUYER, SELLER, ADMIN)
- **Wallet** - User wallet for holding funds
- **Post** - Account listings for sale
- **PostCredential** - Encrypted credentials for sold accounts
- **Transaction** - Escrow transactions between buyer/seller
- **Complaint** - Dispute filings
- **CredentialAccessLog** - Access logs for account credentials
- **Role**, **PostStatus**, **TransactionStatus**, **ComplaintStatus** - Enums

### Controllers (src/main/java/com/group3/accounttrade/controller/)
- **AuthController** - Login, register, OTP verification, password reset
- **HomeController** - Homepage and general routes
- **BuyerController** - Buyer dashboard and purchase flows
- **SellerController** - Seller dashboard and listing management
- **AdminController** - Admin dashboard and system management

### Services (src/main/java/com/group3/accounttrade/service/)
- **UserService** - User CRUD and management
- **EmailService** - Transactional emails
- **OtpService** - OTP generation and validation
- **CustomUserDetailsService** - Spring Security user loading

### Config (src/main/java/com/group3/accounttrade/config/)
- **SecurityConfig** - Security filter chains and authorization rules
- **AsyncConfig** - Async task executor configuration
- **CustomAuthenticationSuccessHandler/FailureHandler** - Auth flow customization

## Design System

The frontend follows the TrustBridge design system (see `DESIGN.md`):
- **Primary**: #137fec (blue for CTAs and branding)
- **Background**: #ffffff (white cards on #f5f7fa gray surface)
- **Success**: #22c55e (green for verified/completed)
- **Warning**: #f59e0b (orange for pending)
- **Error**: #ef4444 (red for errors/risks)
- **Font**: Manrope (bold headings, regular body)
- Cards with subtle rounding and soft shadows

## Database

Hibernate auto-creates tables (`spring.jpa.hibernate.ddl-auto=update`). The schema is in `src/main/resources/static/schema/schema.sql` for reference.

## Site Roadmap (from SITE.md)

Completed:
- Homepage with hero and featured listings
- Login/Register with KYC elements
- User dashboard (Buyer/Seller)
- Admin dashboard

In Progress:
- Marketplace listing page with filters
- Account detail page
- Account listing page for sellers

Pending:
- Dispute center
- About & Trust/Security page
- FAQ & Support page
- 2FA configuration screen
