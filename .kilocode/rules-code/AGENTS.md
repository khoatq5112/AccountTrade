# Code Mode Rules

## Status Entity Pattern (CRITICAL)

All status values are database entities, NOT enums. Always look up via repository:

```java
OrderStatus status = orderStatusRepository.findByStatusName("PROCESSING")
    .orElseThrow(() -> new IllegalStateException("Status not found"));
```

## Dependency Injection

Use `@RequiredArgsConstructor` with `final` fields for constructor injection via Lombok.

## Transaction Workflow

- CredentialService and EscrowService are tightly coupled
- EscrowService calls CredentialService.markCredentialsAsConfirmed() on release
- Both services must create AuditLog and Notification records for state changes

## Port Configuration

Server runs on port8081, not default 8080.
