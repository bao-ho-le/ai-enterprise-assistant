# Backend testing guide

This document describes the backend test suite: its architecture, how to run it, the coverage
rules it enforces, and the known limitations.

## 1. Scope

Only the following packages count towards JaCoCo coverage:

| # | Package |
|---|---------|
| 1 | `com.enterprise.aiassistant.backend.document` |
| 2 | `com.enterprise.aiassistant.backend.storage` |
| 3 | `com.enterprise.aiassistant.backend.processing` |
| 4 | `com.enterprise.aiassistant.backend.ai.knowledge.search` |
| 5 | `com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore` |
| 6 | `com.enterprise.aiassistant.backend.ai.infrastructure.embedding` |
| 7 | `com.enterprise.aiassistant.backend.admin.document` |
| 8 | `com.enterprise.aiassistant.backend.admin.trash` |

Everything else (`auth`, `user`, `department`, `folder`, `ai.chat`, `ai.analytics`,
`ai.knowledge.generation`, `ai.knowledge.qa`, `ai.infrastructure.llm`,
`ai.infrastructure.prompt`, `admin.user`, `admin.role`, `admin.department`, `admin.shared`,
`admin.usage`, `common`) is excluded from the denominator but is still used as a collaborator
(usually mocked) inside the tests.

Document *sharing* (share / revoke / list shares / admin shared documents) is explicitly out of
scope: no business tests exist for it. Classes that also contain sharing methods (for example
`DocumentServiceImpl`) stay in scope and are tested for all their other operations.

## 2. Architecture

The suite is hybrid:

```
src/test/java/.../backend/
├── support/                 shared test infrastructure (containers, fixtures, fakes)
├── **/*Test.java            unit tests        -> Surefire -> target/jacoco.exec
└── **/*IT.java              integration tests -> Failsafe -> target/jacoco-it.exec
```

### 2.1 Unit tests (`*Test`)

* JUnit 5 + Mockito + AssertJ.
* No Spring context, no Docker, no network.
* Repositories and external systems are mocked; real helpers/mappers are used wherever they are
  pure (`DocumentHelper`, `DocumentMapper`, `ChunkingMapper`, `SlidingWindowSplitter`,
  `VectorStoreHelper`, `SearchMapper`, `ExtractionHelper`, ...).
* Every exception assertion pins the exact `ErrorCode` through
  `support/ErrorCodeAssertions`.
* `mvn test` runs only these, so it works on a machine without Docker.

### 2.2 Integration tests (`*IT`)

