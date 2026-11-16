# Backend Testing Audit & Implementation Plan — JaCoCo ≥ 70% (scoped)

**Repository:** `D:\Projects\ai-enterprise-assistant` (Maven module: `backend/`)
**Base package:** `com.enterprise.aiassistant.backend`
**Stack (verified):** Java 17, Spring Boot **4.1.0** (parent), Maven wrapper (`backend/mvnw`), Spring Data JPA + Hibernate, Flyway, Spring Security, MinIO (`io.minio:minio 8.5.17`), Qdrant (`io.qdrant:client 1.15.0` + `langchain4j-qdrant`), LangChain4j 1.4.0 (Gemini), Tika 3.3.1, POI 5.5.1, QueryDSL 5.1.0 (declared), Lombok.
**Date of audit:** 2026-11-16.

> **Status of this document:** analysis only. No test was written, no production file, `pom.xml`, Docker Compose or configuration file was modified, no container was started, no database/bucket/collection was touched, no external AI API was called.

---

## 0. Evidence legend

Every statement below is tagged:

- **[V]** = **Verified** by reading the actual file (path given).
- **[R]** = **Recommendation** (my proposal, not present in the repo).
- **[A]** = **Assumption** (plausible, not verified).
- **[?]** = **Unknown / unverifiable in this audit** (needs a command or human confirmation).

---

## 1. Executive Summary

### 1.1 What exists today

| Fact | Status |
|---|---|
| The repository **already contains 4 test classes**, contradicting the brief's "no automated tests have been written yet" | **[V]** |
| JaCoCo is wired for `prepare-agent` + `report` only. **No `check` goal, no includes/excludes, no threshold** | **[V]** |
| **No Surefire or Failsafe configuration** exists in `pom.xml` (Boot parent defaults only) | **[V]** |
| **No Testcontainers, no test resources, no `application-test.yml`, no `lombok.config`** | **[V]** |
| Flyway migrations **do not create** the core tables `documents`, `document_versions`, `files`, `folders`, `document_chunks`, `document_texts` (V2 explicitly assumes Hibernate `ddl-auto=update` created them) | **[V]** |
| `FileStorageService` has **no delete operation** at all — there is nothing to test for "file deletion" | **[V]** |
| There is **no admin endpoint / service for permanent document deletion** — only folder hard delete (out of scope) | **[V]** |
| Any `@SpringBootTest` boots `QdrantCollectionManager` (`ApplicationReadyEvent`), `RbacInitializer`, `RootFolderInitializer` — three startup side effects that hit Qdrant, `users`, `role_permissions`, `ai_conversations`, and `folders` | **[V]** |
| MinIO bucket is never created by application code — `store()` just puts into `${minio.bucket}` | **[V]** |

Existing tests **[V]**:

- `backend/src/test/java/com/enterprise/aiassistant/backend/BackendApplicationTests.java` (`@SpringBootTest`, empty `contextLoads`)
- `backend/src/test/java/com/enterprise/aiassistant/backend/document/repository/DocumentRepositoryCustomImplTest.java` (covers `appendAccessScope` only)
- `backend/src/test/java/com/enterprise/aiassistant/backend/document/service/DocumentAuthorizationServiceTest.java` (covers `currentAccessScope` only)
- `backend/src/test/java/com/enterprise/aiassistant/backend/authorization/AuthorizationPolicyTest.java` (RBAC/ABAC matrix, partially out of scope)

### 1.2 The decisive coverage insight

