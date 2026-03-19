# Debug Mode Rules

## Key Log Locations

- Application logs: Standard Spring Boot logging (console)
- Audit logs: Stored in `audit_log` table via [`AuditLogRepository`](src/main/java/com/group3/accounttrade/repository/AuditLogRepository.java)
- Credential access logs: `credential_access_log` table

## Common Failure Points

- **Status lookup failures**: Status entities must exist in DB (seeded by DataInitializer)
- **Lazy loading issues**: Open Session in View is enabled but can still fail in async contexts
- **Escrow state transitions**: Can only release from HOLDING or FROZEN status

## Database Connection

- MySQL at localhost:3306/account_trading_system
- Credentials: root/root (see application.properties)

## Auto-Release Debugging

Escrow auto-release runs hourly via `@Scheduled` in [`EscrowService.java`](src/main/java/com/group3/accounttrade/service/EscrowService.java:358). Check server logs for "Auto-release escrow check" messages.
