# Tracko Constitution

## Core Principles

### I. Monorepo with Clear Boundaries
Each component (backend, frontend, CLI, SDK) is independently buildable and testable. Cross-component contracts are defined by the OpenAPI spec. The SDK is auto-generated from the backend's OpenAPI output — never hand-edit it.

### II. API-First Design
The Spring Boot backend is the single source of truth. Frontend and CLI are consumers. All schema changes go through Liquibase migrations. All API changes must update OpenAPI annotations. Breaking API changes require SDK regeneration.

### III. Test Coverage is Non-Negotiable
- Backend: Integration tests are the primary testing strategy — prefer full-stack integration tests (extending `BaseIntegrationTest`) over service-level unit tests. JaCoCo enforces 70% line / 50% branch coverage.
- CLI: pytest with meaningful coverage on command logic and core modules.
- Frontend: Unit tests + integration tests via Flutter test framework.
- All PRs must pass CI checks before merge.

### IV. Database Integrity via Migrations
All schema changes go through Liquibase changelogs — never `ddl-auto`. Migrations must work against H2 (dev/test with `MODE=PostgreSQL`) and PostgreSQL (prod). Test migrations against the test profile before committing.

### V. Security by Default
JWT authentication on all API endpoints. Constructor injection only (no field `@Autowired`). DTOs for all API boundaries — never expose entities directly. Input validation via Jakarta annotations on create endpoints. Secrets in environment variables, never in code.

### VI. Simplicity and Data Density
Start simple, YAGNI. Frontend follows compact, data-dense layouts — no decorative chrome. Backend follows a clean layered architecture (controller → service → repository). CLI uses Rich for output and Typer for commands — consistent patterns across all command groups.

## Technology Constraints

| Layer | Stack | Version |
|-------|-------|---------|
| Backend | Spring Boot, JPA, Liquibase, JJWT | Java 17 |
| Frontend | Flutter, Dio, GetIt | Dart SDK 3.0+ |
| CLI | Typer, Rich | Python 3.10+ |
| SDK | OpenAPI Generator | Auto-generated |
| Dev DB | H2 (file-based, PostgreSQL mode) | — |
| Prod DB | PostgreSQL | — |

No new frameworks or languages without explicit justification. Prefer existing dependencies over adding new ones.

## Development Workflow

1. Feature branches off `main` — no direct pushes to `main`.
2. Code must build and pass tests locally before PR.
3. Backend changes that alter the API must regenerate the SDK.
4. Transaction subsystem changes must respect the read/write split (`TransactionService` for reads, `TransactionWriteService` as the single write entry point).
5. Use `task` runner commands for standard operations (`task start`, `task test`, `task clean`).

## Governance

This constitution defines the non-negotiable architectural boundaries of Tracko. All implementation decisions must align with these principles. Deviations require documented justification and must be reflected as amendments here.

**Version**: 1.0.0 | **Ratified**: 2026-05-03 | **Last Amended**: 2026-05-03
