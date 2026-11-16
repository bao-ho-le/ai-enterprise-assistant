# Test coverage report

Generated from `target/site/jacoco/jacoco.csv` after `mvn clean verify` (merged Surefire +
Failsafe execution data). Date of the measured run: 2026-11-16.

## 1. Summary

| Metric | Value | Required |
|--------|-------|----------|
| Total test classes | 58 (46 unit + 12 integration) | - |
| Total test methods | 446 | - |
| Unit test methods (Surefire) | 374 | - |
| Integration test methods (Failsafe) | 72 | - |
| Failures / errors / skipped | 0 / 0 / 0 | 0 |
| **Line coverage (8 packages)** | **86.6%** (1388 / 1603) | ≥ 70% |
| **Branch coverage (8 packages)** | **86.1%** (464 / 539) | ≥ 50% |
| Methods covered | 254 / 312 | - |
| Build result | `BUILD SUCCESS` (JaCoCo check executed in `verify`) | pass |

## 2. Package-level coverage

| Package | Classes | Line | Branch |
|---------|---------|------|--------|
| `document` | 26 (21 with covered lines) | 75.7% (563/744) | 77.0% (217/282) |
| `storage` | 8 (7) | 71.8% (61/85) | 80.0% (8/10) |
| `processing` | 26 (26) | 98.0% (444/453) | 94.8% (128/135) |
| `ai.knowledge.search` | 4 (4) | 100.0% (77/77) | 100.0% (30/30) |
| `ai.infrastructure.vectorstore` | 7 (6) | 99.4% (156/157) | 100.0% (56/56) |
| `ai.infrastructure.embedding` | 3 (3) | 100.0% (27/27) | 100.0% (6/6) |
| `admin.document` | 2 (2) | 100.0% (15/15) | n/a (no branches) |
| `admin.trash` | 2 (2) | 100.0% (45/45) | 95.0% (19/20) |
| **Total** | 78 | **86.6%** | **86.1%** |

## 3. Uncovered classes and code

| Class | Lines missed | Why |
|-------|--------------|-----|
| `document.service.DocumentServiceImpl` | 35 | `shareDocument`, `revokeDocumentAccess`, `getDocumentShares` bodies - sharing is excluded by requirement |
| `document.entity.QDocument`, `QDocumentVersion`, `QDocumentChunk`, `QDocumentText`, `QDocumentAccess`, `storage.entity.QFileEntity` | 104 total | QueryDSL annotation processor output (compiler-generated `Q*` metamodel); not hand-written code |
| `document.mapper.DocumentMapper` | 23 | `toDocumentAccess` / `toShareResponse` (sharing only) |
| `document.helper.DocumentHelper` | 14 | `validateShareRequest`, `validateTargetUserId`, `validateShareTarget` (sharing only) |
| `document.controller.DocumentController` | 4 | `POST/GET/DELETE .../shares` handlers (sharing only) |
| `document.service.DocumentAuthorizationService` | 1 | branch inside `requireManageAccess` used only by the sharing flow |
| `processing.chunking.strategy.DocumentChunkingStrategy` | 3 | defensive `breadcrumb == null` path when a page starts with a paragraph after a long heading-only page |
| `processing.chunking.strategy.SpreadsheetChunkingStrategy` | 2 | oversized-row flush path when the buffer holds only the header |
| `ai.infrastructure.vectorstore.service.VectorStoreService` | 1 | default `search(vector, limit, null)` bridge method (only the 3-arg version is exercised) |

Machine-generated classes (`Q*`) are the only large uncovered block; any remaining gap is either
excluded sharing behaviour or one-line defensive branches.

## 4. Tests by area

| Area | Unit tests | Integration tests |
|------|-----------|-------------------|
| Document (helper, authorization, mapper, service, controller) | `DocumentHelperTest`, `DocumentAuthorizationHelperTest`, `DocumentAuthorizationServiceTest`, `DocumentMapperTest`, `DocumentServiceImplTest`, `DocumentControllerTest` | `DocumentRepositoryCustomImplIT`, `DocumentRepositoryIT`, `DocumentVersionRepositoryIT`, `DocumentAccessRepositoryIT`, `DocumentChunkRepositoryIT`, `DocumentTextRepositoryIT`, `DocumentWorkflowIT` |
| Storage | `MinioFileStorageServiceTest`, `FileMapperTest`, `StoredFileMapperTest`, `MinioConfigTest`, `FileTestControllerTest` | `MinioFileStorageServiceIT` |
| Processing | `SlidingWindowSplitterTest`, `ChunkingMapperTest`, `DocumentChunkingStrategyTest`, `SpreadsheetChunkingStrategyTest`, `ChunkingStrategyFactoryTest`, `ChunkingServiceImplTest`, `ExtractionHelperTest`, `TikaStructuredContentHandlerTest`, `TikaContentExtractorTest`, `TextExtractorDelegationTest`, `ExcelTextExtractorTest`, `TextExtractorFactoryTest`, `TextExtractionServiceImplTest`, `ProcessingHelperTest`, `ProcessingMapperTest`, `DocumentProcessingServiceTest`, `ProcessingWorkerTest`, `AsyncConfigTest` | covered through `DocumentWorkflowIT` (real extraction → chunking → embedding → Qdrant) |
| Vector store / embedding | `VectorStoreHelperTest`, `VectorStoreMapperTest`, `QdrantVectorStoreServiceTest`, `QdrantCollectionManagerTest`, `QdrantConfigTest`, `GeminiEmbeddingServiceTest` | `QdrantVectorStoreServiceIT` |
| Semantic search | `SearchHelperTest`, `SearchMapperTest`, `SemanticSearchServiceImplTest`, `SemanticSearchControllerTest` | `DocumentWorkflowIT` (search + deleted-document filtering) |
| Admin | `AdminDocumentServiceTest`, `AdminTrashServiceTest`, `AdminDocumentControllerTest`, `AdminTrashControllerTest` | `DocumentWorkflowIT` (trash listing + restore) |
| Application | - | `BackendApplicationIT` (full context against real infrastructure) |

