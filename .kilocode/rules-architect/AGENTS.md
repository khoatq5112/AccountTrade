# Architect Mode Rules

## Core Architecture

Spring Boot marketplace with escrow-based transactions between buyers and sellers.

## Key Service Coupling

- **CredentialService ↔ EscrowService**: Tightly coupled in transaction workflow
  - EscrowService calls CredentialService.markCredentialsAsConfirmed() on release
  - Both must create AuditLog and Notification records for state changes

## Entity Lifecycle Patterns

**Credential Lifecycle**:
AVAILABLE → HOLDING (reserved) → ASSIGNED (paid) → SOLD (confirmed) / DISPUTED / REPLACED

**Escrow Lifecycle**:
HOLDING → FROZEN (dispute) → RELEASED (seller paid) / REFUNDED (buyer refunded)

## Status Entity Pattern (CRITICAL)

All status values are database entities, NOT enums. Requires repository lookup:

```java
OrderStatus status = orderStatusRepository.findByStatusName("PROCESSING")
    .orElseThrow(() -> new IllegalStateException("Status not found"));
```

## Scheduled Tasks

- Escrow auto-release: Every hour via `@Scheduled` in EscrowService
- Verification timeout: 24 hours (configurable via `escrow.verification-timeout-hours`)

## Configuration

- Server port: 8081
- Platform fee: Configurable via `escrow.platform-fee-percent` (default5%)
- Open Session in View: Enabled for lazy loading in templates
