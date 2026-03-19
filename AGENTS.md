# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build/Run Commands

```bash
./mvnw spring-boot:run                          # Start server on port8081
./mvnw test -Dtest=ClassName                    # Run single test class
./mvnw test -Dtest=ClassName#methodName         # Run specific test method
```

## Critical Architecture Patterns

**Status Entity Pattern**: All status values (OrderStatus, EscrowStatus, CredentialStatus) are database entities, NOT enums. Always look up status via repository:

```java
OrderStatus status = orderStatusRepository.findByStatusName("PROCESSING")
    .orElseThrow(() -> new IllegalStateException("Status not found"));
```

**Credential Lifecycle**: AVAILABLE → HOLDING (reserved) → ASSIGNED (paid) → SOLD (confirmed) / DISPUTED / REPLACED

**Escrow Lifecycle**: HOLDING → FROZEN (dispute) → RELEASED (seller paid) / REFUNDED (buyer refunded)

**Auto-release**: Escrows auto-release after24 hours via `@Scheduled` in [`EscrowService.java`](src/main/java/com/group3/accounttrade/service/EscrowService.java:358)

## Non-Obvious Conventions

- **Port**:8081 (not default 8080) - configured in [`application.properties`](src/main/resources/application.properties:3)
- **Open Session in View**: Enabled for lazy loading during template rendering
- **Lombok DI**: Use `@RequiredArgsConstructor` with `final` fields for constructor injection
- **DataInitializer**: Seeds roles, statuses, categories on startup - check [`DataInitializer.java`](src/main/java/com/group3/accounttrade/config/DataInitializer.java) before adding new seed data
- **Platform fee**: Configurable via `escrow.platform-fee-percent` property (default5%)

## Service Coupling

- [`CredentialService`](src/main/java/com/group3/accounttrade/service/CredentialService.java) and [`EscrowService`](src/main/java/com/group3/accounttrade/service/EscrowService.java) are tightly coupled in transaction workflow
- EscrowService calls CredentialService.markCredentialsAsConfirmed() on release
- Both services create AuditLog and Notification records for all state changes