## 5. Remaining limitations

1. **Flyway is not exercised.** Tests use `spring.flyway.enabled=false` and
   `ddl-auto=create-drop`, because the repository's migrations do not create the
   document/version/chunk/text/file/access tables. The suite therefore proves JPA mapping and SQL
   correctness, not migration completeness.
2. **Sharing is untested by design.** The sharing code paths listed in section 3 have no business
   tests.
3. **Integrations tests need Docker**, take several minutes, and pull three images. The unit suite
   is Docker-free.
4. **Asynchronous processing** is verified through Awaitility polling of the real database state
   (bounded at 120 s), not through internal executor hooks.
5. **Qdrant behaviour** is verified through the real client; the tests use a dedicated 8-dimensional
   collection, so production dimensions (e.g. 3072) are not exercised.
6. **Gemini** is never called: unit tests mock `EmbeddingModel`, integration tests use
   `DeterministicEmbeddingService`. Consequently the real provider contract (HTTP errors, quota,
   token accounting) is unverified.

## 6. Production issues discovered (recorded, not fixed)

These were found while writing the tests. No production behaviour was changed.

1. **Flyway migrations are incomplete.** `V1`/`V2` do not create `documents`,
   `document_versions`, `document_chunks`, `document_texts`, `files`, `document_accesses`
   (tests must use `ddl-auto=create-drop`). Evidence: `AbstractPostgresIT` / `PostgresContainerSupport`
   comments; app cannot start against an empty migrated schema without Hibernate DDL.
2. **No MinIO bucket creation.** `MinioFileStorageService.store` assumes the bucket exists; nothing
   in the application creates it. Uploads fail with `NoSuchBucket` on a fresh MinIO
   (`MinioContainerSupport.ensureBucket` exists only because tests must create it).
3. **`DocumentHelper.validateContentType` throws `NullPointerException` for a null content type.**
   `ALLOWED_TYPES` is an immutable `Set.of(...)` and `contains(null)` throws before
   `ErrorCode.UNSUPPORTED_FILE_TYPE` can be raised. Pinned by
   `DocumentHelperTest.FileValidation#shouldFailWithNullPointerExceptionWhenContentTypeIsMissing`.
4. **`DocumentServiceImpl.getDocumentDetail` / `downloadSelectedVersion` depend on
   Open-Session-in-View.** They dereference the lazy `DocumentVersion.file` association outside any
   `@Transactional` boundary. Works in HTTP requests (OSIV filter), throws
   `LazyInitializationException` for non-web callers. `DocumentWorkflowIT` wraps these calls in a
   read-only `TransactionTemplate` to emulate the request-scoped session.
5. **Retry of a failed processing run can violate the chunk unique constraint.**
   `DocumentProcessingService.process` inserts `document_texts` and `document_chunks` without
   deleting previous attempts; `uk_document_chunks_version_chunk_index` makes attempt 2 fail with a
   constraint violation, which `DocumentProcessingRetryExecutor` then retries twice more.
   Evidence: `DocumentProcessingServiceTest` + `DocumentChunkRepositoryIT` (unique constraint test).
6. **Hard-coded upload limit.** `DocumentHelper.MAX_FILE_SIZE` is `50 * 1024 * 1024` while the
   injected `FileUploadProperties` is never read, so `file.upload.max-size` has no effect.
7. **No file deletion capability.** `FileStorageService` exposes only `store` and
   `loadAsResource`; soft-deleting a document leaves the MinIO object forever
   (`DocumentWorkflowIT.shouldKeepStoredObjectAfterSoftDelete` documents that behaviour). There is
   also no permanent document deletion endpoint - only folders can be hard-deleted.
8. **Unused Qdrant configuration.** `QdrantProperties.apiKey` and `restPort` are never consumed by
   `QdrantConfig`, so API-key/TLS-protected Qdrant deployments cannot be configured.
9. **`UploadNewVersionResponse.totalVersions` is never populated.** The field exists in the DTO but
   no mapper sets it, so API clients always receive `0`.
10. **Debug storage endpoint is exposed.** `FileTestController`
    (`POST ${api.prefix}/test/storage/upload`) is a development helper still registered as a
    `@RestController` and is covered by `FileTestControllerTest`.

## 7. How the numbers were produced

```bash
cd backend
mvn clean verify        # Surefire + Failsafe + JaCoCo merge/report/check
```

Artifacts:

* `target/site/jacoco/index.html` (HTML)
* `target/site/jacoco/jacoco.xml` (XML)
* `target/site/jacoco/jacoco.csv` (CSV - source of the tables above)
* `target/jacoco.exec`, `target/jacoco-it.exec`, `target/jacoco-merged.exec`

The `check-scoped-coverage` execution enforces 70% line / 50% branch on exactly the eight scoped
packages, using the merged execution data.
