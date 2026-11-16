package com.enterprise.aiassistant.backend.document;

import com.enterprise.aiassistant.backend.admin.trash.dto.TrashItemResponse;
import com.enterprise.aiassistant.backend.admin.trash.service.AdminTrashService;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.response.SemanticSearchResult;
import com.enterprise.aiassistant.backend.ai.knowledge.search.service.SemanticSearchService;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentBatchUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDetailResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import com.enterprise.aiassistant.backend.support.AbstractFullStackIT;
import com.enterprise.aiassistant.backend.support.TestFixtures;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end business workflows over the real stack: PostgreSQL, MinIO, Qdrant and the real
 * processing pipeline with the deterministic embedding service (no Gemini calls).
 *
 * <p>Covers upload, asynchronous processing, semantic search, versioning and the trash
 * lifecycle. Document sharing is out of scope by requirement.
 */
class DocumentWorkflowIT extends AbstractFullStackIT {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AdminTrashService adminTrashService;

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Emulates the request-scoped Hibernate session that Open-Session-in-View keeps open for
     * HTTP requests: {@code getDocumentDetail}/{@code downloadSelectedVersion} touch
     * lazily-loaded {@code DocumentVersion.file}, which fails outside a session (recorded as a
     * production issue in docs/TEST_COVERAGE_REPORT.md).
     */
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private User actingUser;

    @BeforeEach
    void authenticate() {

        Department department = departmentRepository.save(
                TestFixtures.department("WF-" + UUID.randomUUID()));

        actingUser = userRepository.save(TestFixtures.user(
                "wf-" + UUID.randomUUID(), Role.ADMIN, department));

        UserPrincipal principal = UserPrincipal.from(actingUser, EnumSet.allOf(Permission.class));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===================== upload + processing =====================

    @Test
    void shouldUploadFilePersistMetadataAndProcessItAsynchronously() throws Exception {

        String content = "Enterprise contract about quarterly revenue and compliance.";

        DocumentUploadResponse upload = upload("Alpha contract", content, DocumentType.CONTRACT);

        assertThat(upload.getDocumentId()).isNotNull();
        assertThat(upload.getCurrentVersionId()).isNotNull();
        assertThat(upload.getVersionNumber()).isEqualTo(1);
        assertThat(upload.getFilename()).endsWith(".txt");

        awaitVersionProcessed(upload.getCurrentVersionId());

        String persistedText = jdbcTemplate.queryForObject(
                "SELECT content FROM document_texts WHERE document_version_id = ?",
                String.class,
                upload.getCurrentVersionId());

        assertThat(persistedText).contains("quarterly revenue");

        Long chunkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_chunks WHERE document_version_id = ?",
                Long.class,
                upload.getCurrentVersionId());

        assertThat(chunkCount).isPositive();

        Map<String, Object> file = jdbcTemplate.queryForMap(
                "SELECT f.bucket_name, f.object_key FROM files f "
                        + "JOIN document_versions v ON v.file_id = f.id WHERE v.id = ?",
                upload.getCurrentVersionId());

        Resource stored = fileStorageService.loadAsResource(
                (String) file.get("bucket_name"), (String) file.get("object_key"));

        try (InputStream inputStream = stored.getInputStream()) {
            assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(content);
        }
    }

