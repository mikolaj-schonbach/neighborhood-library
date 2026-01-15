# AI Rules for Neighborhood-library

**Neighborhood Library** is a Minimum Viable Product (MVP) designed to digitize the operations of a local community library. The primary goal of this application is to move away from physical card catalogs and manual shelf-searching, offering a streamlined online experience for both readers and librarians.

Developed as part of the **10xDevs Course**, this system addresses the frustration of physical availability checks by providing a digital catalog. It allows users to browse the collection and reserve titles online, while enabling librarians (Administrators) to manage users, inventory, and loan cycles efficiently.


## CODING_PRACTICES

### Guidelines for SUPPORT_LEVEL

#### SUPPORT_BEGINNER

- When running in agent mode, execute up to 3 actions at a time and ask for approval or course correction afterwards.
- Write code with clear variable names and include explanatory comments for non-obvious logic. Avoid shorthand syntax and complex patterns.
- Provide full implementations rather than partial snippets. Include import statements, required dependencies, and initialization code.
- Add defensive coding patterns and clear error handling. Include validation for user inputs and explicit type checking.
- Suggest simpler solutions first, then offer more optimized versions with explanations of the trade-offs.
- Briefly explain why certain approaches are used and link to relevant documentation or learning resources.
- When suggesting fixes for errors, explain the root cause and how the solution addresses it to build understanding. Ask for confirmation before proceeding.
- Offer introducing basic test cases that demonstrate how the code works and common edge cases to consider.


### Guidelines for VERSION_CONTROL

#### GIT

- Use conventional commits to create meaningful commit messages
- Write meaningful commit messages that explain why changes were made, not just what
- Keep commits focused on single logical changes to facilitate code review and bisection
- Use interactive rebase to clean up history before merging feature branches
- Leverage git hooks to enforce code quality checks before commits and pushes

#### GITHUB

- Use pull request templates to standardize information provided for code reviews
- Configure required status checks to prevent merging code that fails tests or linting
- Use GitHub Actions for CI/CD workflows to automate testing and deployment
- Implement CODEOWNERS files to automatically assign reviewers based on code paths
- Use GitHub Projects for tracking work items and connecting them to code changes

## BACKEND

### Guidelines for JAVA & SPRING BOOT PROJECT

#### JAVA_CORE (Java 17)
- Use Java 17 features: `var` for local variables where type is obvious, text blocks for multiline strings (SQL, HTML snippets), and switch expressions.
- Use `record` for DTOs and immutable data carriers.
- Prefer `Optional` API over null checks.
- Use Stream API for collection processing but avoid over-complexity (maintain readability).

#### SPRING_BOOT_GENERAL
- Use Spring Boot 3.x standards (Jakarta EE 10 packages `jakarta.*` instead of `javax.*`).
- Prefer constructor-based dependency injection.
- Centralize exception handling with `@ControllerAdvice` and `@ExceptionHandler`.
- Use `application.properties` or `application.yml` for configuration; avoid hardcoded values.
- Use SLF4J for logging.

#### SPRING_DATA_JPA & POSTGRESQL
- Define repositories extending `JpaRepository`.
- **Database:** Use PostgreSQL dialect conventions.
- Use `GenerationType.IDENTITY` or `SEQUENCE` for primary keys in Postgres.
- Map entities strictly to the database schema; never return Entities directly to the view layer—map them to DTOs first.
- Use `@Transactional(readOnly = true)` for read operations to optimize Postgres performance.
- Avoid N+1 problems by using `@EntityGraph` or `JOIN FETCH` in JPQL.
- Use Jakarta Bean Validation (`@NotNull`, `@Size`) on DTOs and Entities to ensure data integrity.

#### SPRING_SECURITY
- Use the modern Lambda DSL for `SecurityFilterChain` configuration (avoid deprecated `WebSecurityConfigurerAdapter`).
- Use `requestMatchers` with strict role/authority checks.
- Integrate Thymeleaf security extras (`sec:authorize`) for conditional UI rendering based on roles.
- Ensure CSRF protection is enabled for non-GET requests (HTMX handles this automatically if configured, ensure the meta tag is present).

#### THYMELEAF_VIEW_LAYER
- Controllers should return logical view names (Strings), not JSON (unless specifically an API endpoint).
- Use Thymeleaf fragments (`th:fragment`, `th:replace`) to modularize the UI (e.g., headers, navbars, modals).
- Use `th:object` for binding form data to DTOs (`th:field`).
- Use Bootstrap 5 classes for layout and styling; utilize utility classes for spacing and typography instead of custom CSS where possible.
- Ensure proper use of the Model to pass data from Controller to View.

#### HTMX_INTERACTIONS
- **Endpoint Design:** For HTMX requests, Controllers should often return **HTML Fragments** (partials), not full page layouts or JSON.
- **Controller Logic:**
    - If a request comes from HTMX (`hx-request` header), return the specific fragment (e.g., `return "fragments/list :: item-row";`).
    - If it's a standard browser request, return the full page wrapper.
- Use `hx-target`, `hx-swap`, and `hx-trigger` effectively in the HTML to minimize JavaScript code.
- Handle validation errors by re-rendering the form fragment with error messages (`th:errors`), not by throwing 500 errors.

#### TESTING (JUnit 5 & Mockito)
- Use **JUnit 5** (`@Test`, `@BeforeEach`, `@DisplayName`) for all tests.
- Use **Mockito** for isolating units; prefer `@ExtendWith(MockitoExtension.class)` for unit tests.
- **Integration Tests:** Use `@SpringBootTest` with `@AutoConfigureMockMvc` for testing the full context and HTTP layer.
- **Slice Tests:** Use `@WebMvcTest` for testing Controllers/Thymeleaf rendering in isolation.
- **Repository Tests:** Use `@DataJpaTest` for testing database queries and constraints.
- Assertions: Use `Assertions.assertEquals`, `Assertions.assertTrue`, etc., or AssertJ if available on classpath.

## DATABASE

### Guidelines for SQL

#### POSTGRES

- Use connection pooling to manage database connections efficiently
- Use materialized views for complex, frequently accessed read-only data

## DEVOPS

### Guidelines for CI_CD (JAVA & MAVEN)

#### GITHUB_ACTIONS

- Check if `pom.xml` exists in project root to confirm Maven project structure.
- Always use terminal command: `git branch -a | cat` to verify whether we use `main` or `master` branch.
- Use `actions/setup-java` to set up **Java 17** environment (matches project tech stack).
- Configure caching for Maven dependencies (`cache: maven`) to speed up builds.
- Always use `mvn -B clean verify` (Batch mode) to build and run tests (JUnit 5).
- Use `mvn spring-boot:build-image` if building a Docker container, or `mvn package` for a JAR file.
- Store sensitive configuration (DB passwords, API keys) in GitHub Secrets and pass them as environment variables (`env:`) to the workflow steps.
- If using a database in integration tests, configure a PostgreSQL service container inside the workflow to match the production DB.
- Extract repeated logic (e.g., setup steps) into Composite Actions if the pipeline grows complex.