**[V]** `pom.xml` declares `org.projectlombok:lombok` and there is **no `lombok.config`** in the repository. **[V]** The eight in-scope packages contain roughly 40 Lombok DTO/entity classes (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`). With the current configuration, **all Lombok-generated accessors, builders and `equals/hashCode` are counted as executable lines** by JaCoCo. They are untestable by design and will dominate the denominator of the eight packages.

**Consequence:** a 70% line gate on the eight packages is not meaningfully reachable (and would only be reachable by writing thousands of pointless getter assertions) until one of these is done:

1. **[R] Preferred:** add `backend/lombok.config` with `lombok.addLombokGeneratedAnnotation = true`. JaCoCo filters any method annotated `@Generated`, and Lombok then emits `@lombok.Generated` on everything it generates. This is an officially documented exclusion of *generated* code (the same justification JaCoCo applies to Kotlin/records), not an exclusion of business logic.
2. **[R] Fallback:** add `<exclude>` entries for `**/dto/**` and `**/entity/**` in the JaCoCo `report`/`check` configuration, and record the justification in `pom.xml` comments.

Without one of these two, every coverage number produced below will be systematically depressed by boilerplate.

### 1.3 Recommended shape of the effort

- **Unit layer (Mockito only, no Spring):** 14 classes carry almost all business logic and are 100% mockable: `DocumentHelper`, `DocumentAuthorizationHelper`, `DocumentMapper`, `DocumentAuthorizationService`, `DocumentServiceImpl`, `ChunkingServiceImpl` + both strategies + `SlidingWindowSplitter`, `TikaStructuredContentHandler` + `ExtractionHelper`, `DocumentProcessingService` + `ProcessingHelper`, `VectorStoreHelper` + `VectorStoreMapper` + `QdrantVectorStoreService`, `GeminiEmbeddingService`, `SemanticSearchServiceImpl` + `SearchHelper`, `AdminTrashService`, `MinioFileStorageService`, `SearchMapper`, `FileMapper`, `ChunkingStrategyFactory`, `TextExtractorFactory`, `ExcelTextExtractor`.
- **PostgreSQL integration (Testcontainers):** only where JPQL/PostgreSQL semantics matter — `DocumentRepositoryCustomImpl.filterDocuments` (JPQL string building + `CAST(:x AS string) IS NULL` idiom), `DocumentRepository.findDeletedDocuments`, `DocumentVersionRepository` top-version lookup + unique constraint, `DocumentAccessRepository` derived queries + bulk delete, chunk/text repositories.
- **MinIO integration:** one small suite (`store` → `loadAsResource` → content equality, missing key, bad credentials).
- **Qdrant integration:** one small suite (collection create, upsert, cosine search ordering, `documentId` filter, delete, empty result).
- **Gemini:** never called. `EmbeddingService` is either mocked (unit) or replaced by a deterministic test double (integration).
- **Do not** write a full-context `@SpringBootTest` for the controller layer; three startup initializers make it expensive and Qdrant-dependent.

**Projected outcome (estimate, not measured) [A]:** with Lombok filtering on, Phase 1 alone ≈ 55–65% of scoped lines; Phases 2–3 add the JPQL/Storage/Vector paths (≈ +8–12%); Phase 4 covers the orchestration tail. 72–78% scoped line coverage is a realistic landing zone, with branch coverage ≈ 55–65%.

**Effort estimate:** 9–12 developer-days (Phase 0: 0.5d, Phase 1: 4–5d, Phase 2: 1.5–2d, Phase 3: 1–1.5d, Phase 4: 1.5–2d, Phase 5: 0.5–1d).

---

## 2. Confirmed Testing Scope

### 2.1 In scope (exactly eight packages, verified directories)

| # | Package (directory) | Java files **[V]** |
|---|---|---|
| 1 | `backend/src/main/java/com/enterprise/aiassistant/backend/document/` | 24 |
| 2 | `backend/src/main/java/com/enterprise/aiassistant/backend/storage/` | 10 |
| 3 | `backend/src/main/java/com/enterprise/aiassistant/backend/processing/` (`chunking`, `config`, `extraction`, `orchestration`) | 33 |
| 4 | `backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/search/` | 7 |
| 5 | `backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/` | 10 |
| 6 | `backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/embedding/` | 5 |
| 7 | `backend/src/main/java/com/enterprise/aiassistant/backend/admin/document/` | 2 |
| 8 | `backend/src/main/java/com/enterprise/aiassistant/backend/admin/trash/` | 3 |
| | **Total** | **94** |

### 2.2 Explicitly excluded from the denominator and from test authoring

- `auth/**`, `user/**`, `department/**`, `folder/**` (except as a mocked collaborator), `ai/chat/**`, `ai/analytics/**`, `ai/knowledge/generation/**`, `ai/knowledge/qa/**`, `ai/infrastructure/llm/**`, `ai/infrastructure/prompt/**`, `admin/user`, `admin/role`, `admin/department`, `admin/shared`, `admin/usage`, `common/**`, `BackendApplication`.

### 2.3 In-scope but intentionally *not* tested (constraint: "excluding sharing")

| Code | Why untested | Coverage consequence |
|---|---|---|
| `document/service/DocumentServiceImpl#shareDocument`, `#revokeDocumentAccess`, `#getDocumentShares` **[V]** | Document sharing is excluded from the assignment | ~55 uncovered lines inside `DocumentServiceImpl` |
| `document/mapper/DocumentMapper#toDocumentAccess`, `#toShareResponse` **[V]** | Same | ~20 uncovered lines |
| `document/entity/DocumentAccess.java` **[V]** | Same (also Lombok-excluded) | ~0 after Lombok filter |
| `document/repository/DocumentAccessRepository#searchSharedDocuments` **[V]** | Admin *shared-documents* listing = sharing feature | ~0 (interface) |
| `DocumentAuthorizationService#requireManageAccess` **[V]** | Only used by sharing paths | ~4 uncovered lines |

**[R]** Keep the existing `DocumentAccessRepository#existsByDocumentIdAndUserId` + `#findDocumentIdsSharedWithUser` in scope — they are the *consumer* side of sharing used by document read authorization (`DocumentAuthorizationService#hasExplicitAccess`, `#currentAccessScope`), which is core document behavior, not the sharing feature.

### 2.4 Boundary classes: mock or narrow integration

| Out-of-scope dependency | Used by (in-scope) | Decision |
|---|---|---|
| `auth/service/CurrentUserService` **[V]** | `DocumentServiceImpl`, `DocumentAuthorizationService`, `SemanticSearchServiceImpl`, `AdminTrashService` | **Mock** it (`getCurrentPrincipal`, `getCurrentUser`, `requirePermission`). It reads `SecurityContextHolder` → unsuitable for unit tests. **[R]** |
| `folder/service/FolderService` **[V]** | `DocumentServiceImpl#upload`, `#moveDocument` | **Mock** `resolveTargetFolder`. Folder module is out of scope. **[R]** |
| `folder/repository/FolderRepository` **[V]** | `AdminTrashService#deletedFolders` | **Mock** in unit tests; in the trash *persistence* IT seed folders through the repository (narrow, justified). **[R]** |
| `ai/analytics/usage/service/AIUsageLogService` **[V]** | `SearchHelper#logUsage`, `ProcessingHelper#logUsage` | **Mock** + `ArgumentCaptor` on `AIUsageLogRequest`. **[R]** |
| `user/repository/UserRepository` **[V]** | `DocumentServiceImpl#shareDocument`, `CurrentUserService` | Mock (sharing excluded). **[R]** |
| `document/enums/DocumentStatus` etc. **[V]** | cross-cutting | Real enums (they are in `document/`, in scope). |

---

## 3. Repository Map of the Eight Assigned Packages

### 3.1 `document` (24 files) **[V]**

| Layer | Files |
|---|---|
| Controller | `controller/DocumentController.java` — `POST /documents/upload`, `POST /{id}/versions`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/restore`, `PUT /{id}/move`, `GET /{id}/{versionId}/download`, `GET` (list), `GET /{id}`, `GET /check-title`, plus 3 share endpoints (excluded) |
| Service | `service/DocumentService.java` (interface, 13 methods), `service/DocumentServiceImpl.java` (14 injected collaborators), `service/DocumentAuthorizationService.java` |
| Helper | `helper/DocumentHelper.java` (validation, ~20 public/private validators, `createNewVersion`, static `resolveMediaType`), `helper/DocumentAuthorizationHelper.java` (pure RBAC/ABAC) |
| Mapper | `mapper/DocumentMapper.java` (13 mapping methods + `toDownloadResponse`) |
| Repository | `repository/DocumentRepository.java` (derived + 1 `@Query`), `DocumentRepositoryCustom.java`, `DocumentRepositoryCustomImpl.java` (JPQL builder, `EntityManager`), `DocumentVersionRepository.java`, `DocumentAccessRepository.java`, `DocumentChunkRepository.java`, `DocumentTextRepository.java` |
| Entity | `entity/Document.java`, `DocumentVersion.java`, `DocumentChunk.java`, `DocumentText.java`, `DocumentAccess.java` |
| Enum | `enums/DocumentStatus.java` (ACTIVE, DELETED), `DocumentType.java` (6), `ExtractionMethod.java` (4), `ProcessingStep.java` (3), `VersionStatus.java` (PENDING, PROCESSING, READY, FAILED) |
| DTO | `dto/DocumentAccessScope.java` (record), requests: `DocumentUploadRequest`, `DocumentBatchUploadRequest`, `DocumentUpdateMetadataRequest`, `UploadNewVersionRequest`, `MoveDocumentRequest`, `DocumentFilterRequest`, `ShareDocumentRequest`; responses: `DocumentUploadResponse`, `UploadNewVersionResponse`, `DocumentDownloadResource`, `DocumentDetailResponse` (4 nested records), `DocumentListResponse`, `DocumentMoveResponse`, `DocumentRestoreResponse`, `DocumentUpdateMetadataResponse`, `DocumentShareResponse` |

### 3.2 `storage` (10 files) **[V]**

| Layer | Files |
|---|---|
| Service | `service/FileStorageService.java` — **only `store` + `loadAsResource`**, `service/MinioFileStorageService.java` (`@Value("${minio.bucket}")`) |
| Config | `config/MinioConfig.java` (builds `MinioClient` from `minio.endpoint/access-key/secret-key`), `config/FileUploadProperties.java` (`file.upload.max-size`, **injected into `DocumentHelper` but never read** — `DocumentHelper` hardcodes 50 MB) |
| Controller | `controller/FileTestController.java` — `POST ${api.prefix}/test/storage/upload` *(looks like a leftover debug endpoint; flagged in §14)* |
| Mapper | `mapper/StoredFileMapper.java` (`MultipartFile` → `StoredFileDto`), `mapper/FileMapper.java` (`StoredFileDto` → `FileEntity`, extension parsing) |
| Repository / Entity / DTO | `repository/FileRepository.java` (empty `JpaRepository`), `entity/FileEntity.java` (`@PrePersist`), `dto/response/StoredFileDto.java` |

### 3.3 `processing/*` (33 files) **[V]**

| Sub-package | Files |
|---|---|
| `chunking/service` | `ChunkingService`, `ChunkingServiceImpl` (validates, delegates to factory-selected strategy, wraps strategy errors into `DOCUMENT_CHUNKING_FAILED`) |
| `chunking/strategy` | `ChunkingStrategy` (iface), `ChunkingStrategyFactory` (pdf/docx/txt/doc → document; xlsx/xls → spreadsheet; else `UNSUPPORTED_FILE_TYPE`), `DocumentChunkingStrategy` (page grouping, heading breadcrumb `TreeMap`, `CHUNK_SIZE` flush, long-paragraph split), `SpreadsheetChunkingStrategy` (sheet grouping, header re-injection, flush rules) |
| `chunking/support` | `SlidingWindowSplitter` (`CHUNK_SIZE = 1000`, `OVERLAP_SIZE = 200`, `MAX_HEADING_BREADCRUMB_DEPTH = 3`) |
| `chunking/mapper` `chunking/dto` | `ChunkingMapper` (token estimate `len/4`), `TextChunk` |
| `config` | `AsyncConfig` (core 3, max 5, queue 100, prefix `document-processing-`) |
| `extraction/service` | `TextExtractionService`, `TextExtractionServiceImpl` (factory lookup + delegate) |
| `extraction/extractor` | `TextExtractor` (iface), `TextExtractorFactory` (first `supports(mimeType)`), `PdfTextExtractor`, `DocxTextExtractor`, `TxtTextExtractor`, `ExcelTextExtractor` (POI, `TABLE_ROW` elements, `pageNumber` = sheet index) |
| `extraction/extractor/tika` | `TikaContentExtractor` (AutoDetectParser + handler), `TikaStructuredContentHandler` (`startElement`/`characters`/`endElement`, `div.page` counter, nested-tag guard) |
| `extraction/helper` `extraction/mapper` `extraction/dto` `extraction/enums` | `ExtractionHelper` (`validateExtractedText`, `resolveTagName`, `resolveHeadingLevel`, `toPages`), `ExtractionMapper`, `DocumentElement`, `ExtractedText`, `ElementType` |
| `orchestration/service` | `DocumentProcessingService#process(Long)` — extract → save text → chunk → save chunks → embed each chunk → `vectorStoreService.upsert`, sets `PROCESSING`/`TEXT_EXTRACTING`/`CHUNKING`/`EMBEDDING`/`READY` |
| `orchestration/helper` `orchestration/mapper` | `ProcessingHelper` (`validateStatus`, `@Transactional(REQUIRES_NEW) handleFailed`, `handleFailureUnlessAlreadySucceeded`, `logUsage`), `ProcessingMapper` |
| `orchestration/worker` | `DocumentProcessingWorker` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)`), `DocumentProcessingBatchWorker` (submits each id to `documentProcessingExecutor`), `DocumentProcessingRetryExecutor` (`@Retryable` 3 attempts, backoff 3000 ms, only `FileStorageException`/`ProcessingException`/`EmbeddingException`/`VectorStoreException`) |
| `orchestration/event` | `DocumentVersionCreatedEvent`, `DocumentVersionCreatedBatchEvent` (records, no lines) |

### 3.4 `ai/knowledge/search` (7 files) **[V]**

`controller/SemanticSearchController` (`GET ${api.prefix}/search/semantic`), `service/SemanticSearchService`, `service/SemanticSearchServiceImpl` (validate → `requirePermission(AI_SEMANTIC_SEARCH)` → embed → vector search → filter ACTIVE + readable + current-version → map → log usage), `helper/SearchHelper` (`DEFAULT_TOP_K=10`, `MAX_TOP_K=50`, `MAX_KEYWORD_LENGTH=255`, validation + usage logging as `ConversationType.SEMANTIC_SEARCH`), `mapper/SearchMapper`, `dto/request/SemanticSearchRequest`, `dto/response/SemanticSearchResult`.

### 3.5 `ai/infrastructure/vectorstore` (10 files) **[V]**

`service/VectorStoreService` (upsert 1/list, delete, 2-arg default `search`), `service/QdrantVectorStoreService` (maps points, `upsertAsync`/`deleteAsync`/`searchAsync`, sets `scoreThreshold` from properties, optional `documentId` filter), `manager/QdrantCollectionManager` (`ApplicationReadyEvent`, creates collection with `Cosine` if absent), `helper/VectorStoreHelper` (dimension/id/limit validation, `buildDocumentFilter`, payload put/get), `mapper/VectorStoreMapper` (payload ↔ DTO, `PointStruct`, `ScoredPoint` → `SearchResult`, `float[]` → `List<Float>`), `config/QdrantConfig` + `config/QdrantProperties` (**`apiKey` and `restPort` are declared but unused**), `dto/SearchResult`, `dto/VectorPayload`, `dto/VectorPoint`.

### 3.6 `ai/infrastructure/embedding` (5 files) **[V]**

`service/EmbeddingService` (`embed`, `getModelName`), `service/GeminiEmbeddingService` (validate → `embeddingModel.embed` → `toEmbeddingResult`; token estimate `max(1, len/4)` when `response.tokenUsage()` is null; wraps failures in `EmbeddingException(EMBEDDING_FAILED)`), `config/GeminiEmbeddingModelConfig` (`GoogleAiEmbeddingModel` bean — bean creation performs **no** network call), `config/GeminiProperties`, `dto/EmbeddingResult`.

### 3.7 `admin/document` (2 files) **[V]**

`controller/AdminDocumentController` (`@PreAuthorize("hasRole('ADMIN')")`; list, detail, shares, download, delete, restore; default page size 20), `service/AdminDocumentService` (pure delegation to `DocumentService` — note `getDocuments` → `getDocumentsForAdmin`).

### 3.8 `admin/trash` (3 files) **[V]**

`controller/AdminTrashController` (`@PreAuthorize("hasRole('ADMIN')")`; list, restore document, restore folder, permanently delete folder), `service/AdminTrashService` (`@Transactional(readOnly)`, `requirePermission(DOCUMENT_DELETE)`, merges deleted documents + deleted folders, sorts `deletedAt` **desc with nulls last**, paginates **in memory**), `dto/TrashItemResponse`.

---

## 4. JaCoCo Configuration Audit

### 4.1 Current state **[V]** (`backend/pom.xml`)

```xml
<properties>
  <java.version>17</java.version>
  <jacoco.version>0.8.16</jacoco.version>
</properties>
...
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>${jacoco.version}</version>
  <executions>
    <execution><goals><goal>prepare-agent</goal></goals></execution>
    <execution>
      <id>report</id><phase>verify</phase><goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

| Check | Result |
|---|---|
| Version | `0.8.16` — released 2026-10-24, officially supports up to Java 27, so **Java 17 is fully supported** (JaCoCo has supported 17 since 0.8.8) **[V]** (jacoco.org change history) |
| `prepare-agent` | Present, default config → writes `target/jacoco.exec`, sets `argLine` picked up by Surefire automatically **[V]** |
| Surefire integration | Implicit via the `argLine` property; **no explicit `maven-surefire-plugin` block** → Boot parent defaults (`*Test`, `*Tests`, `Test*`, `*TestCase`) **[V]** |
| Failsafe integration | **Absent** → any `*IT` class will never run under `mvn verify` **[V]** |
| Execution data collection | Only the Surefire execution data file. No IT data file, no merge **[V]** |
| Combined UAT+IT reporting | Not configured **[V]** |
| HTML report | Generated at `target/site/jacoco/index.html` on `verify` **[V]** (goal default) |
| Coverage exclusions | **None** — this is the problem in §1.2 **[V]** |
| Coverage threshold | **None** — nothing fails the build today **[V]** |
| Scoping (`includes`) | **None** — the report currently includes *every* class in the module **[V]** |

### 4.2 How to restrict the report/check to the eight packages

JaCoCo `report`/`check` `includes` patterns are **class-file paths** (relative to `target/classes`, `/`-separated, `*` = one segment, `**` = any depth) **[V]** (jacoco.org mojo docs). The exact eight patterns, derived from the verified directory layout:

```
com/enterprise/aiassistant/backend/document/**
com/enterprise/aiassistant/backend/storage/**
com/enterprise/aiassistant/backend/processing/**
com/enterprise/aiassistant/backend/ai/knowledge/search/**
com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/**
com/enterprise/aiassistant/backend/ai/infrastructure/embedding/**
com/enterprise/aiassistant/backend/admin/document/**
com/enterprise/aiassistant/backend/admin/trash/**
```

Note the deliberate non-overlap with `ai/infrastructure/llm/**` and `ai/knowledge/{qa,generation}/**`, which share the same parents as two in-scope packages — this is why a naive `ai/**` include would break the scope rule.

### 4.3 Recommended `pom.xml` changes **[R]** (do not apply during this audit)

**(a) New file `backend/lombok.config`** — justified exclusion of *generated* code:

```properties
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```

**(b) Replace the JaCoCo plugin block** — scoped includes, merged UT+IT data, and the 70% gate:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>${jacoco.version}</version>
  <executions>
    <!-- 1. unit-test agent -->
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>

    <!-- 2. integration-test agent (different data file) -->
    <execution>
      <id>prepare-agent-integration</id>
      <goals><goal>prepare-agent-integration</goal></goals>
      <configuration>
        <destFile>${project.build.directory}/jacoco-it.exec</destFile>
      </configuration>
    </execution>

    <!-- 3. merge the two data files (must be declared before report/check) -->
    <execution>
      <id>merge-results</id>
      <phase>verify</phase>
      <goals><goal>merge</goal></goals>
      <configuration>
        <fileSets>
          <fileSet>
            <directory>${project.build.directory}</directory>
            <includes><include>*.exec</include></includes>
          </fileSet>
        </fileSets>
        <destFile>${project.build.directory}/jacoco-merged.exec</destFile>
      </configuration>
    </execution>

    <!-- 4. scoped HTML/XML report -->
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
      <configuration>
        <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
        <includes>
          <include>com/enterprise/aiassistant/backend/document/**</include>
          <include>com/enterprise/aiassistant/backend/storage/**</include>
          <include>com/enterprise/aiassistant/backend/processing/**</include>
          <include>com/enterprise/aiassistant/backend/ai/knowledge/search/**</include>
          <include>com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/**</include>
          <include>com/enterprise/aiassistant/backend/ai/infrastructure/embedding/**</include>
          <include>com/enterprise/aiassistant/backend/admin/document/**</include>
          <include>com/enterprise/aiassistant/backend/admin/trash/**</include>
        </includes>
      </configuration>
    </execution>

    <!-- 5. scoped 70% gate -->
    <execution>
      <id>check-scoped-coverage</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
        <includes>
          <!-- same eight patterns as above -->
        </includes>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.70</minimum>
              </limit>
            </limits>
          </rule>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>BRANCH</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.50</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Notes and caveats:

- The branch rule at `0.50` is a **secondary** gate; raise it to `0.55`–`0.60` only after Phase 5 measurement. **[R]**
- `jacoco:merge` requires the `fileSets` element; if the merge goal misbehaves in your Maven/plugin combination, the deterministic fallback is: keep integration tests under Surefire by naming them `*Tests`/`*Test` (then only `prepare-agent` + `report` + `check` are needed). **[R]** Decide in Phase 0 with a one-shot measurement.
- Do **not** put `<includes>` on `prepare-agent` in the first iteration: agent-level filtering uses a different pattern language and can silently exclude nothing. Report/check scoping is sufficient and is the mechanism that defines the denominator. **[R]**
- `mvn test` does **not** reach `verify` → the gate must be run as `./mvnw verify`. Document that in the team README of the test suite. **[R]**
- Do not exclude `FileUploadProperties`, `MinioConfig`, `AsyncConfig`, `QdrantConfig`, `GeminiEmbeddingModelConfig` merely because they are "config": each contains 3–15 real lines and is cheap to cover (see matrix). **[R]**

### 4.4 Generated-code exclusion decision

| Candidate | Decision | Justification |
|---|---|---|
| Lombok accessors/builders/`equals`/`hashCode` (`@Data`, `@Getter`, `@Setter`, `@Builder`) | **Exclude via `lombok.config`** | Compiler-generated; no business logic. JaCoCo's documented `@Generated` filter. **[R]** |
| `dto/request/**`, `dto/response/**` (records + `@Data`) | Hand-written records expose canonical constructors → exclude the folder via `<exclude>**/dto/**</exclude>` **only if** the Lombok route proves insufficient | **[R]** |
| `*.java` QueryDSL `Q*` classes | Do not exist yet — `target/generated-sources` is empty/absent; no exclusion needed until QueryDSL APT actually runs | **[V]** `backend/target` does not exist |
| `enums/**` | Keep in scope (near-zero lines; `values()`/`valueOf` appear once) | Cheap honesty. **[R]** |

---

## 5. Maven Testing Infrastructure Audit

### 5.1 Present **[V]**

| Capability | Declared artifact | Scope |
|---|---|---|
| Spring Boot Test | `spring-boot-starter-*-test` (Boot 4 per-technology split): `data-jpa-test`, `flyway-test`, `validation-test`, `webmvc-test`, `security-test` | test |
| JUnit 5 | via the starters (existing tests import `org.junit.jupiter.api.Test`) | test |
| Mockito | via the starters (existing tests use `org.mockito.Mockito.mock`) | test |
| AssertJ | via the starters **[A]** — the guide lists `MockMvcTester` (AssertJ) as a supported integration, but the project currently uses JUnit assertions | test |
| MockMvc | via `spring-boot-starter-webmvc-test` | test |
| Spring Security Test | `spring-boot-starter-security-test` → `@WithMockUser` available | test |
| Testcontainers | **not declared** | — |
| PostgreSQL Testcontainers | **not declared** | — |
| MinIO test support | **not declared** (no official Testcontainers module; use `GenericContainer`) | — |
| Qdrant test support | **not declared** (no official Testcontainers module; use `GenericContainer` + real `QdrantClient`) | — |

**[?] Unverified transitive specifics:** whether Mockito/AssertJ/JUnit are truly on the test classpath today cannot be confirmed from the POM alone, and `backend/target` does not exist (nothing has been compiled in this workspace). Confirm with `cd backend && ./mvnw -q dependency:tree -Dscope=test`. If any is missing, add **only** `spring-boot-starter-test` (`<scope>test</scope>`) once — Boot 4 still ships it as the umbrella test starter.

### 5.2 Dependencies to add **[R]** (minimum set)

```xml
<!-- Docker-backed integration tests -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<!-- only if async assertions are needed for the processing workers -->
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <scope>test</scope>
</dependency>
```

- Versions: let the Boot 4 parent BOM manage `org.testcontainers` and `org.awaitility`; add an explicit BOM **only** if `dependency:tree` shows an unmanaged version. **[R]/[?]**
- MinIO: **no** dependency needed — the test uses the production `io.minio:minio` client against a container. **[R]**
- Qdrant: **no** dependency needed — the test uses `io.qdrant:client` against a container. **[R]**
- No `spring-boot-testcontainers` / `@ServiceConnection` needed; a `@DynamicPropertySource` base class is simpler and keeps the production config untouched. **[R]**

### 5.3 Surefire / Failsafe recommendation **[R]**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

- Naming convention: unit `*Test`/`*Tests` (Surefire defaults), integration `*IT` (Failsafe defaults `IT*`, `*IT`, `*ITCase`).
- `failsafe:verify` must be bound (as above) or failing ITs will not fail the build.
- Optional determinism tweak (only if date assertions fail): Surefire `<argLine>@{argLine} -Duser.timezone=UTC</argLine>` — the `@{argLine}` placeholder is mandatory once you override `argLine`, otherwise JaCoCo's agent is silently dropped. **[R]**

---

## 6. PostgreSQL / MinIO / Qdrant Testing Strategy

### 6.1 Testcontainers vs Docker Compose (project-specific comparison)

| Criterion | Testcontainers | Existing `docker-compose.yml` |
|---|---|---|
| Data safety | Ephemeral, destroyed per run **[R]** | **Uses `enterprise-postgres-data` declared `external: true`, plus `enterprise-qdrant-data`, `enterprise-minio-data` — real dev data** **[V]** |
| Port conflicts | Random host ports, no clash **[R]** | Fixed `5432`, `9000`, `6333/6334` — collide with an already-running dev stack **[V]** |
| Isolation | One container per test JVM, dedicated credentials/db **[R]** | Shares `POSTGRES_USER/DB` from `.env` **[V]** |
| Startup control | `@DynamicPropertySource` per suite **[R]** | Requires manual `docker compose up`; the brief forbids starting/stopping it |
| Parallelism | Independent per class/singleton **[R]** | Single global instance |
| Image pinning | Pin exact tags **[R]** | `qdrant/qdrant:latest`, `quay.io/minio/minio:latest` (unpinned) **[V]** |

**Decision: Testcontainers. [R]** The Compose file must never be used for automated tests: its volumes are declared `external`, so a "clean" test run against it would write into the developer's real data.

### 6.2 PostgreSQL

- Image `postgres:16-alpine` — **matches** `docker-compose.yml` **[V]**.
- Singleton container started once per JVM, shared through an abstract base class + `@DynamicPropertySource` **[R]**.
- **Schema strategy (important [V] finding):** `V1__create_auth_tables.sql` creates only `users` + `refresh_tokens`; `V2__add_department_rbac_and_document_access.sql` *assumes* `documents`, `folders`, `ai_conversations`, `ai_usage_logs` already exist ("dự án đang chạy `spring.jpa.hibernate.ddl-auto=update` và `spring.flyway.enabled=false`, nên schema thực tế do Hibernate tạo"). A Flyway-only test database therefore **cannot** be created from migrations. For tests use:
  ```properties
  spring.flyway.enabled=false
  spring.jpa.hibernate.ddl-auto=create-drop
  spring.jpa.properties.hibernate.jdbc.time_zone=UTC
  ```
  **[R]** Do not "fix" this by editing production migrations inside this assignment; record it as a release risk (§14).
- Repository slices: prefer `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` **or** a plain `@SpringBootTest` with the container. `@DataJpaTest` in Boot 4 was moved into a technology package — **verify the exact import** (`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` is the expected location; `WebMvcTest` is confirmed at `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`). **[?]/[V]**
- `@DataJpaTest` alone is **not** enough for the custom repository: `DocumentRepositoryCustomImpl` is `@Repository` with `@PersistenceContext` and will be picked up by component scan inside a `@DataJpaTest`? → verify; if not, add `@Import(DocumentRepositoryCustomImpl.class)`. **[R]/[?]**
- Transactions: service-level ITs (`DocumentServiceImplIT`) must **not** use class-level `@Transactional`, or the AFTER_COMMIT event never fires and the "real" behaviour is not observed. Rollback assertions must be written explicitly. **[R]**
- PostgreSQL-specific behaviour that justifies the container: `CAST(:keyword AS string) IS NULL` and `LOWER(...) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))` in `DocumentRepository`/`FolderRepository`, `@Query` DTO constructor projection in `DocumentRepositoryCustomImpl`, and `deleteByDocumentVersionId` derived bulk delete. **[V]**

### 6.3 MinIO

- `GenericContainer` on `quay.io/minio/minio:<pinned tag>` with `server /data`, `MINIO_ROOT_USER/PASSWORD` = test-only values, mapped port 9000. **[R]**
- The test must create the bucket itself: **[V]** no production code calls `makeBucket`. Use `minioClient.makeBucket(MakeBucketArgs.builder().bucket("test-files").build())` in `@BeforeAll`. **[R]**
- Object isolation: every test uses a unique key prefix; assert `bucketName`/`objectKey` from `StoredFileDto`, and read back through `loadAsResource` comparing bytes. Clean up with `removeObject` in `@AfterEach` **[R]** (production has no delete — see §14).
- Failure-path IT: point a second `MinioClient` at a closed port and assert `FileStorageException(FILE_UPLOAD_FAILED / FILE_STORAGE_READ_FAILED)`. **[R]**

### 6.4 Qdrant

- `GenericContainer` on `qdrant/qdrant:<pinned tag>`, expose gRPC 6334 (REST 6333 only if needed). **[R]**
- Per-suite collection name `test_chunks_<random>`; `vectorSize` **must match** `qdrant.vector-size` (production value `3072` **[V]**) or use a test property `qdrant.vector-size=8` and assert against 8 — **recommend 8** for fast, readable deterministic vectors. **[R]**
- Deterministic vectors: unit basis vectors (`[1,0,0,…]`, `[0,1,0,…]`) give exact cosine ordering; assert order and `score ≈ expected` with tolerance. **[R]**
- Filters: two points with different `documentId` payloads → `search(vector, limit, documentId)` returns only the matching one. **[V]** `VectorStoreHelper#buildDocumentFilter` uses `ConditionFactory.match("documentId", documentId)`.
- `scoreThreshold`: `qdrant.score-threshold=0.65` in the example config **[V]**; for ITs set `0.0` so ordering tests are not silently emptied, and add a dedicated threshold test with a controlled orthogonal vector. **[R]**
- Deletion: `delete(pointId)` then search → point absent. Empty collection → empty list. **[R]**
- **Never** reuse the development collection `document_chunks`. **[R]**

### 6.5 Gemini (no real calls, ever)

Three layers of defence **[R]**:

1. **Unit:** `EmbeddingModel` is an interface (`dev.langchain4j.model.embedding.EmbeddingModel`) → plain Mockito mock; `Response<Embedding>` built with `Response.from(Embedding.from(new float[]{...}))` and, in a second test, with a `TokenUsage` to cover both branches of `GeminiEmbeddingService#toEmbeddingResult`.
2. **Integration:** a `@TestConfiguration` bean `DeterministicEmbeddingService implements EmbeddingService` returning a unit-vector derived from `Math.abs(text.hashCode()) % dimension`, plus fixed `inputTokens`, and `getModelName()` = `"deterministic-test-embedding"`. Registered with `@MockitoBean`/`@TestConfiguration` + `@Primary`.
3. **Guardrail:** `gemini.api-key=test-not-a-real-key` in test properties and **no** `@SpringBootTest` may rely on `GeminiEmbeddingModelConfig` producing a working model. `GeminiLLMService` (out of scope) must not be exercised at all; if a full-context test drags it in, it is still safe (bean creation is lazy-by-use, no network at construction **[V]** `GeminiConfig`/`GeminiEmbeddingModelConfig`).

---

## 7. Unit Testing Plan (Phase D)

Conventions used below: **M** = mocked dependencies, **In** = input, **Out** = expected result, **X** = expected exception, **Br** = branches covered.

### 7.1 `document/helper/DocumentHelper` — P0, highest value per line

Dependencies: M `FileUploadProperties` (unused by the code **[V]** — pass a real instance), M `DocumentVersionRepository`, M `DocumentMapper`.
Constants **[V]**: `MAX_FILES = 10`, `MAX_FILE_SIZE = 50 MB`, `ALLOWED_TYPES` = pdf/doc/docx/xls/xlsx/txt, `ALLOWED_SORTS` = {newest, oldest}.

| # | Method | In | Expected |
|---|---|---|---|
| 1 | `validateFiles` | `null` | X `DocumentException FILE_REQUIRED` |
| 2 | `validateFiles` | `List.of()` | X `EMPTY_FILE` |
| 3 | `validateFiles` | 11 mocked files | X `TOO_MANY_FILES` |
| 4 | `validateFiles` | 10 valid files | no throw (Br: `size()>10` false edge) |
| 5 | `validateFile` | `null` | X `FILE_REQUIRED` |
| 6 | `validateFile` | `isEmpty()` true | X `EMPTY_FILE` |
| 7 | `validateFile` | size `50*1024*1024+1` | X `FILE_TOO_LARGE` |
| 8 | `validateFile` | size exactly `50 MB`, `application/pdf` | no throw (boundary) |
| 9 | `validateContentType` (via 8) | `video/mp4` | X `BusinessException UNSUPPORTED_FILE_TYPE` |
| 10 | `validateBatchRequest` | `documents == null` | X `DOCUMENTS_METADATA_REQUIRED` |
| 11 | `validateBatchRequest` | `documents = []` | X `DOCUMENTS_METADATA_REQUIRED` |
| 12 | `validateBatchRequest` | 2 files / 1 doc | X `FILE_METADATA_MISMATCH` |
| 13 | `validateBatchRequest` | blank title | X `DOCUMENT_TITLE_REQUIRED` |
| 14 | `validateBatchRequest` | null `documentType` | X `DOCUMENT_TYPE_REQUIRED` |
| 15 | `validateBatchRequest` | 1:1 valid | no throw |
| 16 | `validateRequest` *(not called by `DocumentServiceImpl` — dead public API **[V]**)* | `null` / blank title / null type / valid | `REQUEST_REQUIRED` / `DOCUMENT_TITLE_REQUIRED` / `INVALID_DOCUMENT_TYPE` / no throw |
| 17 | `validateDocumentId` | `null` / `0` / `-1` / `5` | `DOCUMENT_ID_REQUIRED` / `DOCUMENT_ID_INVALID` ×2 / ok |
| 18 | `validateFilter` | `null` | returns silently (Br: null short-circuit) |
| 19 | `validateFilter` | `sort="random"` | `INVALID_SORT_OPTION` |
| 20 | `validateFilter` | `sort="NEWEST"` | ok (toLowerCase branch) |
| 21 | `validateFilter` | `fromDate > toDate` | `INVALID_DATE_RANGE` |
| 22 | `validateFilter` | `minSize=-1` / `maxSize=-1` / `min>max` | `INVALID_FILE_SIZE` ×2, `INVALID_FILE_SIZE_RANGE` |
| 23 | `validateFilter` | `keyword` length 256 | `KEYWORD_TOO_LONG` |
| 24 | `validateVersionRequest` | `null` | `REQUEST_REQUIRED` (BusinessException) |
| 25 | `validateVersionRequest` | `changeNote` 256 chars | `CHANGE_NOTE_TOO_LONG` |
| 26 | `validateVersionRequest` | `title=""` / `title` 256 / `description` 1001 | `DOCUMENT_TITLE_REQUIRED` / `TITLE_TOO_LONG` / `DESCRIPTION_TOO_LONG` |
| 27 | `validateVersionRequest` | all null | no throw |
| 28 | `validateDocumentStatus` | document `DELETED` / `ACTIVE` | X `DOCUMENT_DELETED` / ok |
| 29 | `validateDocumentIsDeleted` | `ACTIVE` / `DELETED` | X `DOCUMENT_NOT_DELETED` / ok |
| 30 | `validateMetadata` | null title, long description, ok | as 26 |
| 31 | `validateDocumentVersion` | `null` / `1L` | X `DOCUMENT_VERSION_NOT_FOUND` / ok |
| 32 | `validateFileStorageMetadata` | `null` file | X `FileStorageException FILE_NOT_FOUND` |
| 33 | `validateFileStorageMetadata` | blank bucket / blank objectKey | X `FILE_STORAGE_METADATA_INVALID` |
| 34 | `resolveMediaType` (static) | `null`, `" "`, `"not/a/type;;"`, `"application/pdf"` | octet-stream ×3, `APPLICATION_PDF` |
| 35 | `createNewVersion` | repo returns version #3 | M `documentMapper.toDocumentVersion(doc, file, 4, note)`; Out same instance |
| 36 | `createNewVersion` | repo empty | versionNumber `1` |

**(Br)** every `if` in the class, including the `fileUploadProperties` never-read field (no branch).
Sharing validators (`validateShareRequest`, `validateTargetUserId`, `validateShareTarget`) — **excluded** by scope rule (§2.3), adding ~12 uncovered lines.

### 7.2 `document/helper/DocumentAuthorizationHelper` — P0 (pure, zero mocks)

Partially covered today by `AuthorizationPolicyTest` **[V]**. Add the missing branches:

| # | Scenario | Expected |
|---|---|---|
| 1 | principal without `DOCUMENT_READ`, any role | `canRead=false` (permission gate branch) |
| 2 | ADMIN / SUPERVISOR | `true` regardless of owner/department |
| 3 | owner | `true` |
| 4 | non-owner, `hasExplicitAccess=true` | `true` |
| 5 | non-owner, `document.getDepartment() == null` | `true` ("legacy shared document" branch) |
| 6 | non-owner, same department | `true` |
| 7 | non-owner, other department | `false` |
| 8 | `canDownload` with/without `DOCUMENT_DOWNLOAD` | true only when both |
| 9 | `canModify` owner + `document.department != null` + principal **other** department | `false` (subtle AND branch — currently untested) |
| 10 | `canModify` owner + `document.department == null` | `true` |
| 11 | MANAGER same department (not owner) | `true` for UPDATE/DELETE/MANAGE_ACCESS |
| 12 | SUPERVISOR `canUpdate/canDelete/canManageAccess` | `false` (principal role branch) |
| 13 | `document.getOwner() == null` + ADMIN | `true` (null-safe `isOwner`) |

### 7.3 `document/service/DocumentAuthorizationService` — P0

M: `CurrentUserService`, `DocumentAccessRepository`, real `DocumentAuthorizationHelper`.
Covered today: `currentAccessScope` (4 tests). Add:

| # | Method | Scenario | Expected |
|---|---|---|---|
| 1 | `canRead` | `hasExplicitAccess` true | repo `existsByDocumentIdAndUserId` called; true |
| 2 | `canRead` | `document.getId() == null` | repo **never** called (`verifyNoInteractions`) — Br: null-id guard |
| 3 | `requireRead` | denied principal | X `AuthorizationException ACCESS_DENIED` |
| 4 | `requireDownload` | no download permission | X `ACCESS_DENIED` |
| 5 | `requireUpdate` / `requireDelete` / `requireManageAccess` | denied | X `ACCESS_DENIED` (3 tests) |
| 6 | `requireCreate` | no `DOCUMENT_CREATE` | X `PERMISSION_DENIED` (different ErrorCode — must assert) |
| 7 | `filterReadableDocumentIds` | 3 docs: readable own, other-department, shared | Out set = {own, shared}; repo called once with principal id |
| 8 | `filterReadableDocumentIds` | empty collection | empty set, repo still queried (verify) |

### 7.4 `document/mapper/DocumentMapper` — P1

No mocks. One test per method: `toDocument`, `toDocumentMoveResponse`, `toDocumentVersion`, `toUploadResponse` (depends on `document.getCurrentVersion()` — must be set or NPE), `toRestoreResponse`, `toUpdateMetadataReponse` *(sic, typo in source)*, `toUploadNewVersionResponse` (uses `version.getStatus().name()`), `toDocumentDownloadResource`, `toDocumentInfo`, `toCurrentVersionInfo`, `toVersionHistory` (ordering: versions `[1,3,2]` → `[3,2,1]`), `toAdvancedInfo`.
`toDownloadResponse`: **Br** — `fileSize != null` vs `null` (Content-Length header set/absent) + header `Content-Disposition: attachment; filename*=UTF-8''…` for a Vietnamese filename.

### 7.5 `document/service/DocumentServiceImpl` — P0 (largest single class)

M (13): `FileStorageService`, `FileRepository`, `DocumentRepository`, `DocumentVersionRepository`, `DocumentMapper`, `FileMapper`, `DocumentHelper`, `ApplicationEventPublisher`, `FolderService`, `CurrentUserService`, `DocumentAuthorizationService`, `DocumentAccessRepository`, `UserRepository`.

| # | Method | Scenario | Expected |
|---|---|---|---|
| 1 | `upload` | 2 valid files | 2 responses; `fileStorageService.store` ×2; `fileRepository.save` ×2; `resolveTargetFolder` per item; `versionIds` captured from `DocumentVersionCreatedBatchEvent` |
| 2 | `upload` | `validateFiles` throws (`files=[]`) | X `EMPTY_FILE`; `verifyNoInteractions(fileStorageService)` |
| 3 | `upload` | metadata mismatch | X `FILE_METADATA_MISMATCH`; nothing stored |
| 4 | `upload` | `requireCreate` throws | X `PERMISSION_DENIED`; nothing stored |
| 5 | `uploadNewVersion` | `documentId=null` | X `DOCUMENT_ID_REQUIRED` |
| 6 | `uploadNewVersion` | doc not found | X `DOCUMENT_NOT_FOUND` |
| 7 | `uploadNewVersion` | `requireUpdate` throws | X `ACCESS_DENIED` |
| 8 | `uploadNewVersion` | doc `DELETED` | X `DOCUMENT_DELETED` |
| 9 | `uploadNewVersion` | happy path, request title/description/type all null | version saved; `document.currentVersion` replaced; title/desc/type **unchanged** (3 false branches); event `DocumentVersionCreatedEvent(newVersionId)` published |
| 10 | `uploadNewVersion` | request title/description/type set | all 3 overridden (3 true branches) |
| 11 | `updateDocumentMetadata` | `request == null` | X `REQUEST_REQUIRED` |
| 12 | `updateDocumentMetadata` | doc deleted | X `DOCUMENT_DELETED` |
| 13 | `updateDocumentMetadata` | only description set | title/type untouched |
| 14 | `updateDocumentMetadata` | all set | all applied, mapper called |
| 15 | `downloadSelectedVersion` | versionId null | X `DOCUMENT_VERSION_NOT_FOUND` |
| 16 | `downloadSelectedVersion` | version belongs to another document | X `DOCUMENT_VERSION_NOT_FOUND` (Br: `!getId().equals`) |
| 17 | `downloadSelectedVersion` | `file == null` | X `FileStorageException FILE_NOT_FOUND` |
| 18 | `downloadSelectedVersion` | happy | `loadAsResource(bucket, key)` + mapper |
| 19 | `loadProcessingResource` | version not found | X `DOCUMENT_VERSION_NOT_FOUND` |
| 20 | `loadProcessingResource` | `version.getDocument() == null` | status validation skipped (Br) |
| 21 | `loadProcessingResource` | `version.getFile() == null` | X `FILE_NOT_FOUND` |
| 22 | `loadProcessingResource` | happy | resource returned |
| 23 | `deleteDocument` | not found | X `DOCUMENT_NOT_FOUND` |
| 24 | `deleteDocument` | happy | status `DELETED`, `deletedAt` non-null, `deletedBy == currentUser`, `save` called, **no** storage call (soft delete) |
| 25 | `deleteDocument` | already `DELETED` | X `DOCUMENT_DELETED` |
| 26 | `restoreDocument` | doc `ACTIVE` | X `DOCUMENT_NOT_DELETED` |
| 27 | `restoreDocument` | doc `DELETED` | status `ACTIVE`, `deletedAt`/`deletedBy` null, response mapped |
| 28 | `getDocuments` | filter null, scope unrestricted | delegates `filterDocuments(filter, scope, pageable)` |
| 29 | `getDocuments` | invalid sort | X `INVALID_SORT_OPTION`; repo never called |
| 30 | `getDocumentsForAdmin` | calls `currentAccessScope()` | verify delegation order (documented behaviour: relies on ADMIN role) |
| 31 | `getDocumentDetail` | happy (currentVersion + file + 2 versions) | 4 sub-objects mapped; version history sorted desc |
| 32 | `getDocumentDetail` | `currentVersion == null` | **documents current NPE behaviour** (see §14 risk) — assert `NullPointerException` *explicitly* and mark as a defect |
| 33 | `existsByTitle` | true/false | passthrough |
| 34 | `moveDocument` | `folderId=null` | `resolveTargetFolder(null)` used, folder set, response mapped |
| 35 | `moveDocument` | doc deleted | X `DOCUMENT_DELETED` |

Sharing methods 3× — excluded.

### 7.6 `storage/service/MinioFileStorageService` — P0

M: `MinioClient`, `StoredFileMapper`; set `bucket` via `ReflectionTestUtils.setField(service, "bucket", "test-files")`.

| # | Scenario | Expected |
|---|---|---|
| 1 | `store(null)` | X `BusinessException EMPTY_FILE`; `verifyNoInteractions(minioClient)` |
| 2 | `store(empty file)` | X `EMPTY_FILE` |
| 3 | `store(valid)` | `PutObjectArgs` captured: bucket, `object` = `<uuid>-name.pdf`, contentType; DTO from mapper returned |
| 4 | `store` when `putObject` throws `ErrorResponseException` | X `FileStorageException FILE_UPLOAD_FAILED`, cause preserved |
| 5 | `store` when DTO mapper throws | same catch branch (proves the `catch (Exception)` breadth — assert exception type, not the mapper bug) |
| 6 | `loadAsResource` | returns `InputStreamResource`; bucket/object forwarded |
| 7 | `loadAsResource` when `getObject` throws | X `FILE_STORAGE_READ_FAILED` |

### 7.7 `storage/mapper/FileMapper` + `StoredFileMapper` — P1/P2

- `FileMapper.toFileEntity`: extension `"Report.PDF"` → `"pdf"`; `"noext"` → `null`; `null` filename → `null`; bucket/provider injected via `ReflectionTestUtils` (`minio.bucket`, `minio.provider`).
- `StoredFileMapper.toDto`: `objectKey == storedName`; all 6 fields propagated.

### 7.8 `processing/chunking/*` — P0/P1

**`SlidingWindowSplitter.split` [V] `CHUNK_SIZE=1000`, `OVERLAP=200`:**

| # | In | Out |
|---|---|---|
| 1 | `null` | `[]` |
| 2 | `""` | `[]` |
| 3 | 999 chars | 1 piece |
| 4 | exactly 1000 | 1 piece (Br: `end == length` break) |
| 5 | 1001 | 2 pieces; piece2 = chars[800..1001] (overlap proven) |
| 6 | 2500 | 3 pieces with starts 0/800/1600 and total length check |
| 7 | content preservation | concatenation with overlap yields the original at expected offsets |

**`ChunkingStrategyFactory.getStrategy`:** `pdf`, `docx`, `txt`, `doc` → `DocumentChunkingStrategy`; `xlsx`, `xls` → `SpreadsheetChunkingStrategy`; `"PDF"` uppercase → document (Locale.ROOT branch); `"csv"` → X `ProcessingException UNSUPPORTED_FILE_TYPE`; `" "` → X same.

**`ChunkingMapper.toTextChunk`:** `tokenCount = len/4` for 8, 0, 4000 chars.

**`DocumentChunkingStrategy.chunk`:**

| # | Scenario | Expected |
|---|---|---|
| 1 | empty elements list | `[]` (grouping yields empty map) |
| 2 | one paragraph, 50 chars, page 1 | 1 chunk, `chunkIndex=0`, `pageNumber=1`, `startChar=0`, `endChar=51` |
| 3 | heading `h1` + paragraph | chunk content = `"H1\nparagraph"` (breadcrumb prefix) |
| 4 | `h1`,`h2` then paragraph | breadcrumb `"H1 > H2"` |
| 5 | `h1`,`h2`,`h1`, paragraph | new `h1` clears `h2` → breadcrumb = second `h1` only (Br: `tailMap(...).clear()`) |
| 6 | 4 nested headings + paragraph | breadcrumb keeps last 3 (`MAX_HEADING_BREADCRUMB_DEPTH`, Br: `dropCount>0`) |
| 7 | paragraph of 1500 chars | buffer flushed (if any), `SlidingWindowSplitter` pieces, each prefixed with breadcrumb, `endChar = elementStart + piece.length()` |
| 8 | two paragraphs whose sum > 1000 | 2 chunks (Br: `buffer.length()+content+1 > CHUNK_SIZE`) |
| 9 | 3 short paragraphs | single trailing chunk (Br: final `!buffer.isEmpty()`) |
| 10 | pages 1 and 2 | `chunkIndex` continuous across pages (no reset) |
| 11 | heading only, no paragraph | `[]` (no chunk emitted for headings) |

**`SpreadsheetChunkingStrategy.chunk`:**

| # | Scenario | Expected |
|---|---|---|
| 1 | single sheet with header only | 1 chunk = header (Br: `chunkIndex == startChunkIndex`) |
| 2 | header + 2 short rows | 1 chunk containing header + rows |
| 3 | header + rows exceeding 1000 | 2 chunks, **both** starting with header (Br: buffer reset re-appends header) |
| 4 | row of 1500 chars | flushed buffer chunk + 2 pieces each `header + "\n" + piece` (Br: `row.length() > CHUNK_SIZE`) |
| 5 | header-only buffer at flush time | no spurious chunk (Br: `buffer.length() > header.length()`) |
| 6 | 2 sheets | chunk indices continuous across sheets |

**`ChunkingServiceImpl.chunk`:** `fileExtension=null` → X `ProcessingException DOCUMENT_CHUNKING_FAILED`; `" "` → same; `extractedText=null` → X `BusinessException DOCUMENT_TEXT_EMPTY`; empty `elements` → same; factory returns strategy and strategy throws → X `ProcessingException DOCUMENT_CHUNKING_FAILED` with cause; **factory throw is not wrapped** (assert `UNSUPPORTED_FILE_TYPE`, proving the "strategy outside try" comment **[V]**).

### 7.9 `processing/extraction/*` — P0/P1

**`ExtractionHelper` (P0, pure):** `validateExtractedText(null)`, blank content, `"  "`; `resolveTagName(null,"h1")→"h1"`, `resolveTagName("","h1")→"h1"`, `resolveTagName("p","x:p")→"p"`; `resolveHeadingLevel`: `h1`→1, `h6`→6, `h7`→null, `p`→null, `null`→null, `"h10"`→null (length guard), `"H1"`→null; `toPages`: grouping by page preserves insertion order, joins with `"\n"`, single page, empty list → `[]`.

**`TikaStructuredContentHandler` (P0, ~90 lines of branchy SAX code):** drive the handler directly (no Tika):

| # | Sequence | Expected |
|---|---|---|
| 1 | `startElement("p")` + chars + `endElement("p")` | 1 `PARAGRAPH` element, trimmed content, `pageNumber=1` |
| 2 | `startElement("h2")` … | `HEADING`, `headingLevel=2` |
| 3 | `<p><b>` nested (start `b` while `p` open) | nested tag ignored; text still captured |
| 4 | whitespace-only content | element **not** added (Br: `content.isEmpty()`) |
| 5 | `endElement` for a tag that was never opened | early return (Br: `!openTagName.equals(tagName)`) |
| 6 | `div class="page"` twice | subsequent elements get `pageNumber=2` |
| 7 | `div` without `class="page"` | no page increment |
| 8 | `characters()` with no open tag | nothing buffered |
| 9 | two consecutive paragraphs | buffer cleared between them |

**`TextExtractorFactory`:** list of 2 mocks, first supports → returned; none supports → X `BusinessException TEXT_EXTRACTOR_NOT_FOUND` with mime appended; empty list → same.

**`PdfTextExtractor` / `DocxTextExtractor` / `TxtTextExtractor`:** `supports()` true for exact mime and for upper-case variant; false for another mime (`TxtTextExtractor.supports(null)` → false, no NPE); `extract` delegates to `TikaContentExtractor.extract(inputStream, filename)`; `Resource.getInputStream()` throws `IOException` → X `ProcessingException TEXT_EXTRACTION_FAILED` (one per extractor; same shape).

**`ExcelTextExtractor` (P1, real POI in memory):** build `.xlsx` with 2 sheets (sheet1: header + 2 rows, blank cells; sheet2: header only) via `XSSFWorkbook` → `ByteArrayResource`; assert `elements` are `TABLE_ROW` with `pageNumber = 1/2`, blank cell skipped, `content` non-blank rows only, `extractionMethod = DIRECT_TEXT`; unreadable bytes (`new byte[]{1,2,3}`) → X `ProcessingException TEXT_EXTRACTION_FAILED`.

**`TikaContentExtractor` (P1):** real extraction of a tiny `.txt` (`new ByteArrayResource("hello".getBytes())`) → `content` non-blank, `pages` size ≥ 1, `DIRECT_TEXT`; `InputStream` that throws → X `ProcessingException`. *(PDF/DOCX binary fixtures are optional; the handler unit tests already cover structure logic.)*

**`TextExtractionServiceImpl` / `ExtractionMapper`:** delegation + mapping fields.

### 7.10 `processing/orchestration/*` — P0/P1

**`DocumentProcessingService.process` (P0, highest orchestration value).** M (11): `DocumentVersionRepository`, `FileStorageService`, `DocumentTextRepository`, `TextExtractionService`, `ExtractionHelper`, `ChunkingService`, `DocumentChunkRepository`, `EmbeddingService`, `VectorStoreService`, `ProcessingMapper`, `ProcessingHelper`.

| # | Scenario | Expected |
|---|---|---|
| 1 | version not found | X `BusinessException DOCUMENT_VERSION_NOT_FOUND`; `processingHelper.handleFailureUnlessAlreadySucceeded(id, null, ex)` called (Br: `version == null`) |
| 2 | version `READY` | `processingHelper.validateStatus` throws → same handler called → X |
| 3 | happy path, 2 chunks | status transitions recorded in order (`PROCESSING`→`TEXT_EXTRACTING`→`CHUNKING`→`EMBEDDING`); `documentTextRepository.save` ×1; `documentChunkRepository.save` ×2; `embeddingService.embed` ×2; `vectorStoreService.upsert(2 points)` ×1; final `READY` + `processingStep == null` |
| 4 | extraction returns blank text | `extractionHelper` throws `BusinessException DOCUMENT_TEXT_EMPTY` → rethrown **unwrapped** (BusinessException branch) |
| 5 | chunking throws `RuntimeException` | wrapped as `BusinessException DOCUMENT_PROCESSING_FAILED` with cause; handler invoked |
| 6 | `embeddingService.embed` throws on 2nd chunk | `processingHelper.logUsage(model, null, FAILED, msg)` called; wrapped exception |
| 7 | `vectorStoreService.upsert` throws | wrapped; handler invoked; version not `READY` |
| 8 | 0 chunks | `upsert(List.of())` still called (documents current behaviour), version `READY` |

**`ProcessingHelper` (P1):** `validateStatus(READY)` → X `DOCUMENT_VERSION_INVALID_STATUS`; `PENDING/PROCESSING/FAILED` → no throw; `handleFailed` version present → sets `FAILED`+step+message; version absent → no exception, warn branch (verify via `assertDoesNotThrow`); `handleFailureUnlessAlreadySucceeded` READY → `handleFailed` **not** called (`verify(..., never())`); `FAILED/PENDING` → called with the current `processingStep`; `null` version → called with `null` step; `logUsage` → capture `AIUsageLogRequest` and assert `ConversationType.DOCUMENT_INDEXING`, `outputTokens=0`. *(Note: `@Transactional(REQUIRES_NEW)` cannot be asserted without Spring — document it, do not fake it.)*

**`ProcessingMapper` (P1):** `toDocumentText`, `toDocumentChunk`, `toVectorPoint` — assert payload holds `chunkId == chunk.id`, `documentId`/`documentVersionId` from the version chain, `embeddingModel` from the result, `id == chunk.getId()` and `vector` identity.

**`DocumentProcessingBatchWorker` (P1):** inject a same-thread `Executor` (`Runnable::run`) → all `versionIds` processed; empty list → no calls; retry executor throws on the 2nd id → the 1st still processed and the exception is swallowed (Br: catch).

**`DocumentProcessingWorker` (P2):** `onDocumentVersionCreated(event)` → `retryExecutor.processWithRetry(id)`; throwing executor → exception swallowed (`assertDoesNotThrow`).

**`DocumentProcessingRetryExecutor` (P2):** single-line delegation → covered by the worker/batch tests; **do not** build a Spring `@EnableRetry` context just to assert `maxAttempts` (annotation values are visible in source; asserting them tests the framework, not the code).

**`AsyncConfig` (P2):** assert returned `ThreadPoolTaskExecutor` has core 3 / max 5 / queue 100 / prefix `document-processing-`.

### 7.11 `ai/infrastructure/embedding/GeminiEmbeddingService` — P0

M: `EmbeddingModel`; real `GeminiProperties` (set via setters).

| # | Scenario | Expected |
|---|---|---|
| 1 | `embed(null)` | X `EmbeddingException EMBEDDING_TEXT_REQUIRED`; `verifyNoInteractions(embeddingModel)` |
| 2 | `embed("   ")` | same |
| 3 | `embed("hello")`, `Response.from(Embedding.from(vector))` (no token usage) | `dimension == vector.length`, `model == properties.getEmbeddingModel()`, `inputTokens == max(1, 5/4) == 1` |
| 4 | same with `TokenUsage(7,0)` | `inputTokens == 7` (Br: tokenUsage non-null) |
| 5 | `response.content() == null` | X `EmbeddingException EMBEDDING_FAILED` (Br: catch-all) |
| 6 | `embeddingModel.embed` throws | X `EmbeddingException EMBEDDING_FAILED`, cause preserved |
| 7 | `getModelName()` | configured value |
| 8 | `toEmbeddingResult` direct call with a 3072-length vector | dimension 3072, token estimate `len/4` |

**`GeminiEmbeddingModelConfig` (P2):** `embeddingModel()` returns non-null with `GoogleAiEmbeddingModel` and does not perform network I/O (construct + assert type + `apiKey`/`modelName` via reflection or just non-null).

### 7.12 `ai/infrastructure/vectorstore/*` — P0/P1

**`VectorStoreHelper` (P0, pure + `QdrantProperties` with `vectorSize=4`):**

| # | Method | Scenario | Expected |
|---|---|---|---|
| 1 | `validateVectorPoints` | `null` | X `VECTOR_POINTS_REQUIRED` |
| 2 | | `List.of()` | returns (Br: empty early-return) |
| 3 | | list containing `null` | X `VECTOR_POINT_REQUIRED` |
| 4 | | point without id | X `VECTOR_POINT_ID_REQUIRED` |
| 5 | | `vector == null` / `length == 0` | X `VECTOR_REQUIRED` (message contains id) |
| 6 | | length 3 vs 4 | X `VECTOR_DIMENSION_INVALID` with formatted message |
| 7 | | 2 valid points | no throw |
| 8 | `validatePointId` | `null` / `0` / `-1` / `7` | `VECTOR_POINT_ID_REQUIRED` / `VECTOR_POINT_ID_INVALID` ×2 / ok |
| 9 | `validateSearchRequest` | `null`/empty vector, wrong dim, `limit=0`, `limit=-1`, `documentId=0`, valid | `VECTOR_REQUIRED`, `VECTOR_DIMENSION_INVALID`, `VECTOR_SEARCH_LIMIT_INVALID` ×2, `DocumentException DOCUMENT_ID_INVALID`, ok |
| 10 | `buildDocumentFilter` | `null` | `null` (no filter) |
| 11 | | `5L` | filter with 1 must-condition on `"documentId"`, integer value 5 |
| 12 | `putLong/putInt/putString` | null vs non-null | key absent when null (6 branches) |
| 13 | `getLong/getInt/getString` | missing key, wrong value type, correct type | `null`, `null`, value |

**`VectorStoreMapper` (P1):** `toQdrantPayload` → all 10 keys (nulls omitted); round-trip `toVectorPayload(toQdrantPayload(payload))` equality; `toVectorPayload` on empty map → all nulls; `toPointStruct` id + vector + payload; `toSearchResult` (`sort`/`pointId` from `getId().getNum()`, score cast); `toSearchResults` list size; `toFloatList` content/order.

**`QdrantVectorStoreService` (P0):** M `QdrantClient`, `QdrantProperties` (collection, threshold 0.6), `VectorStoreMapper` (real), `VectorStoreHelper` (real).
Async stubbing: `when(qdrantClient.upsertAsync(anyString(), anyList())).thenReturn(SettableApiFuture.create()...)`; complete the future (or `Futures.getUnchecked` on a completed future) — **[?]** confirm the exact `SettableApiFuture` factory in `io.qdrant:client 1.15.0`.

| # | Scenario | Expected |
|---|---|---|
| 1 | `upsert(point)` | delegates to list overload; `upsertAsync("document_chunks", 1 struct)` |
| 2 | `upsert(List.of())` | no exception, `upsertAsync` called with empty list (current behaviour) |
| 3 | `upsert(null)` | X `VECTOR_POINTS_REQUIRED`, client untouched |
| 4 | `upsert` client throws | X `VectorStoreException VECTOR_UPSERT_FAILED` |
| 5 | `delete(null)` / `delete(0L)` | X from helper, client untouched |
| 6 | `delete(9L)` | `deleteAsync` with `pointId 9`; X `VECTOR_DELETE_FAILED` on failure |
| 7 | `search(vec, 5, null)` | request **without** filter (Br); `scoreThreshold` from properties; `withPayload` enabled |
| 8 | `search(vec, 5, 42L)` | request **with** filter (`ArgumentCaptor<SearchPoints>`); `limit=5` |
| 9 | `search` wrong dim / `limit=0` | X from helper |
| 10 | `search` client throws | X `VectorStoreException VECTOR_SEARCH_FAILED` |
| 11 | `VectorStoreService.search(vec, limit)` (default method) | delegates with `documentId == null` |

**`QdrantCollectionManager` (P1):** `initialize()` when `collectionExistsAsync` resolves `true` → `createCollectionAsync` never called; when `false` → created with `size = properties.vectorSize` and `Distance.Cosine` (captor).

**`QdrantConfig` (P2):** `qdrantClient()` non-null; host/port/tls taken from properties (no connection made at construction). **Note [V]:** `QdrantProperties.apiKey` and `restPort` are unused by the config — they are dead configuration.

### 7.13 `ai/knowledge/search/*` — P0/P1

**`SearchHelper` (P0, M `AIUsageLogService`):** `validateSearchRequest(null)`; keyword null / `""` / `"   "` → `SEARCH_KEYWORD_REQUIRED`; keyword 256 chars → `KEYWORD_TOO_LONG`; 255 → ok; `topK=0`, `topK=51` → `INVALID_TOP_K`; `topK=1`, `50` → ok; `documentId=0`, `-1` → `INVALID_DOCUMENT_ID`; `resolveTopK(null)→10`, `resolveTopK(7)→7`; `logUsage` → captured `AIUsageLogRequest` with `SEMANTIC_SEARCH`, `outputTokens=0`, given status/message.

**`SemanticSearchServiceImpl` (P0, 7 mocks incl. `CurrentUserService`, `DocumentAuthorizationService`):**

| # | Scenario | Expected |
|---|---|---|
| 1 | invalid request | X from helper; `requirePermission` never called |
| 2 | `requirePermission` throws | X `AuthorizationException`; embedding never called |
| 3 | embedding OK, vector search returns `[]` | `List.of()`, `logUsage(SUCCESS)` |
| 4 | hits for a `DELETED` document | filtered out (`fetchActiveDocuments` Br: status filter) |
| 5 | hit whose `documentVersionId` ≠ `currentVersion.id` | filtered out (Br: `isCurrentVersionHit` false) |
| 6 | readable hit, current version | mapped result with score/page/chars/content |
| 7 | document not readable (`filterReadableDocumentIds` returns subset) | filtered out; mapper receives only readable hits |
| 8 | `currentVersion == null` | filtered out (Br: null guard) |
| 9 | `embeddingService.embed` throws | `logUsage(model, null, FAILED, msg)` **then** rethrow same exception (`assertSame`) |
| 10 | model name taken from `EmbeddingResult.getModel()` when embed succeeds | captured usage log has the result's model, not `getModelName()` |

**`SearchMapper` (P1):** all 8 fields, plus `toSemanticSearchResults` on empty list.

**`SemanticSearchController` (P2):** standalone MockMvc: `GET /api/v1/search/semantic?keyword=x&topK=3` → 200 + JSON array; delegation verified; validation is delegated (no local logic).

### 7.14 `admin/document/*` + `admin/trash/*` — P1/P0

**`AdminDocumentService` (P1, M `DocumentService`):** 6 one-line delegations — `getDocuments` must call **`getDocumentsForAdmin`** (regression guard, Br-free); `deleteDocument` returns void; `restoreDocument` returns mapped response.

**`AdminTrashService` (P0, M `DocumentRepository`, `FolderRepository`, `CurrentUserService`):**

| # | Scenario | Expected |
|---|---|---|
| 1 | `requirePermission(DOCUMENT_DELETE)` throws | X `AuthorizationException PERMISSION_DENIED`; repos untouched |
| 2 | `type=null` (both sources) | documents + folders merged, size = sum |
| 3 | `type="DOCUMENT"` | `folderRepository` never called (Br) |
| 4 | `type="folder"` (case-insensitive) | `documentRepository` never called (Br) |
| 5 | `keyword="  x  "` | trimmed `"x"` passed to both repos (Br: trim) |
| 6 | `keyword="   "` | `null` passed (Br: `isBlank`) |
| 7 | deletedAt values `[null, t1, t2]` | order `[t2, t1, null]` (Br: `nullsLast` + `reverseOrder`) |
| 8 | pageable page 0 size 2 of 5 items | `content.size()==2`, `totalElements==5`, `totalPages==3` |
| 9 | pageable offset > size | empty content, no `IndexOutOfBounds` (Br: `Math.min`) |
| 10 | folder with null owner/department/deletedBy | nulls in response, no NPE (3 branches) |

**`AdminTrashController` / `AdminDocumentController` (P2):** `MockMvc` standalone or `@WebMvcTest` + `@WithMockUser(roles="ADMIN")` + `@Import` of a method-security config (`@EnableMethodSecurity`) — verify 200/204, `POST /admin/trash/documents/{id}/restore` returns 204, and (if method security is enabled in the slice) a 403 for a non-admin. Keep these to ~6 assertions total; the controllers hold no business logic.

---

## 8. Integration Testing Plan (Phase E)

### 8.1 Shared infrastructure

**[R]** Three abstract base classes + one fixture factory:

| Base class | Container | Notes |
|---|---|---|
| `support/AbstractPostgresIT` | `PostgreSQLContainer<>("postgres:16-alpine")` singleton | `@DynamicPropertySource`: datasource URL/user/pass, `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=create-drop`, `hibernate.jdbc.time_zone=UTC` |
| `support/AbstractMinioIT` | `GenericContainer<>("quay.io/minio/minio:<pinned>")` | creates bucket `test-files` in `@BeforeAll`; builds `MinioClient` with the mapped endpoint |
| `support/AbstractQdrantIT` | `GenericContainer<>("qdrant/qdrant:<pinned>")` | gRPC 6334; collection `test_chunks_<uuid>`; `qdrant.vector-size=8`, `qdrant.score-threshold=0.0` |
| `support/TestFixtures` | — | builders for `User`, `Department`, `Folder`, `FileEntity`, `Document`, `DocumentVersion`; `persist(...)` helpers using real repositories |

Containers are `static` + started once per test JVM (`@Testcontainers` + `@Container` on `static` fields is fine; **prefer** a manually started singleton to share across classes without `withReuse`). **[R]**

### 8.2 PostgreSQL integration

| Test class | Covers | Key assertions |
|---|---|---|
| `document/repository/DocumentRepositoryCustomImplIT` | `filterDocuments` (list + count) | DTO projection field-by-field; each filter branch in `appendFilters`: keyword (case-insensitive, `%…%`), `documentType`, `extension`, `minSize`, `maxSize`, `versionStatus`, `documentStatus`, `ownerId`, `departmentId`, `fromDate`, `toDate`; `sort=oldest` → ascending vs default descending; ABAC: `unrestricted=true` (all rows), restricted owner-only, restricted + department, restricted + shared ids, restricted + `department IS NULL` (legacy doc visible); pagination `size=2,page=1` → correct slice + `totalElements`; `PageImpl` metadata |
| `document/repository/DocumentRepositoryIT` | `findDeletedDocuments(keyword, pageable)` | keyword `null` → all DELETED (this exercises the PostgreSQL-specific `CAST(:keyword AS string) IS NULL`); `"abc"` → case-insensitive match; ordering by `deletedAt DESC`; ACTIVE documents excluded; paging metadata. Also `existsByTitle`, `countByStatus`, `countByDepartmentIdAndStatus`, `findByIdIn`, `findByFolderId`, `findByFolderIdAndStatus` |
| `document/repository/DocumentVersionRepositoryIT` | derived queries | `findTopByDocumentIdOrderByVersionNumberDesc` empty → `Optional.empty`, 3 versions → #3; `countByDocumentId`; **unique (document_id, version_number)** → `DataIntegrityViolationException`; `Document#versions` cascade + orphanRemoval (delete document → versions gone) |
| `document/repository/DocumentAccessRepositoryIT` | sharing *storage* used by read authorization | `existsByDocumentIdAndUserId`, `findByDocumentIdAndUserId`, `findByDocumentIdOrderByCreatedAtAsc`, `deleteByDocumentId` (derived bulk delete requires a transaction — assert the row is gone and **that a `@Transactional` boundary is needed**), unique (document_id,user_id) violation, `findDocumentIdsSharedWithUser` returns only that user's ids |
| `document/repository/DocumentChunkRepositoryIT` | chunk persistence | `findByDocumentVersionIdOrderByChunkIndexAsc` ordering; unique (version, chunk_index); `deleteByDocumentVersionId` |
| `document/repository/DocumentTextRepositoryIT` | text persistence | `findByDocumentVersion`, `findByDocumentVersionId`, `existsByDocumentVersionId`, `deleteByDocumentVersionId`, unique on version |
| `document/service/DocumentServiceImplIT` | transactions | `upload()` inside the real transaction: document + version + file rows committed, `currentVersion` FK set; `deleteDocument()` persists `DELETED`+`deletedAt`+`deletedBy` and **leaves the MinIO object**; rollback: force a failure after `fileStorageService.store` (e.g. `@MockitoBean FolderService` throwing) → **no** `documents` row while the object **is** in MinIO (documents the orphan-leak risk); `restoreDocument` clears the audit columns |
| `admin/trash/AdminTrashRepositoryIT` (narrow) | trash persistence | seed deleted documents (varied `deletedAt`, one `null`) + deleted folders through real repositories; `AdminTrashService.getTrashItems` returns the merged, sorted, paged result; `restoreDocument` flips status back and the item disappears from the trash list |

### 8.3 MinIO integration

`storage/service/MinioFileStorageServiceIT` (extends `AbstractMinioIT`, real `MinioClient`, real `StoredFileMapper`):
1. `store(MockMultipartFile("report.pdf","application/pdf",bytes))` → `objectKey` starts with a UUID, `store` returns `bucket == "test-files"`.
2. `loadAsResource(bucket, key)` → bytes equal to the uploaded bytes (content integrity).
3. Two uploads of identical content → **two distinct object keys** (no dedup).
4. `loadAsResource("test-files", "missing-key")` → `FileStorageException FILE_STORAGE_READ_FAILED`.
5. `store` with a client pointed at a closed port → `FileStorageException FILE_UPLOAD_FAILED`.
6. **No delete test** — `FileStorageService` has no delete method **[V]**.

### 8.4 Qdrant integration

`ai/infrastructure/vectorstore/QdrantVectorStoreServiceIT` (extends `AbstractQdrantIT`, real `QdrantClient`, real mapper/helper, `vectorSize=8`):
1. `QdrantCollectionManager.initialize()` creates the collection; a second `initialize()` is a no-op (idempotent).
2. `upsert(deterministicPoint(1, e1))`, `upsert(point 2, e2)` → `search(e1, 10)` returns point 1 first, `score ≈ 1.0`.
3. Metadata filter: points 1/2 share `documentId=100`, point 3 has `documentId=200` → `search(e1, 10, 100L)` excludes point 3; `search(e1, 10, null)` includes it.
4. `delete(2L)` → `search(e2, 10)` no longer returns point 2.
5. Search on an empty collection → empty list (no exception).
6. Payload round-trip through the server: `chunkId/documentId/documentVersionId/pageNumber/content` come back intact.
7. `upsert` with a wrong-dimension vector → `VectorStoreException`/`BusinessException` (helper validation) and the collection is unchanged.

### 8.5 Application workflow integration (mock Gemini, real everything in scope)

All in one `support/`-backed `@SpringBootTest` family. **[V]** Startup implications to handle: `QdrantCollectionManager` needs a Qdrant container (or `@MockitoBean QdrantClient`), `RbacInitializer` needs `users`/`role_permissions`/`ai_conversations` (created by `create-drop`), `RootFolderInitializer` needs `folders`.

| Workflow | Test class | Composition | Assertions |
|---|---|---|---|
| **1. Document upload** REST → validation → storage → PG | `document/DocumentUploadWorkflowIT` | `MockMvc` + real `MinioFileStorageService` + real PG + `@MockitoBean EmbeddingService`, `@MockitoBean VectorStoreService`, `@WithMockUser` with `DOCUMENT_CREATE` | 200 + JSON `documentId/currentVersionId/versionNumber=1/status`; DB: 1 `documents`, 1 `document_versions`, 1 `files`, `folder` = root when `folderId` omitted; MinIO: 1 object; 400 for unsupported content type; 403 without permission |
| **2. Processing** document → extract → chunk → embed → Qdrant | `processing/DocumentProcessingWorkflowIT` | real PG + real MinIO (file fixture) + deterministic `EmbeddingService` + real Qdrant | call `DocumentProcessingService.process(versionId)`; assert 1 `document_texts`, `N` `document_chunks`, `version.status=READY`, `processingStep=null`, `N` searchable points in Qdrant; failure variant: `TextExtractionService` throws `ProcessingException` → version `FAILED` with `processingStep=TEXT_EXTRACTING` and `errorMessage` (**this validates the `REQUIRES_NEW` failure handler**) |
| **3. Semantic search** query → embed → Qdrant → filter → results | `ai/knowledge/search/SemanticSearchWorkflowIT` | real PG (ACTIVE document + current version + chunks) + real Qdrant (seeded via `VectorStoreService`) + deterministic embedding + mocked `CurrentUserService` | admin-ish principal → 1 result with correct page/content; matched document `DELETED` → empty; matched hit from an **old version** → empty; document in another department + non-admin principal → empty |
| **4. Versioning** create → update → version management → retrieval | `document/DocumentVersioningWorkflowIT` | real PG + real MinIO + service under test | `uploadNewVersion` → version #2, `currentVersion` switched, `DocumentDetailResponse.versionHistory` ordered `[2,1]`, `changeNote` preserved, `title` override applied only when provided; `downloadSelectedVersion` returns each version's own bytes |
| **5. Trash lifecycle** delete → trash → restore → (permanent delete) | `admin/trash/TrashLifecycleWorkflowIT` | real PG (+MinIO for the "file still exists" assertion) | `deleteDocument` → `AdminTrashService.getTrashItems(null,null,paged)` contains it with `deletedByName`; `restoreDocument` → `ACTIVE`, trash empty; delete again → still restorable. **Permanent document delete does not exist** — assert only that folder hard delete lives outside this scope (`FolderService#hardDeleteFolder`) **[V]** |

**[R] Do not** write a `@SpringBootTest` that exercises the asynchronous event chain end-to-end (upload → AFTER_COMMIT → `documentProcessingExecutor` → retry proxy). It is time-dependent and the executors would run real Gemini unless double-mocked. Cover it as: unit test on the workers + workflow 2 by calling `DocumentProcessingService.process` directly. Record this as a deliberate gap.

---

## 9. Complete Class-Level Test Matrix

Legend — Type: **U** unit, **I** integration, **B** both. Coverage: **H** high, **M** medium, **L** low. Order = recommended implementation order within a package.

### 9.1 `document`

| Package | Class (path under `.../backend/`) | Responsibility | Type | Dependencies to mock | Test scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| document | `document/helper/DocumentHelper.java` | all request/file/filter/status validation; version numbering | U | FileUploadProperties, DocumentVersionRepository, DocumentMapper | §7.1 #1–36 | P0 | H |
| document | `document/helper/DocumentAuthorizationHelper.java` | pure RBAC/ABAC decisions | U | — | §7.2 #1–13 | P0 | M |
| document | `document/service/DocumentAuthorizationService.java` | authorization gate + access scope + readable filtering | U | CurrentUserService, DocumentAccessRepository | §7.3 #1–8 | P0 | M |
| document | `document/service/DocumentServiceImpl.java` | upload/version/metadata/download/delete/restore/list/move orchestration | B | 13 collaborators (§7.5) | §7.5 #1–35 (+IT rollback) | P0 | H |
| document | `document/repository/DocumentRepositoryCustomImpl.java` | JPQL filter/count/ABAC query builder | B | (EntityManager via container) | §8.2 row 1; extend existing `appendAccessScope` unit test (already present) | P0 | H |
| document | `document/mapper/DocumentMapper.java` | entity→DTO mapping, download response | U | — | §7.4 | P1 | M |
| document | `document/repository/DocumentRepository.java` | derived queries + `findDeletedDocuments` | I | — | §8.2 row 2 | P1 | L (interface = no lines) |
| document | `document/repository/DocumentVersionRepository.java` | top version, count | I | — | §8.2 row 3 | P1 | L |
| document | `document/repository/DocumentAccessRepository.java` | explicit-access persistence (read path only) | I | — | §8.2 row 4 | P1 | L |
| document | `document/repository/DocumentChunkRepository.java` | chunk persistence | I | — | §8.2 row 5 | P2 | L |
| document | `document/repository/DocumentTextRepository.java` | extracted-text persistence | I | — | §8.2 row 6 | P2 | L |
| document | `document/repository/DocumentRepositoryCustom.java` | interface | — | — | no test (no code) | P2 | L |
| document | `document/controller/DocumentController.java` | HTTP mapping only | U | DocumentService, DocumentMapper | 6 MockMvc cases (§7.14 tail) | P2 | M |
| document | `document/entity/{Document,DocumentVersion,DocumentChunk,DocumentText,DocumentAccess}.java` | JPA mapping + lifecycle callbacks | I | — | `@PrePersist` sets `createdAt`; `@PreUpdate` sets `updatedAt`; cascade/orphanRemoval; unique constraints (via ITs above) | P2 | L (Lombok-filtered) |
| document | `document/enums/{DocumentStatus,DocumentType,ExtractionMethod,ProcessingStep,VersionStatus}.java` | enums | — | — | exercised implicitly | P2 | L |
| document | `document/dto/**` (17 classes incl. `DocumentAccessScope`) | data carriers | — | — | none (see §4.4) | P3 | L |
| document | `document/service/DocumentService.java` | interface | — | — | none | — | — |

### 9.2 `storage`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| storage | `storage/service/MinioFileStorageService.java` | MinIO put/get + exception translation | B | MinioClient, StoredFileMapper (unit); real client (IT) | §7.6 #1–7; §8.3 1–5 | P0 | H |
| storage | `storage/mapper/FileMapper.java` | DTO→entity, extension parsing, `@Value` bucket/provider | U | — (ReflectionTestUtils) | §7.7 | P1 | M |
| storage | `storage/mapper/StoredFileMapper.java` | MultipartFile→DTO | U | — | §7.7 | P2 | L |
| storage | `storage/controller/FileTestController.java` | debug upload endpoint | U | FileStorageService | MockMvc standalone: 200 + body passthrough | P2 | L |
| storage | `storage/config/MinioConfig.java` | `MinioClient` bean | U | — | bean non-null; endpoint/credentials reflect properties | P2 | L |
| storage | `storage/config/FileUploadProperties.java` | `file.upload.max-size` binding | U | — | bind via `ApplicationContextRunner`/`Binder` (`DataSize.ofMegabytes(500)`) | P3 | L |
| storage | `storage/repository/FileRepository.java` | JPA repo | I | — | save/find in workflow ITs | P2 | L |
| storage | `storage/entity/FileEntity.java` | entity + `@PrePersist` | I | — | `createdAt` set on persist | P2 | L |
| storage | `storage/service/FileStorageService.java` | interface (2 methods) | — | — | none | — | — |
| storage | `storage/dto/response/StoredFileDto.java` | data carrier | — | — | none | P3 | L |

### 9.3 `processing/*`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| processing | `processing/chunking/support/SlidingWindowSplitter.java` | fixed-size windows with overlap | U | — | §7.8 1–7 | P0 | H |
| processing | `processing/chunking/strategy/DocumentChunkingStrategy.java` | page grouping, heading breadcrumb, buffer flush | U | ChunkingMapper, SlidingWindowSplitter (both real) | §7.8 1–11 | P0 | H |
| processing | `processing/chunking/strategy/SpreadsheetChunkingStrategy.java` | sheet grouping, header re-injection | U | same (real) | §7.8 1–6 | P0 | H |
| processing | `processing/chunking/strategy/ChunkingStrategyFactory.java` | extension → strategy | U | 2 real strategies | §7.8 factory rows | P0 | M |
| processing | `processing/chunking/service/ChunkingServiceImpl.java` | input validation + error wrapping | U | Factory, Strategy | §7.8 service rows | P0 | M |
| processing | `processing/chunking/mapper/ChunkingMapper.java` | token estimate | U | — | §7.8 | P1 | L |
| processing | `processing/extraction/helper/ExtractionHelper.java` | tag/heading resolution, page grouping, empty-text guard | U | — | §7.9 | P0 | H |
| processing | `processing/extraction/extractor/tika/TikaStructuredContentHandler.java` | SAX → `DocumentElement` | U | real ExtractionHelper + ExtractionMapper | §7.9 1–9 | P0 | H |
| processing | `processing/extraction/extractor/TextExtractorFactory.java` | first `supports()` match | U | List<TextExtractor> mocks | §7.9 | P0 | M |
| processing | `processing/extraction/extractor/ExcelTextExtractor.java` | POI sheet/row extraction | U | real POI workbook | §7.9 | P1 | H |
| processing | `processing/extraction/extractor/tika/TikaContentExtractor.java` | Tika parse + pages rebuild | U | real Tika (tiny txt) / throwing stream | §7.9 | P1 | M |
| processing | `processing/extraction/extractor/{Pdf,Docx,Txt}TextExtractor.java` | mime gate + delegate + IOException wrap | U | TikaContentExtractor, Resource | §7.9 (3×3 cases) | P1 | M |
| processing | `processing/extraction/service/TextExtractionServiceImpl.java` | factory + delegate | U | Factory, Extractor | §7.9 | P1 | L |
| processing | `processing/extraction/mapper/ExtractionMapper.java` | DTO building | U | — | §7.9 | P1 | L |
| processing | `processing/orchestration/service/DocumentProcessingService.java` | extract→chunk→embed→upsert pipeline + status machine | B | 11 collaborators; IT with real infra | §7.10 #1–8; §8.5 workflow 2 | P0 | H |
| processing | `processing/orchestration/helper/ProcessingHelper.java` | status guard, failure marking, usage logging | U (I for `REQUIRES_NEW`) | DocumentVersionRepository, AIUsageLogService | §7.10 | P1 | M |
| processing | `processing/orchestration/mapper/ProcessingMapper.java` | entity/point mapping | U | — | §7.10 | P1 | M |
| processing | `processing/orchestration/worker/DocumentProcessingBatchWorker.java` | fan-out to executor | U | RetryExecutor, same-thread Executor | §7.10 | P1 | L |
| processing | `processing/orchestration/worker/DocumentProcessingWorker.java` | async event entry point | U | RetryExecutor | §7.10 | P2 | L |
| processing | `processing/orchestration/worker/DocumentProcessingRetryExecutor.java` | `@Retryable` declaration | U | DocumentProcessingService | delegation only; do not assert annotations | P2 | L |
| processing | `processing/config/AsyncConfig.java` | executor bean | U | — | pool sizes/prefix | P2 | L |
| processing | `processing/extraction/dto/{DocumentElement,ExtractedText}.java`, `enums/ElementType.java`, `chunking/dto/TextChunk.java`, `orchestration/event/*.java` | data carriers | — | — | none | P3 | L |

### 9.4 `ai/knowledge/search`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| search | `ai/knowledge/search/service/SemanticSearchServiceImpl.java` | query→embed→search→authorize→map→log | B | 7 collaborators; IT with real PG+Qdrant | §7.13 #1–10; §8.5 workflow 3 | P0 | H |
| search | `ai/knowledge/search/helper/SearchHelper.java` | request validation, topK, usage logging | U | AIUsageLogService | §7.13 | P0 | M |
| search | `ai/knowledge/search/mapper/SearchMapper.java` | hit→result | U | — | §7.13 | P1 | L |
| search | `ai/knowledge/search/controller/SemanticSearchController.java` | HTTP mapping | U | SemanticSearchService | MockMvc | P2 | L |
| search | `ai/knowledge/search/dto/**`, `service/SemanticSearchService.java` | carriers/interface | — | — | none | P3 | L |

### 9.5 `ai/infrastructure/vectorstore`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| vectorstore | `ai/infrastructure/vectorstore/helper/VectorStoreHelper.java` | validation + filter building + payload accessors | U | QdrantProperties | §7.12 1–13 | P0 | H |
| vectorstore | `ai/infrastructure/vectorstore/service/QdrantVectorStoreService.java` | upsert/delete/search against Qdrant | B | QdrantClient, properties (unit); real client (IT) | §7.12 1–11; §8.4 1–7 | P0 | H |
| vectorstore | `ai/infrastructure/vectorstore/mapper/VectorStoreMapper.java` | payload/DTO/point mapping | U | real VectorStoreHelper | §7.12 | P1 | H |
| vectorstore | `ai/infrastructure/vectorstore/manager/QdrantCollectionManager.java` | collection bootstrap | B | QdrantClient | §7.12; §8.4 1 | P1 | M |
| vectorstore | `ai/infrastructure/vectorstore/config/QdrantConfig.java` | `QdrantClient` bean | U | QdrantProperties | non-null + property wiring | P2 | L |
| vectorstore | `ai/infrastructure/vectorstore/config/QdrantProperties.java`, `dto/{SearchResult,VectorPayload,VectorPoint}.java`, `service/VectorStoreService.java` | carriers/interface | — | — | default `search` method covered via §7.12 #11 | P3 | L |

### 9.6 `ai/infrastructure/embedding`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| embedding | `ai/infrastructure/embedding/service/GeminiEmbeddingService.java` | validate, call model, token accounting, error wrap | U | EmbeddingModel | §7.11 #1–8 | P0 | H |
| embedding | `ai/infrastructure/embedding/config/GeminiEmbeddingModelConfig.java` | `EmbeddingModel` bean | U | GeminiProperties | bean non-null (no network) | P2 | L |
| embedding | `ai/infrastructure/embedding/config/GeminiProperties.java`, `dto/EmbeddingResult.java`, `service/EmbeddingService.java` | carrier/interface | — | — | none | P3 | L |

### 9.7 `admin/document` + `admin/trash`

| Package | Class | Responsibility | Type | Deps to mock | Scenarios | Prio | Cov |
|---|---|---|---|---|---|---|---|
| admin/document | `admin/document/service/AdminDocumentService.java` | delegation for admin screens | U | DocumentService | 6 delegations (`getDocuments`→`getDocumentsForAdmin`) | P1 | M |
| admin/document | `admin/document/controller/AdminDocumentController.java` | HTTP + `@PreAuthorize` | U | AdminDocumentService, DocumentMapper | MockMvc: list 200, detail 200, delete 204, restore 200 | P2 | M |
| admin/trash | `admin/trash/service/AdminTrashService.java` | merge + sort + in-memory pagination | U (I for persistence) | DocumentRepository, FolderRepository, CurrentUserService | §7.14 #1–10; §8.2 last row | P0 | H |
| admin/trash | `admin/trash/controller/AdminTrashController.java` | HTTP + `@PreAuthorize` | U | AdminTrashService, DocumentService, FolderService | MockMvc: list 200, restore doc 204, restore folder 204, hard delete 204 | P2 | M |
| admin/trash | `admin/trash/dto/TrashItemResponse.java` | carrier | — | — | none | P3 | L |

### 9.8 Aggregate

| Metric | Value |
|---|---|
| In-scope files | 94 **[V]** |
| Classes with authorable logic (U/I) | 43 |
| Data carriers / interfaces / records (no meaningful tests) | 51 |
| Tests to author (estimate, **not** a commitment) | ~170–210 test methods across ~40 classes **[A]** |
| P0 classes (must exist for the 70% gate) | 17 |

---

## 10. Critical Application Workflows

```
[1] Upload
POST /api/v1/documents/upload (multipart)
  → DocumentHelper.validateFiles / validateBatchRequest
  → DocumentAuthorizationService.requireCreate (DOCUMENT_CREATE)
  → MinioFileStorageService.store → StoredFileDto
  → FileMapper → FileEntity → FileRepository.save
  → FolderService.resolveTargetFolder(folderId | root)
  → DocumentMapper.toDocument → DocumentRepository.save
  → DocumentMapper.toDocumentVersion(#1) → DocumentVersionRepository.save
  → document.currentVersion = version → save
  → publish DocumentVersionCreatedBatchEvent(versionIds)   [AFTER_COMMIT]

[2] Processing (async, retried 3×, backoff 3s)
DocumentProcessingBatchWorker → DocumentProcessingRetryExecutor(@Retryable)
  → DocumentProcessingService.process(versionId)  @Transactional
      status=PROCESSING, step=TEXT_EXTRACTING
      FileStorageService.loadAsResource
      TextExtractionService.extract(resource, mimeType)   [Tika / POI]
      ExtractionHelper.validateExtractedText (blank → BusinessException, fail fast)
      DocumentTextRepository.save
      step=CHUNKING  → ChunkingService.chunk(extracted, extension)
                     → ChunkingStrategyFactory → Document/Spreadsheet strategy
      DocumentChunkRepository.save(*) 
      step=EMBEDDING → EmbeddingService.embed(chunk.content) per chunk
                     → ProcessingHelper.logUsage(DOCUMENT_INDEXING, ±tokens, SUCCESS/FAILED)
      VectorStoreService.upsert(points)
      status=READY, step=null
    catch BusinessException → rethrow (after REQUIRES_NEW failure mark)
    catch Exception        → DOCUMENT_PROCESSING_FAILED wrapper

[3] Semantic search
GET /api/v1/search/semantic?keyword=&topK=&documentId=
  → SearchHelper.validateSearchRequest / resolveTopK
  → CurrentUserService.requirePermission(AI_SEMANTIC_SEARCH)
  → EmbeddingService.embed(keyword)
  → VectorStoreService.search(vector, topK, documentId)   [scoreThreshold from qdrant.*]
  → DocumentRepository.findAllById(hit document ids) → keep ACTIVE
  → DocumentAuthorizationService.filterReadableDocumentIds
  → keep only hits whose documentVersionId == currentVersion.id
  → SearchMapper.toSemanticSearchResults
  → logUsage(SEMANTIC_SEARCH, SUCCESS|FAILED)

[4] Versioning
POST /api/v1/documents/{id}/versions
  → validate id → load → requireUpdate → validate file + request + status
  → store → FileEntity → DocumentHelper.createNewVersion(next = max+1)
  → document.currentVersion = newVersion (+ optional title/description/type override)
  → publish DocumentVersionCreatedEvent(versionId)
GET /{documentId}/{versionId}/download → requireDownload → version must belong to document
  → DocumentHelper.validateFileStorageMetadata → loadAsResource → mapper (Content-Disposition)

[5] Trash lifecycle
DELETE /api/v1/documents/{id}       soft delete: status=DELETED, deletedAt, deletedBy (object kept in MinIO)
GET  /api/v1/admin/trash            AdminTrashService: documents + folders merged, sorted, in-memory paged
POST /api/v1/admin/trash/documents/{id}/restore   → DocumentService.restoreDocument (ACTIVE, clears audit cols)
POST /api/v1/documents/{id}/restore               → same service, non-admin path (requireDelete)
DELETE /api/v1/admin/trash/folders/{id}           → FolderService.hardDeleteFolder (out of scope)
-- no permanent document delete exists in the codebase [V]
```

---

## 11. Scoped Coverage Configuration Recommendation

### 11.1 Tracking model

Two layers, both produced by the same run:

1. **Overall assigned scope** — the JaCoCo `check` gate (`BUNDLE` + the eight `includes`) at **LINE ≥ 0.70** (mandatory) and **BRANCH ≥ 0.50** (secondary).
2. **Per-package** — read from the HTML report (`target/site/jacoco/<package>/index.html`) or from `target/site/jacoco/jacoco.csv`. **[R]** Add a tiny `docs/coverage-tracking.md` table (or CI job step) that records the eight numbers per phase; do not add a second Maven gate per package, because a single weak package would block progress without improving it.

### 11.2 Per-package targets (gate = overall 70%)

| # | Package | Line target | Rationale |
|---|---|---|---|
| — | **Overall (all 8)** | **≥ 70%** | mandatory |
| 1 | `document` | 68–74% | contains the 5 excluded sharing lines but also the largest coverage reservoir |
| 2 | `storage` | 70–80% | tiny surface; one IT covers most of it |
| 3 | `processing/*` | 72–80% | pure algorithms, near-100% by unit tests |
| 4 | `ai/knowledge/search` | 75–85% | one service + helpers |
| 5 | `ai/infrastructure/vectorstore` | 72–82% | helper+mapper carry most lines |
| 6 | `ai/infrastructure/embedding` | 78–88% | one small service |
| 7 | `admin/document` | 80–90% | delegation only |
| 8 | `admin/trash` | 80–90% | one branchy service |

Numbers in these ranges are **targets to observe**, not separate gates. **[R]**

### 11.3 Branch coverage (secondary)

- Currently 0% enforced. Target **≥ 50%** at gate time, **≥ 55%** aspirational.
- The branch-heavy classes (in descending value): `DocumentHelper`, `DocumentChunkingStrategy`, `SpreadsheetChunkingStrategy`, `TikaStructuredContentHandler`, `VectorStoreHelper`, `DocumentAuthorizationHelper`, `DocumentProcessingService`, `AdminTrashService`, `SearchHelper`, `DocumentServiceImpl`.
- Assertions on *which* ErrorCode was thrown are what make a branch meaningful; a test that only asserts "an exception was thrown" does not verify the branch's contract. **[R]**

### 11.4 Rules of engagement (do not)

- Do not `<exclude>` a service/helper/strategy to raise the number. Only Lombok-generated code (and, if needed, pure DTOs) may be excluded, with a comment justifying it.
- Do not write accessor/getter assertions to pad coverage.
- Do not count a mocked `VectorStoreService`/`EmbeddingService` call as proof of Qdrant/Gemini integration — the MinIO/Qdrant ITs exist for that.
- Do not include `auth`, `folder`, `ai/chat`, `ai/analytics`, `ai/infrastructure/llm` in the report numerator/denominator; their tests may exist but must not influence the gate.
- Do not use `assertNotNull` as the only assertion on a mapped DTO — assert the field values that the business rule produces.

### 11.5 Gradual enforcement

| Stage | Gate | When |
|---|---|---|
| 0 | report only, no `check` — record the baseline | end of Phase 0 |
| 1 | `check` LINE ≥ 0.50, BRANCH off | end of Phase 1 |
| 2 | `check` LINE ≥ 0.60, BRANCH ≥ 0.40 | end of Phase 3 |
| 3 | `check` LINE ≥ 0.70, BRANCH ≥ 0.50 | end of Phase 5 (**final**) |

Each stage is a one-line edit to `<minimum>`; keep the `check` execution present from stage 0 so the build wiring is proven early. **[R]**

---

## 12. Phased Implementation Roadmap

### Phase 0 — Testing infrastructure (0.5 d)

**Tasks**
1. `cd backend && ./mvnw -q dependency:tree -Dscope=test` → confirm JUnit 5 / Mockito / AssertJ presence; add `spring-boot-starter-test` only if a gap exists. **[?]**
2. Add Testcontainers deps (§5.2) and `maven-failsafe-plugin` (§5.3).
3. Add `backend/lombok.config` (§4.3a), then run `./mvnw -DskipTests package` and confirm `target/classes` is unchanged in behaviour.
4. Add the scoped JaCoCo `report` config **without** `check`; run `./mvnw verify` and record the baseline CSV (§11.5 stage 0).
5. Add `support/` base classes + `TestFixtures` + `DeterministicEmbeddingService`.
6. Decide the merged-exec vs surefire-only IT execution (§4.3 caveat) with one measurement.

**Files touched:** `backend/pom.xml`, `backend/lombok.config`, `backend/src/test/java/.../support/*`, `backend/src/test/resources/` *(only if a properties file is preferred over `@DynamicPropertySource`)*.
**Deliverables:** green `./mvnw verify`, `target/site/jacoco/jacoco.csv` baseline, container smoke test (one PG IT that only loads the context-free repository).
**Acceptance:** `./mvnw verify` runs a container-backed test and writes both exec files. **Risks:** Docker unavailable on the build machine **[?]**; `jacoco:merge` behaviour.

### Phase 1 — Core unit tests (4–5 d)

**Order (by dependency depth, not by package)**
1. `DocumentHelper` → 2. `DocumentAuthorizationHelper` → 3. `DocumentAuthorizationService` → 4. `SlidingWindowSplitter` → 5. `ExtractionHelper` → 6. `TikaStructuredContentHandler` → 7. `ChunkingMapper` + `ChunkingStrategyFactory` + both strategies + `ChunkingServiceImpl` → 8. `VectorStoreHelper` → 9. `VectorStoreMapper` → 10. `QdrantVectorStoreService` (mock client) → 11. `GeminiEmbeddingService` → 12. `SearchHelper` + `SearchMapper` → 13. `SemanticSearchServiceImpl` → 14. `DocumentMapper` → 15. `MinioFileStorageService` + `FileMapper` → 16. `ProcessingHelper` + `ProcessingMapper` → 17. `DocumentServiceImpl` → 18. `AdminTrashService` + `AdminDocumentService` → 19. controllers (`FileTestController`, `DocumentController`, `SemanticSearchController`, admin controllers) → 20. remaining extractors + `AsyncConfig` + config beans.

**Deliverables:** ~35 unit test classes, no Spring context in this phase.
**Acceptance:** stage-1 gate (LINE ≥ 0.50) passes; `mvn test` under 60 s. **Risks:** over-mocking `DocumentServiceImpl` (13 collaborators) → build the fixture once in a `DocumentServiceImplTest` helper, not per test.

### Phase 2 — PostgreSQL integration (1.5–2 d)

`AbstractPostgresIT` + `TestFixtures`, then `DocumentRepositoryCustomImplIT` → `DocumentRepositoryIT` → `DocumentVersionRepositoryIT` → `DocumentAccessRepositoryIT` → chunk/text repositories → `DocumentServiceImplIT` (transaction/rollback) → trash persistence test.
**Acceptance:** every `appendFilters` branch and the ABAC matrix verified against real PostgreSQL; unique-constraint tests prove schema assumptions. **Risks:** §6.2 Flyway gap; `@DataJpaTest` slice not importing `DocumentRepositoryCustomImpl` **[?]**; `deleteBy…` derived deletes needing a transaction.

### Phase 3 — MinIO + Qdrant integration (1–1.5 d)

`AbstractMinioIT` + `MinioFileStorageServiceIT` (5 tests); `AbstractQdrantIT` + `QdrantVectorStoreServiceIT` (7 tests) + `QdrantCollectionManager` IT.
**Acceptance:** real bytes round-trip; real cosine ordering; `documentId` filter proven server-side; container start-up < 20 s each. **Risks:** unpinned upstream images; Qdrant gRPC readiness (poll `collectionExistsAsync` with timeout instead of sleeping).

### Phase 4 — Application workflows (1.5–2 d)

`DocumentUploadWorkflowIT`, `DocumentProcessingWorkflowIT`, `SemanticSearchWorkflowIT`, `DocumentVersioningWorkflowIT`, `TrashLifecycleWorkflowIT` (§8.5).
**Acceptance:** each workflow asserts DB rows **and** the external side effect (MinIO object / Qdrant point). **Risks:** context startup cost (3 initializers); share test containers across the five IT classes; never let the async worker call a real Gemini (double-mock).

### Phase 5 — Coverage optimisation (0.5–1 d)

Open `target/site/jacoco/index.html` → per-package → per-class; list uncovered lines/branches; add only tests that assert a *behaviour* that is missing; re-measure; flip the gate to 0.70/0.50; document deliberate gaps (§2.3, §8.5 async chain).
**Acceptance:** `./mvnw verify` fails below 70% line coverage and passes at the achieved level; the uncovered list contains **only** justified items.

### Dependency-adjusted ordering note

The roadmap deliberately runs **all unit tests before any integration test** even though `DocumentRepositoryCustomImplIT` would cover a P0 class: `DocumentRepositoryCustomImplTest` already guards its most dangerous branch (null-scope fail-fast) **[V]**, so the residual risk of deferring the JPQL IT by one phase is low, while the payoff (fast feedback + the stage-1 gate) is high.

---

## 13. File-Level Implementation Checklist

### 13.1 Configuration (Phase 0)

- [ ] `backend/pom.xml` — add Testcontainers (`junit-jupiter`, `postgresql`), `maven-failsafe-plugin`, scoped JaCoCo `report`/`merge`/`check` (§4.3)
- [ ] `backend/lombok.config` — `config.stopBubbling = true`, `lombok.addLombokGeneratedAnnotation = true` (§4.3a)
- [ ] *(optional)* `backend/src/test/resources/application-test.properties` — only if `@DynamicPropertySource` proves insufficient

### 13.2 Test support (Phase 0)

- [ ] `backend/src/test/java/com/enterprise/aiassistant/backend/support/AbstractPostgresIT.java`
- [ ] `.../support/AbstractMinioIT.java`
- [ ] `.../support/AbstractQdrantIT.java`
- [ ] `.../support/TestFixtures.java`
- [ ] `.../support/DeterministicEmbeddingService.java`
- [ ] `.../support/TestMultipartFiles.java` (valid pdf/docx/txt/xlsx, oversize, empty, bad mime)

### 13.3 Unit tests (Phase 1)

- [ ] `document/helper/DocumentHelperTest.java`
- [ ] `document/helper/DocumentAuthorizationHelperTest.java` *(extend the matrix in `authorization/AuthorizationPolicyTest.java` or keep separate — separate is cleaner for the coverage report)*
- [ ] `document/service/DocumentAuthorizationServiceTest.java` *(exists — add the 8 missing cases)*
- [ ] `document/service/DocumentServiceImplTest.java`
- [ ] `document/mapper/DocumentMapperTest.java`
- [ ] `document/controller/DocumentControllerTest.java`
- [ ] `storage/service/MinioFileStorageServiceTest.java`
- [ ] `storage/mapper/FileMapperTest.java`
- [ ] `storage/mapper/StoredFileMapperTest.java`
- [ ] `storage/controller/FileTestControllerTest.java`
- [ ] `storage/config/MinioConfigTest.java`, `storage/config/FileUploadPropertiesTest.java`
- [ ] `processing/chunking/support/SlidingWindowSplitterTest.java`
- [ ] `processing/chunking/strategy/DocumentChunkingStrategyTest.java`
- [ ] `processing/chunking/strategy/SpreadsheetChunkingStrategyTest.java`
- [ ] `processing/chunking/strategy/ChunkingStrategyFactoryTest.java`
- [ ] `processing/chunking/service/ChunkingServiceImplTest.java`
- [ ] `processing/chunking/mapper/ChunkingMapperTest.java`
- [ ] `processing/extraction/helper/ExtractionHelperTest.java`
- [ ] `processing/extraction/extractor/tika/TikaStructuredContentHandlerTest.java`
- [ ] `processing/extraction/extractor/tika/TikaContentExtractorTest.java`
- [ ] `processing/extraction/extractor/TextExtractorFactoryTest.java`
- [ ] `processing/extraction/extractor/PdfTextExtractorTest.java`
- [ ] `processing/extraction/extractor/DocxTextExtractorTest.java`
- [ ] `processing/extraction/extractor/TxtTextExtractorTest.java`
- [ ] `processing/extraction/extractor/ExcelTextExtractorTest.java`
- [ ] `processing/extraction/mapper/ExtractionMapperTest.java`
- [ ] `processing/extraction/service/TextExtractionServiceImplTest.java`
- [ ] `processing/orchestration/service/DocumentProcessingServiceTest.java`
- [ ] `processing/orchestration/helper/ProcessingHelperTest.java`
- [ ] `processing/orchestration/mapper/ProcessingMapperTest.java`
- [ ] `processing/orchestration/worker/DocumentProcessingWorkerTest.java`
- [ ] `processing/orchestration/worker/DocumentProcessingBatchWorkerTest.java`
- [ ] `processing/orchestration/worker/DocumentProcessingRetryExecutorTest.java`
- [ ] `processing/config/AsyncConfigTest.java`
- [ ] `ai/infrastructure/embedding/service/GeminiEmbeddingServiceTest.java`
- [ ] `ai/infrastructure/embedding/config/GeminiEmbeddingModelConfigTest.java`
- [ ] `ai/infrastructure/vectorstore/helper/VectorStoreHelperTest.java`
- [ ] `ai/infrastructure/vectorstore/mapper/VectorStoreMapperTest.java`
- [ ] `ai/infrastructure/vectorstore/service/QdrantVectorStoreServiceTest.java`
- [ ] `ai/infrastructure/vectorstore/manager/QdrantCollectionManagerTest.java`
- [ ] `ai/infrastructure/vectorstore/config/QdrantConfigTest.java`
- [ ] `ai/knowledge/search/helper/SearchHelperTest.java`
- [ ] `ai/knowledge/search/mapper/SearchMapperTest.java`
- [ ] `ai/knowledge/search/service/SemanticSearchServiceImplTest.java`
- [ ] `ai/knowledge/search/controller/SemanticSearchControllerTest.java`
- [ ] `admin/document/service/AdminDocumentServiceTest.java`
- [ ] `admin/document/controller/AdminDocumentControllerTest.java`
- [ ] `admin/trash/service/AdminTrashServiceTest.java`
- [ ] `admin/trash/controller/AdminTrashControllerTest.java`

### 13.4 Integration tests (Phases 2–4)

- [ ] `document/repository/DocumentRepositoryCustomImplIT.java`
- [ ] `document/repository/DocumentRepositoryIT.java`
- [ ] `document/repository/DocumentVersionRepositoryIT.java`
- [ ] `document/repository/DocumentAccessRepositoryIT.java`
- [ ] `document/repository/DocumentChunkRepositoryIT.java`
- [ ] `document/repository/DocumentTextRepositoryIT.java`
- [ ] `document/service/DocumentServiceImplIT.java`
- [ ] `admin/trash/AdminTrashServiceIT.java`
- [ ] `storage/service/MinioFileStorageServiceIT.java`
- [ ] `ai/infrastructure/vectorstore/service/QdrantVectorStoreServiceIT.java`
- [ ] `document/DocumentUploadWorkflowIT.java`
- [ ] `processing/DocumentProcessingWorkflowIT.java`
- [ ] `ai/knowledge/search/SemanticSearchWorkflowIT.java`
- [ ] `document/DocumentVersioningWorkflowIT.java`
- [ ] `admin/trash/TrashLifecycleWorkflowIT.java`

### 13.5 Existing tests — keep / adjust

- [ ] `BackendApplicationTests.java` — **[R]** either delete it or convert it to a container-backed smoke test; today it will fail on a fresh machine (needs PostgreSQL with a non-Flyway schema, Qdrant, MinIO at startup). Do not let it become the accidental gate.
- [ ] `DocumentRepositoryCustomImplTest.java` — keep (fast unit guard for `appendAccessScope`).
- [ ] `DocumentAuthorizationServiceTest.java` — keep and extend.
- [ ] `AuthorizationPolicyTest.java` — keep; it touches out-of-scope packages for the *inputs* but tests in-scope helpers; note in its javadoc that it is not part of the scoped denominator (test classes never are).

---

## 14. Risks and Unresolved Questions

### 14.1 Verified defects / gaps that shape the plan

| # | Finding | Impact on testing |
|---|---|---|
| 1 | **Flyway cannot build the schema.** `V1` creates `users`/`refresh_tokens`; `V2` alters `documents`, `folders`, `ai_conversations`, `ai_usage_logs` that no migration creates (its own header says schema comes from `ddl-auto=update`). **[V]** | Integration tests must use `ddl-auto=create-drop` + `flyway.enabled=false`. A production deploy on a fresh DB with the documented `ddl-auto=validate` + `flyway.enabled=true` would fail. |
| 2 | **`application.properties.example` contradicts reality** (`ddl-auto=validate`, `flyway.enabled=true`) while V2 asserts `ddl-auto=update`, `flyway.enabled=false`. **[V]** | Actual dev values live in an untracked `.env` **[V]** — treat as **[?]**; do not assume. |
| 3 | **No Lombok filter.** ~40 DTO/entity classes inflate the denominator. **[V]** | Add `lombok.config` first, or 70% is unreachable without junk tests. |
| 4 | **`FileStorageService` has no delete.** Only `store` + `loadAsResource`. **[V]** | "File deletion" tests are impossible; MinIO objects are orphaned forever after a document is deleted (soft delete). |
| 5 | **No permanent document deletion** anywhere (only `FolderService#hardDeleteFolder`). **[V]** | The brief's "Permanent deletion" trash scenario cannot be implemented for documents. |
| 6 | **No bucket bootstrap.** `store()` assumes `${minio.bucket}` exists. **[V]** | Every MinIO test (and a fresh dev environment) must create the bucket; consider flagging as a production bug. |
| 7 | **`DocumentServiceImpl#getDocumentDetail` dereferences `document.getCurrentVersion()` without a null check.** **[V]** | A document whose current version was never set throws `NullPointerException` instead of `DOCUMENT_HAS_NO_CURRENT_VERSION` (an unused `ErrorCode`). Test #32 in §7.5 pins the current behaviour; the fix is a production change and out of scope here. |
| 8 | **`DocumentHelper.fileUploadProperties` is injected and never read**; the 50 MB limit is a hardcoded constant; `file.upload.max-size=500MB` in the example has no effect. **[V]** | Cover `FileUploadProperties` as a binder test only; note the dead dependency. |
| 9 | **`QdrantProperties.apiKey` / `restPort` unused** by `QdrantConfig`. **[V]** | Dead configuration; ignore in tests. |
| 10 | **`FileTestController`** exposes an unauthenticated-looking `POST ${api.prefix}/test/storage/upload` debug endpoint inside `storage/`. **[V]** (security config not audited — out of scope) | One MockMvc test to cover the lines; flag for removal in a security review. |
| 11 | **`QdrantCollectionManager` runs on `ApplicationReadyEvent`**, `RbacInitializer` executes `ALTER TABLE … DROP CONSTRAINT` at startup, `RootFolderInitializer` needs `folders`. **[V]** | Any `@SpringBootTest` needs PostgreSQL **and** Qdrant, or mocked `QdrantClient`/`FolderService`. Prefer slice tests. |
| 12 | **`DocumentProcessingService` re-`save`s without clearing previous chunks** for a re-processing attempt (no `documentChunkRepository.deleteByDocumentVersionId` call in `process`) — **[V]** code reading, not executed | Do not write a "reprocess the same version" test that assumes idempotency; assert only retry-after-failure (transaction rollback makes it safe). Flag as a risk. |
| 13 | **`@Retryable` sits on a different bean than the `@Transactional` method** (`DocumentProcessingRetryExecutor` → `DocumentProcessingService`), which is the correct arrangement. **[V]** | Do not test retries by self-invocation; the annotation only works through the proxy. |

### 14.2 Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Docker not available on the developer/CI machine | Medium **[?]** | Phase 0 smoke test first; if unavailable, tag ITs `@Tag("docker")` and let `check` run on unit data + document the gap |
| Lombok exclusion rejected by reviewers as "inflating coverage" | Medium | Justify in `pom.xml` comment + this document §4.4; the alternative (getter assertions) is worse |
| Unpinned upstream images (`qdrant:latest`, `minio:latest`) drift and break ITs | Medium | Pin tags in the test base classes; **do not** edit `docker-compose.yml` |
| `DocumentServiceImplTest` becomes an unmaintainable 35-test mock wall | High | One shared fixture builder; assert one behaviour per test; group by method with `@Nested` |
| Over-mocking hides a real regression (e.g. mapper/entity mismatch) | Medium | The 5 workflow ITs are the counterweight; keep at least one assertion on persisted state per workflow |
| Context startup cost (3 initializers + 2 containers) makes `verify` slow | Medium | Singleton containers; ≤5 `@SpringBootTest` classes; everything else sliced or plain unit |
| JaCoCo `check` fires before the harness is ready and blocks other work | Low | Stage the `minimum` (§11.5) |
| Scope creep into `folder`/`auth`/`ai/chat` while writing fixtures | Medium | `TestFixtures` only builds entities that in-scope code needs; folder/auth are mocked |
| Async event chain makes "upload" ITs flaky | High | Mock `EmbeddingService` **and** `VectorStoreService` in upload ITs; cover processing by direct invocation |

### 14.3 Unresolved questions (need a human or a command)

1. **Are JUnit 5 / Mockito / AssertJ actually on the test classpath?** The POM declares only the Boot `*-test` starters; `backend/target` does not exist, so nothing is proven. → `./mvnw dependency:tree -Dscope=test`. **[?]**
2. **What are the real dev/prod values** of `spring.jpa.hibernate.ddl-auto` / `spring.flyway.enabled`? They live in the untracked `.env` and `backend/.env`. **[?]**
3. **Which Boot-4 import paths** apply to `@DataJpaTest` / `@WebMvcTest` in this exact 4.1.0 release? `WebMvcTest` is documented at `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`; `DataJpaTest` is expected under `org.springframework.boot.data.jpa.test.autoconfigure` but was not verified. **[?]**
4. **Which `SettableApiFuture` factory** does `io.qdrant:client 1.15.0` expose for stubbing async calls? **[?]**
5. **Is the MinIO bucket expected to be created manually** in every environment (no code does it)? **[?]**
6. **Is `gemini-embedding-001` configured to 3072 dimensions** in all environments (test Qdrant collections must match)? The example sets `qdrant.vector-size=3072`, but the model's dimension is an assumption. **[A]/[?]**
7. **What is the intended CI entry point?** No workflow file was discovered by file search (the `.github` directory exists but no files were returned), and `.github` contents were not otherwise inspected. **[?]**
8. **Does the team want `BackendApplicationTests` kept?** It currently cannot pass without a fully provisioned PostgreSQL + Qdrant + MinIO. **[?]**
9. **Is `FileTestController` intentional?** If it is meant to be deleted, exclude it from scope instead of testing it. **[?]**
10. **Are QueryDSL `Q*` classes generated** during a normal build? The APT processor is configured, yet no `querydsl-jpa` repository implementations exist in scope — confirm no generated sources need excluding. **[?]**

---

## 15. Traceability to the acceptance criteria

| Criterion | Where satisfied |
|---|---|
| Exactly eight packages | §2.1, `includes` in §4.2/§4.3 |
| ≥ 70% JaCoCo line coverage in scope | §4.3 (`check`), §11 |
| Both unit and integration tests | §7 (43 classes) / §8 (6 PostgreSQL, 1 MinIO, 1 Qdrant, 5 workflows) |
| PostgreSQL / MinIO / Qdrant isolated | §6.1–6.4, §8.1 |
| No real Gemini dependency | §6.5, §7.11 (mock), §8 (`DeterministicEmbeddingService`) |
| Concrete class-level scenarios | §7.1–7.14, §9 matrix |
| Actionable implementation order | §12 (Phase 0→5), §13 checklist |
| No invented code, no unnecessary dependencies, no repeated exploration | Every claim tagged **[V]**, unknowns listed in §14.3; dependencies limited to Testcontainers (+Awaitility if needed) |