* Real PostgreSQL, real MinIO and real Qdrant through Testcontainers.
* `DocumentRepositoryCustomImplIT`, `DocumentRepositoryIT`, `DocumentVersionRepositoryIT`,
  `DocumentAccessRepositoryIT`, `DocumentChunkRepositoryIT`, `DocumentTextRepositoryIT` use
  `AbstractPostgresIT` (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`), so
  PostgreSQL semantics (CAST, `LOWER`, unique constraints, `ORDER BY`) are real.
* `MinioFileStorageServiceIT` and `QdrantVectorStoreServiceIT` drive the real clients against the
  containers without a Spring context.
* `DocumentWorkflowIT` and `BackendApplicationIT` use `AbstractFullStackIT`: the full Spring
  context plus all three containers and the real asynchronous processing pipeline.
* `mvn verify` runs them by default; `-DskipITs` skips them explicitly.

### 2.3 Container support

Singleton containers live in `support/`:

| Class | Image | Notes |
|-------|-------|-------|
| `PostgresContainerSupport` | `postgres:16-alpine` | isolated DB `ai_assistant_test`, user `test_user` |
| `MinioContainerSupport` | `quay.io/minio/minio@sha256:14cea493...` (pinned digest) | creates bucket `test-files` itself |
| `QdrantContainerSupport` | `qdrant/qdrant:v1.15.1` | collection `test_document_chunks`, dimension 8 |

Containers are started once per JVM and shared by every test class. They publish random host
ports; nothing connects to the development stack.

### 2.4 No Gemini calls

`support/DeterministicEmbeddingService` replaces the Gemini embedding client in integration tests
(`DeterministicEmbeddingTestConfig`, `@Primary`). It returns stable vectors derived from
`Math.floorMod`, a stable model name (`deterministic-test-embedding`) and a stable token count.
Unit tests mock LangChain4j's `EmbeddingModel`.

The full-stack tests still supply dummy properties (`gemini.api-key=test-key-not-used`,
`gemini.llm-model.*`) so that configuration binding succeeds; no request is ever made.

### 2.5 Schema creation

The repository's Flyway migrations do **not** create the document/file/folder tables. For tests
only:

```properties
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

Consequence: the test suite verifies JPA mapping and SQL behaviour, **not** production Flyway
migration completeness. See `TEST_COVERAGE_REPORT.md` for the recorded production issue.

## 3. JaCoCo configuration

`backend/pom.xml` configures:

* `prepare-agent` → Surefire agent, `destFile=target/jacoco.exec`, property `jacoco.unit.argLine`.
* `prepare-agent-integration` → Failsafe agent, `destFile=target/jacoco-it.exec`, property
  `jacoco.it.argLine`.
* `merge-exec` (bound to `verify`) → merges **only** `target/jacoco.exec` and
  `target/jacoco-it.exec` into `target/jacoco-merged.exec`.
* `report` → HTML/XML/CSV with the eight package includes.
* `check-scoped-coverage` → fails the build below **70% line** or **50% branch** for those eight
  packages.

Surefire uses `@{jacoco.unit.argLine}` and Failsafe uses `@{jacoco.it.argLine}`; the two agents
never overwrite each other.

### Lombok

`backend/lombok.config`:

```properties
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```

Generated getters/setters/builders/constructors carry `@lombok.Generated`, which JaCoCo filters.
No DTO or entity package is excluded, and there are no getter/setter-only tests.

## 4. Running the tests

The Maven wrapper in this repository is incomplete (`Cannot find path
'.mvn/wrapper/maven-wrapper.properties'`), so use an installed Maven 3.9.x:

```bash
cd backend

# Unit tests only - no Docker required
mvn test

# Unit + integration tests + merged coverage report (default verification)
mvn clean verify

# Unit tests, skipping integration tests explicitly
mvn clean verify -DskipITs

# A single unit test class
mvn test -Dtest=DocumentHelperTest

# A single integration test class
mvn test-compile failsafe:integration-test failsafe:verify -Dit.test=DocumentWorkflowIT
```

Requirements for the integration tests:

* Docker running (Docker Desktop or an equivalent daemon reachable by Testcontainers).
* The three images above pullable (or already cached).
* Roughly 2-3 GB of free container memory.

## 5. Reports

After `mvn clean verify`:

| File | Content |
|------|---------|
| `target/site/jacoco/index.html` | HTML report (open this first) |
| `target/site/jacoco/jacoco.xml` | machine-readable report |
| `target/site/jacoco/jacoco.csv` | per-class coverage table |
| `target/jacoco.exec` | unit-test execution data |
| `target/jacoco-it.exec` | integration-test execution data |
| `target/jacoco-merged.exec` | merged data used by report/check |

## 6. Test data isolation

* Each container is fresh per JVM; nothing is shared with the development stack.
* `AbstractPostgresIT` relies on Spring's per-test rollback, so JPA slice tests never leak rows.
* `AbstractFullStackIT.resetDocumentState()` runs before every test and deletes document-related
  rows (`document_accesses`, `document_chunks`, `document_texts`, `document_versions`,
  `documents`, `files`) and recreates the Qdrant collection. Seeded users/departments and the
  root folder survive, so initializers are not disturbed.
* `DocumentWorkflowIT` creates a fresh user + department per test (unique UUID suffix) and
  installs a `UserPrincipal` in the `SecurityContext`; the context is cleared afterwards.
* MinIO tests use unique object keys (UUID prefix) and a dedicated bucket.

## 7. Adding new tests

1. Decide the type: pure logic → `*Test` in the same package as the class; real SQL/broker
   behaviour → `*IT`.
2. Unit test: mock the collaborators with `mock(...)`, assert outputs and interactions, and use
   `ErrorCodeAssertions` for exceptions.
3. Repository test: extend `AbstractPostgresIT`, autowire the real repository, and use
   `TestFixtures` to build entities. Flush + clear the `EntityManager` before asserting derived
   queries so you read from the database, not the first-level cache.
4. Full-stack test: extend `AbstractFullStackIT` only when the scenario genuinely crosses layers
   (upload → processing → vector store, for instance).
5. Never call Gemini, never point at development infrastructure, never use `Thread.sleep` for
   asynchronous work - use Awaitility with a bounded timeout.
6. Keep every test assertion meaningful. Do not add trivial getter/setter tests to raise the
   coverage number.

## 8. Known limitations

* Flyway migrations are not exercised (test-only `ddl-auto=create-drop`).
* Document sharing/revocation is not tested (excluded by requirement), so a small part of
  `DocumentServiceImpl`, `DocumentAuthorizationService` and `DocumentHelper` is intentionally
  uncovered.
* The MinIO bucket is created by the test support, because production code never creates it.
* `QdrantProperties.apiKey` / `restPort` are unused by production code and therefore only covered
  through configuration binding, if at all.
* Integration tests are slow (container startup + asynchronous processing) and need Docker; the
  unit suite stays fast and Docker-free.