    @Test
    void shouldStoreEveryUploadedFileUnderItsOwnObjectKey() {

        DocumentUploadResponse first = upload("First", "first body", DocumentType.OTHER);
        DocumentUploadResponse second = upload("Second", "second body", DocumentType.OTHER);

        Long distinctKeys = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT f.object_key) FROM files f JOIN document_versions v "
                        + "ON v.file_id = f.id WHERE v.id IN (?, ?)",
                Long.class,
                first.getCurrentVersionId(),
                second.getCurrentVersionId());

        assertThat(distinctKeys).isEqualTo(2);
    }

    // ===================== versioning =====================

    @Test
    void shouldCreateNewVersionAndSwitchCurrentVersion() throws Exception {

        DocumentUploadResponse upload = upload("Versioned", "version one body", DocumentType.REPORT);

        UploadNewVersionRequest request = new UploadNewVersionRequest();
        request.setChangeNote("updated numbers");
        request.setTitle("Versioned (updated)");

        UploadNewVersionResponse newVersion = documentService.uploadNewVersion(
                upload.getDocumentId(),
                txtFile("v2.txt", "version two body"),
                request
        );

        assertThat(newVersion.getVersionNumber()).isEqualTo(2);
        assertThat(newVersion.getChangeNote()).isEqualTo("updated numbers");
        assertThat(newVersion.getCurrentVersionId()).isNotEqualTo(upload.getCurrentVersionId());

        DocumentDetailResponse detail = readOnly(() ->
                documentService.getDocumentDetail(upload.getDocumentId()));

        assertThat(detail.documentInfo().title()).isEqualTo("Versioned (updated)");
        assertThat(detail.currentVersion().versionId()).isEqualTo(newVersion.getCurrentVersionId());
        assertThat(detail.versionHistory())
                .extracting(DocumentDetailResponse.VersionHistoryItem::versionNumber)
                .containsExactly(2, 1);

        DocumentDownloadResource previous = readOnly(() -> documentService.downloadSelectedVersion(
                upload.getDocumentId(), upload.getCurrentVersionId()));

        try (InputStream inputStream = previous.getResource().getInputStream()) {
            assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("version one body");
        }
    }

    @Test
    void shouldRejectNewVersionForDeletedDocument() {

        DocumentUploadResponse upload = upload("Deleted target", "body", DocumentType.OTHER);

        documentService.deleteDocument(upload.getDocumentId());

        UploadNewVersionRequest request = new UploadNewVersionRequest();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentService.uploadNewVersion(
                        upload.getDocumentId(), txtFile("v2.txt", "body two"), request))
                .isInstanceOf(com.enterprise.aiassistant.backend.common.exception.business_exception
                        .DocumentException.class);
    }

    // ===================== metadata =====================

    @Test
    void shouldUpdateMetadataWithoutTouchingVersion() {

        DocumentUploadResponse upload = upload("Original title", "body", DocumentType.OTHER);

        DocumentUpdateMetadataRequest request = new DocumentUpdateMetadataRequest();
        request.setDescription("updated description");

        documentService.updateDocumentMetadata(upload.getDocumentId(), request);

        DocumentDetailResponse detail = readOnly(() ->
                documentService.getDocumentDetail(upload.getDocumentId()));

        assertThat(detail.documentInfo().title()).isEqualTo("Original title");
        assertThat(detail.documentInfo().description()).isEqualTo("updated description");
        assertThat(detail.currentVersion().versionId()).isEqualTo(upload.getCurrentVersionId());
    }

    // ===================== semantic search =====================

    @Test
    void shouldFindProcessedDocumentThroughSemanticSearch() {

        String content = "Semantic marker: supplier onboarding checklist and risk assessment.";

        DocumentUploadResponse upload = upload("Semantic document", content, DocumentType.REPORT);

        awaitVersionProcessed(upload.getCurrentVersionId());

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(content);
        request.setTopK(5);

        List<SemanticSearchResult> results = semanticSearchService.search(request);

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> {
            assertThat(result.getDocumentId()).isEqualTo(upload.getDocumentId());
            assertThat(result.getVersionId()).isEqualTo(upload.getCurrentVersionId());
            assertThat(result.getContent()).isNotBlank();
            assertThat(result.getScore()).isNotNull();
        });
    }

    @Test
    void shouldExcludeDeletedDocumentFromSemanticSearch() {

        String content = "Deleted marker: obsolete pricing appendix.";

        DocumentUploadResponse upload = upload("Obsolete", content, DocumentType.OTHER);

        awaitVersionProcessed(upload.getCurrentVersionId());

        documentService.deleteDocument(upload.getDocumentId());

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(content);
        request.setTopK(5);

        assertThat(semanticSearchService.search(request))
                .noneSatisfy(result -> assertThat(result.getDocumentId())
                        .isEqualTo(upload.getDocumentId()));
    }

    // ===================== trash lifecycle =====================

    @Test
    void shouldSoftDeleteListInTrashAndRestore() {

        DocumentUploadResponse upload = upload("Trash candidate", "trash body", DocumentType.OTHER);

        documentService.deleteDocument(upload.getDocumentId());

        Page<TrashItemResponse> trash = adminTrashService.getTrashItems(
                "Trash candidate", "DOCUMENT", PageRequest.of(0, 20));

        assertThat(trash.getContent())
                .extracting(TrashItemResponse::getItemId)
                .contains(upload.getDocumentId());
        assertThat(trash.getContent())
                .filteredOn(item -> item.getItemId().equals(upload.getDocumentId()))
                .allSatisfy(item -> {
                    assertThat(item.getType()).isEqualTo("DOCUMENT");
                    assertThat(item.getName()).isEqualTo("Trash candidate");
                    assertThat(item.getDeletedAt()).isNotNull();
                    assertThat(item.getDeletedByName()).isNotNull();
                });

        documentService.restoreDocument(upload.getDocumentId());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM documents WHERE id = ?", String.class, upload.getDocumentId());

        assertThat(status).isEqualTo("ACTIVE");

        assertThat(adminTrashService.getTrashItems(
                "Trash candidate", "DOCUMENT", PageRequest.of(0, 20)).getContent())
                .extracting(TrashItemResponse::getItemId)
                .doesNotContain(upload.getDocumentId());
    }

    @Test
    void shouldKeepStoredObjectAfterSoftDelete() {

        DocumentUploadResponse upload = upload("Kept object", "kept body", DocumentType.OTHER);

        documentService.deleteDocument(upload.getDocumentId());

        Long objects = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM files f JOIN document_versions v ON v.file_id = f.id "
                        + "WHERE v.id = ?",
                Long.class,
                upload.getCurrentVersionId());

        assertThat(objects).isEqualTo(1);
    }

    // ===================== helpers =====================

    private DocumentUploadResponse upload(String title, String content, DocumentType type) {

        List<MultipartFile> files = List.of(txtFile(title + ".txt", content));
        DocumentUploadRequest metadata = new DocumentUploadRequest();
        metadata.setTitle(title);
        metadata.setDescription("description of " + title);
        metadata.setDocumentType(type);

        DocumentBatchUploadRequest batch = new DocumentBatchUploadRequest();
        ReflectionTestUtils.setField(batch, "documents", List.of(metadata));

        return documentService.upload(files, batch).get(0);
    }

    private static MultipartFile txtFile(String name, String content) {
        return new MockMultipartFile(
                "files", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private void awaitVersionProcessed(Long versionId) {

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(currentVersionStatus(versionId))
                        .isEqualTo("READY"));
    }

    private <T> T readOnly(java.util.function.Supplier<T> action) {

        org.springframework.transaction.support.TransactionTemplate template =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        template.setReadOnly(true);

        return template.execute(status -> action.get());
    }

    private String currentVersionStatus(Long versionId) {

        return jdbcTemplate.queryForObject(
                "SELECT status FROM document_versions WHERE id = ?", String.class, versionId);
    }
}
