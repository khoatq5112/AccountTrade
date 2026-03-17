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

# Run specific test method
./mvnw test -Dtest=AccountTradeApplicationTests#testMethodName

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

The server runs on port **8081**.

## Architecture

### Frontend Organization
- **Templates**: `src/main/resources/templates/` - Thymeleaf HTML pages
- **Static Assets**: `src/main/resources/static/` - CSS, JS, images
- **Schema**: `src/main/resources/static/schema/schema.sql` - Database schema reference

### Build Dependencies (pom.xml)
- Spring Boot Starter Web, Security, Data JPA, Thymeleaf, Validation
- MySQL Connector, Lombok
- Spring Boot Test, JUnit 5

### Security Config
- **Roles**: BUYER, SELLER, ADMIN (defined in Role enum)
- **Auth Flow**: Login → OTP verification → Role-based dashboard redirect
- **Key Files**:
  - `SecurityConfig` - Filter chains, authorization rules
  - `CustomAuthenticationSuccessHandler/FailureHandler` - Auth redirects
  - `CustomUserDetailsService` - User loading for Spring Security
- **User** - Base user with roles (BUYER, SELLER, ADMIN)
- **Wallet** - User wallet for holding funds
- **Post** - Account listings for sale
- **PostCredential** - Encrypted credentials for sold accounts
- **Transaction** - Escrow transactions between buyer/seller
- **Complaint** - Dispute filings
- **CredentialAccessLog** - Access logs for account credentials
- **Cart** - Shopping cart for buyer purchases
- **Category** - Hierarchical categories for marketplace listings
- **Role**, **PostStatus**, **TransactionStatus**, **ComplaintStatus** - Enums

### Controllers (src/main/java/com/group3/accounttrade/controller/)
- **AuthController** - Login, register, OTP verification, password reset
- **HomeController** - Homepage and general routes
- **BuyerController** - Buyer dashboard and purchase flows
- **SellerController** - Seller dashboard and listing management
- **AdminController** - Admin dashboard and system management
- **MarketplaceController** - Marketplace listing, search, category browsing
- **CartController** - Shopping cart operations

### Services (src/main/java/com/group3/accounttrade/service/)
- **UserService** - User CRUD and management
- **EmailService** - Transactional emails
- **OtpService** - OTP generation and validation
- **CustomUserDetailsService** - Spring Security user loading
- **CartService** - Shopping cart operations
- **CategoryService** - Category management

### Config (src/main/java/com/group3/accounttrade/config/)
- **SecurityConfig** - Security filter chains and authorization rules
- **AsyncConfig** - Async task executor configuration
- **CustomAuthenticationSuccessHandler/FailureHandler** - Auth flow customization
- **DataInitializer** - Initializes roles, statuses, and categories on startup

### Repositories (src/main/java/com/group3/accounttrade/repository/)
- **UserRepository**, **PostRepository**, **CartRepository** - Standard JPA repositories
- **CategoryRepository** - Supports hierarchical category queries
- **RoleRepository**, **PostStatusRepository**, **TransactionStatusRepository** - Lookup tables

## Design System

The frontend follows the TrustBridge design system (see `DESIGN.md`):
- **Primary**: #137fec (blue for CTAs and branding)
- **Background**: #ffffff (white cards on #f5f7fa gray surface)
- **Success**: #22c55e (green for verified/completed)
- **Warning**: #f59e0b (orange for pending)
- **Error**: #ef4444 (red for errors/risks)
- **Font**: Manrope (bold headings, regular body)
- Cards with subtle rounding and soft shadows

**Note**: The project uses Phosphor Icons via CDN for UI icons.

## Site Roadmap (from SITE.md)

Completed:
- Homepage with hero and featured listings
- Login/Register with OTP verification
- User dashboard (Buyer/Seller)
- Admin dashboard
- Marketplace listing page with filters
- Account listing page for sellers
- Shopping cart

In Progress:
- Account detail page
- Checkout and escrow flow

Pending:
- Dispute center
- About & Trust/Security page
- FAQ & Support page
- 2FA configuration screen

## Database

- **Host**: localhost:3306
- **Database**: account_trading_system
- **Credentials**: root/root (check application.properties)
- **Hibernate**: Auto-creates tables (`spring.jpa.hibernate.ddl-auto=update`)
- Schema reference: `src/main/resources/static/schema/schema.sql`

**Note**: Email credentials are stored in application.properties - ensure these are overridden via environment variables in production.

## Feature Implementation

When implementing new pages or features, reference `next-prompt.md` for page-specific design requirements including structure, components, and UI patterns.
