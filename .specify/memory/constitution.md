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

## Code Quality Standards

### Clean Code
- No dead code, commented-out blocks, or unused imports in committed code.
- Methods do one thing. Classes have a single responsibility.
- Meaningful names — no abbreviations unless universally understood (e.g., `id`, `dto`).
- Prefer explicit over clever. Readability beats brevity.

### Error Handling
- Backend: Custom exceptions with appropriate HTTP status mapping. Never swallow exceptions silently.
- Frontend: All API calls must handle error states and show user-facing feedback.
- CLI: Errors render as Rich panels with red borders. Non-zero exit codes on failure.

### Code Consistency
- Backend: Follow existing layered patterns (controller → service → repository). Use ModelMapper for entity↔DTO.
- Frontend: Match existing widget patterns, use shared components from `lib/component/`.
- CLI: One Typer sub-app per command group. Use `core/output.py` helpers for all formatted output.

## Testing Standards

### Backend (Integration-First)
- Integration tests are the default — test through the full HTTP stack via `MockMvc`.
- All integration tests extend `BaseIntegrationTest` (auto-rollback, helper methods).
- Unit tests only for isolated utility/helper logic with no Spring dependencies.
- Test both happy path and error cases (invalid input, unauthorized access, not found).
- Use `@Transactional` rollback — no manual cleanup between tests.

### CLI (Live Backend)
- Tests run against a live backend instance — no mocked API calls.
- Ensures real end-to-end validation of CLI commands through the actual API.

### Frontend
- Widget tests for custom components.
- Integration tests for critical user flows.

### All Components
- Tests must be deterministic — no flaky tests, no reliance on external services.
- New features require tests before merge. Bug fixes require a regression test.

## User Experience Consistency

### Frontend UI Rules
- Compact, data-dense layouts. Maximize information per screen.
- Flat rows with thin dividers for lists — no cards on repeating items.
- Consistent typography: 14–15px titles, 10–11.5px subtitles, 32×32 avatars.
- Use `AmountText` widget for all currency displays.
- Dark mode must work — use borders for separation, not shadows.
- Tap feedback via `Material` + `InkWell`, never bare `GestureDetector`.

### CLI UX Rules
- All commands support `--raw` for JSON output (scriptability).
- All commands support `--help` with clear descriptions.
- Interactive prompts for missing required input. Non-interactive mode with explicit flags.
- Consistent table formatting via Rich across all command groups.

### Error UX
- User-facing errors must be actionable — say what went wrong and what to do next.
- Never expose stack traces or internal IDs to end users.

## Performance Requirements

### Backend
- API responses under 200ms for single-entity operations under normal load.
- Paginate all list endpoints — no unbounded queries.
- Use database indexes for frequently filtered/sorted columns.
- Avoid N+1 queries — use JPA fetch joins or `@EntityGraph` where needed.

### Frontend
- Screens must render within 1 frame after data arrives — no unnecessary rebuilds.
- Lazy-load lists. Paginate API calls for large datasets.
- Minimize widget tree depth in repeating list items.

### Database
- All foreign keys indexed. Composite indexes for common query patterns.
- Liquibase migrations must not lock tables for extended periods in production.

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

**Version**: 1.1.0 | **Ratified**: 2026-05-03 | **Last Amended**: 2026-05-03
