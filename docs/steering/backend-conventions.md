---
inclusion: fileMatch
fileMatchPattern: "backend/**/*.java"
---

# Backend Conventions — Java / Spring Boot

## Package Structure
All code lives under `com.trako`:
- `controllers/` — REST endpoints (annotated with `@RestController`)
- `services/` — Business logic (`@Service`)
- `repositories/` — Spring Data JPA interfaces (`@Repository`)
- `entities/` — JPA entity classes (`@Entity`)
- `dtos/` — Request/response data transfer objects
- `models/` — Domain models (non-entity POJOs)
- `enums/` — Enum types
- `exceptions/` — Custom exception classes
- `configs/` and `config/` — Spring configuration classes
- `filters/` — Servlet filters (e.g., JWT auth filter)
- `helpers/` — Utility/helper classes
- `constants/` — Constant values
- `util/` — General utilities

## Coding Standards
- Java 17 features are available (records, sealed classes, pattern matching, text blocks)
- Use constructor injection (not field injection with `@Autowired`)
- DTOs for API request/response; never expose entities directly
- Use `ModelMapper` (already a dependency) for entity-to-DTO mapping
- Validate request bodies with `@Valid` and Jakarta Validation annotations
- Custom exceptions should extend appropriate Spring exception types
- Use Liquibase for all schema changes — never use `ddl-auto=create` or `update`

## Testing
- Test profile: `application-test.properties` (H2 in-memory, PostgreSQL mode)
- Use `@SpringBootTest` for integration tests
- Use `@DataJpaTest` for repository-only tests
- Use `@WebMvcTest` for controller-only tests with `@MockBean` for services
- JaCoCo enforces 70% line coverage, 50% branch coverage
- Test classes live in `src/test/java/com/trako/` mirroring the main package structure

## API Documentation
- SpringDoc OpenAPI is configured — keep `@Operation`, `@Tag`, and `@Schema` annotations up to date
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## Security
- JWT authentication via custom filter
- Token secret and validity configured in `application.properties`
- Use `@PreAuthorize` or security config for endpoint-level authorization

## Database & Migrations
- All schema changes go through Liquibase changelogs in `src/main/resources/db/changelog/`
- Master changelog: `db.changelog-master.yaml`
- Dev uses H2 file DB; test uses H2 in-memory; prod uses PostgreSQL
- Always test migrations against the test profile before committing
