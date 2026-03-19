# Ask Mode Rules

## Project Overview

TrustBridge Market - Spring Boot account trading marketplace with escrow system.

## Key Documentation

- [`CLAUDE.md`](CLAUDE.md) - Full project documentation
- [`DESIGN.md`](DESIGN.md) - Design system (colors, typography, components)
- [`SITE.md`](SITE.md) - Site roadmap and feature status

## Architecture Summary

- **Controllers**: Handle HTTP requests in `src/main/java/com/group3/accounttrade/controller/`
- **Services**: Business logic in `src/main/java/com/group3/accounttrade/service/`
- **Repositories**: Data access in `src/main/java/com/group3/accounttrade/repository/`
- **Templates**: Thymeleaf HTML in `src/main/resources/templates/`

## Non-Obvious Patterns

- Status values are DB entities (not enums) - look up via repository
- CredentialService and EscrowService are tightly coupled
- Server runs on port8081
