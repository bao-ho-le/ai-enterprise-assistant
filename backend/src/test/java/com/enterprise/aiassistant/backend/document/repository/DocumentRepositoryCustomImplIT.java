package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.support.AbstractPostgresIT;
import com.enterprise.aiassistant.backend.support.TestFixtures;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the hand-written JPQL of {@link DocumentRepositoryCustomImpl} against real PostgreSQL,
 * including the ABAC scope pushed down into the query and the constructor-projection mapping.
 */
class DocumentRepositoryCustomImplIT extends AbstractPostgresIT {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository versionRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    private Department itDepartment;

    private Department hrDepartment;

    private User alice;

    private User bob;

    private Folder rootFolder;

    @BeforeEach
    void setUp() {

        itDepartment = departmentRepository.save(TestFixtures.department("IT"));
        hrDepartment = departmentRepository.save(TestFixtures.department("HR"));

        alice = userRepository.save(TestFixtures.user("alice", Role.EMPLOYEE, itDepartment));
        bob = userRepository.save(TestFixtures.user("bob", Role.EMPLOYEE, hrDepartment));

        rootFolder = folderRepository.save(TestFixtures.folder("root", null, alice, itDepartment));
    }

    // ===================== ABAC scope =====================

    @Nested
    @DisplayName("access scope pushed into the query")
    class AccessScope {

        @Test
        void shouldReturnEveryDocumentWhenScopeIsUnrestricted() {

            persistDocument("alpha", alice, itDepartment);
            persistDocument("beta", bob, hrDepartment);

            Page<DocumentListResponse> page = filter(request(), unrestrictedScope(), pageable());

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(DocumentListResponse::getTitle)
                    .containsExactlyInAnyOrder("alpha", "beta");
        }

        @Test
        void shouldReturnOwnedDocumentsAndLegacyDocumentsWithoutDepartmentForRestrictedScope() {

            persistDocument("owned-by-alice", alice, itDepartment);
            persistDocument("owned-by-bob", bob, hrDepartment);
            persistDocument("legacy-shared", null, null);

            DocumentAccessScope scope = new DocumentAccessScope(
                    false, alice.getId(), itDepartment.getId(), List.of());

            Page<DocumentListResponse> page = filter(request(), scope, pageable());

            assertThat(page.getContent()).extracting(DocumentListResponse::getTitle)
                    .containsExactlyInAnyOrder("owned-by-alice", "legacy-shared");
        }

        @Test
        void shouldIncludeDocumentSharedWithTheUserForRestrictedScope() {

            Document bobDocument = persistDocument("owned-by-bob", bob, hrDepartment);

            DocumentAccessScope scope = new DocumentAccessScope(
                    false, alice.getId(), itDepartment.getId(), List.of(bobDocument.getId()));

            Page<DocumentListResponse> page = filter(request(), scope, pageable());

            assertThat(page.getContent()).extracting(DocumentListResponse::getTitle)
                    .contains("owned-by-bob");
        }

        @Test
        void shouldReturnDepartmentDocumentsOfColleaguesForRestrictedScope() {

            User colleague = userRepository.save(TestFixtures.user("carol", Role.EMPLOYEE, itDepartment));
            persistDocument("colleague-document", colleague, itDepartment);

            DocumentAccessScope scope = new DocumentAccessScope(
                    false, alice.getId(), itDepartment.getId(), List.of());

            Page<DocumentListResponse> page = filter(request(), scope, pageable());

            assertThat(page.getContent()).extracting(DocumentListResponse::getTitle)
                    .containsExactly("colleague-document");
        }

        @Test
        void shouldFailFastWhenScopeIsNullInsteadOfReturningEverything() {

            persistDocument("alpha", alice, itDepartment);

            assertThatThrownBy(() -> filter(request(), null, pageable()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("DocumentAccessScope");
        }
    }

    // ===================== filters =====================

    @Nested
    @DisplayName("filters")
    class Filters {

        @Test
        void shouldFilterByKeywordIgnoringCase() {

            persistDocument("Quarterly Report", alice, itDepartment);
            persistDocument("Meeting Notes", alice, itDepartment);

            DocumentFilterRequest request = request();
            request.setKeyword("quarterly");

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("Quarterly Report");
        }

        @Test
        void shouldFilterByDocumentType() {

            persistDocument("contract", alice, itDepartment, DocumentType.CONTRACT);
            persistDocument("report", alice, itDepartment, DocumentType.REPORT);

            DocumentFilterRequest request = request();
            request.setDocumentType(DocumentType.CONTRACT);

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("contract");
        }

        @Test
        void shouldFilterByExtension() {

            persistDocument("a", alice, itDepartment, DocumentType.OTHER, "pdf", 100L);
            persistDocument("b", alice, itDepartment, DocumentType.OTHER, "xlsx", 100L);

            DocumentFilterRequest request = request();
            request.setExtension("xlsx");

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("b");
        }

        @Test
        void shouldFilterByFileSizeRange() {

            persistDocument("small", alice, itDepartment, DocumentType.OTHER, "pdf", 10L);
            persistDocument("medium", alice, itDepartment, DocumentType.OTHER, "pdf", 500L);
            persistDocument("large", alice, itDepartment, DocumentType.OTHER, "pdf", 5000L);

            DocumentFilterRequest request = request();
            request.setMinSize(100L);
            request.setMaxSize(1000L);

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("medium");
        }

        @Test
        void shouldFilterByVersionStatusAndDocumentStatus() {

            persistDocument("ready-active", alice, itDepartment, DocumentType.OTHER, "pdf", 10L,
                    VersionStatus.READY, DocumentStatus.ACTIVE);
            persistDocument("failed-active", alice, itDepartment, DocumentType.OTHER, "pdf", 10L,
                    VersionStatus.FAILED, DocumentStatus.ACTIVE);
            persistDocument("ready-deleted", alice, itDepartment, DocumentType.OTHER, "pdf", 10L,
                    VersionStatus.READY, DocumentStatus.DELETED);

            DocumentFilterRequest request = request();
            request.setStatus(VersionStatus.READY);

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactlyInAnyOrder("ready-active", "ready-deleted");

            request.setDocumentStatus(DocumentStatus.DELETED);

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("ready-deleted");
        }

        @Test
        void shouldFilterByOwnerAndDepartment() {

            persistDocument("alice-doc", alice, itDepartment);
            persistDocument("bob-doc", bob, hrDepartment);

            DocumentFilterRequest byOwner = request();
            byOwner.setOwnerId(bob.getId());

            assertThat(filter(byOwner, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("bob-doc");

            DocumentFilterRequest byDepartment = request();
            byDepartment.setDepartmentId(itDepartment.getId());

            assertThat(filter(byDepartment, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("alice-doc");
        }

        @Test
        void shouldFilterByUploadedDateRange() {

            LocalDateTime oldUpload = LocalDateTime.of(2024, 1, 10, 9, 0);
            LocalDateTime newUpload = LocalDateTime.of(2025, 6, 20, 15, 30);

            Document oldDocument = persistDocument("old", alice, itDepartment);
            Document newDocument = persistDocument("new", alice, itDepartment);
            overrideVersionCreatedAt(oldDocument, oldUpload);
            overrideVersionCreatedAt(newDocument, newUpload);

            DocumentFilterRequest request = request();
            request.setFromDate(LocalDateTime.of(2025, 1, 1, 0, 0));

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("new");

            request.setFromDate(null);
            request.setToDate(LocalDateTime.of(2024, 12, 31, 23, 59));

            assertThat(filter(request, unrestrictedScope(), pageable()).getContent())
                    .extracting(DocumentListResponse::getTitle)
                    .containsExactly("old");
        }
    }

    // ===================== sorting, pagination, projection =====================

    @Test
    void shouldSortNewestFirstByDefaultAndOldestFirstWhenRequested() {

        LocalDateTime first = LocalDateTime.of(2024, 3, 1, 8, 0);
        LocalDateTime second = LocalDateTime.of(2024, 5, 1, 8, 0);
        LocalDateTime third = LocalDateTime.of(2024, 7, 1, 8, 0);

        overrideVersionCreatedAt(persistDocument("first", alice, itDepartment), first);
        overrideVersionCreatedAt(persistDocument("second", alice, itDepartment), second);
        overrideVersionCreatedAt(persistDocument("third", alice, itDepartment), third);

        assertThat(filter(request(), unrestrictedScope(), pageable()).getContent())
                .extracting(DocumentListResponse::getTitle)
                .containsExactly("third", "second", "first");

        DocumentFilterRequest oldestFirst = request();
        oldestFirst.setSort("oldest");

        assertThat(filter(oldestFirst, unrestrictedScope(), pageable()).getContent())
                .extracting(DocumentListResponse::getTitle)
                .containsExactly("first", "second", "third");
    }

    @Test
    void shouldPaginateResultsAndReportTotalElements() {

        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 0, 0);
        overrideVersionCreatedAt(persistDocument("one", alice, itDepartment), base);
        overrideVersionCreatedAt(persistDocument("two", alice, itDepartment), base.plusDays(1));
        overrideVersionCreatedAt(persistDocument("three", alice, itDepartment), base.plusDays(2));

        PageRequest secondPage = PageRequest.of(1, 2);

        Page<DocumentListResponse> page = filter(request(), unrestrictedScope(), secondPage);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).extracting(DocumentListResponse::getTitle)
                .containsExactly("one");
    }

    @Test
    void shouldProjectOwnerDepartmentFolderAndDeletionAuditFields() {

        persistDocument("audited", alice, itDepartment);

        User deleter = userRepository.save(TestFixtures.user("dave", Role.MANAGER, itDepartment));
        Document deleted = persistDocument("deleted-one", alice, itDepartment);
        deleted.setStatus(DocumentStatus.DELETED);
        deleted.setDeletedBy(deleter);
        deleted.setDeletedAt(LocalDateTime.of(2025, 2, 2, 10, 0));
        documentRepository.save(deleted);
        entityManager.flush();
        entityManager.clear();

        DocumentListResponse row = filter(request(), unrestrictedScope(), pageable()).getContent().stream()
                .filter(item -> "deleted-one".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(row.getFolderId()).isEqualTo(rootFolder.getId());
        assertThat(row.getOwnerId()).isEqualTo(alice.getId());
        assertThat(row.getOwnerName()).isEqualTo(alice.getFullName());
        assertThat(row.getDepartmentId()).isEqualTo(itDepartment.getId());
        assertThat(row.getDepartmentName()).isEqualTo(itDepartment.getName());
        assertThat(row.getDeletedByName()).isEqualTo(deleter.getFullName());
        assertThat(row.getDeletedAt()).isEqualTo(LocalDateTime.of(2025, 2, 2, 10, 0));
        assertThat(row.getDocumentStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(row.getExtension()).isEqualTo("pdf");
        assertThat(row.getSize()).isEqualTo(1024L);
        assertThat(row.getVersionStatus()).isEqualTo(VersionStatus.READY);
        assertThat(row.getDocumentType()).isEqualTo(DocumentType.OTHER);
        assertThat(row.getUploadTime()).isNotNull();
    }

    // ===================== helpers =====================

    private DocumentAccessScope unrestrictedScope() {
        return new DocumentAccessScope(true, alice.getId(), null, List.of());
    }

    private DocumentFilterRequest request() {
        return new DocumentFilterRequest();
    }

    private Pageable pageable() {
        return PageRequest.of(0, 50);
    }

    private Page<DocumentListResponse> filter(
            DocumentFilterRequest filter,
            DocumentAccessScope scope,
            Pageable pageable
    ) {
        return documentRepository.filterDocuments(filter, scope, pageable);
    }

    private Document persistDocument(String title, User owner, Department department) {
        return persistDocument(title, owner, department, DocumentType.OTHER, "pdf", 1024L,
                VersionStatus.READY, DocumentStatus.ACTIVE);
    }

    private Document persistDocument(
            String title,
            User owner,
            Department department,
            DocumentType type
    ) {
        return persistDocument(title, owner, department, type, "pdf", 1024L,
                VersionStatus.READY, DocumentStatus.ACTIVE);
    }

    private Document persistDocument(
            String title,
            User owner,
            Department department,
            DocumentType type,
            String extension,
            long size
    ) {
        return persistDocument(title, owner, department, type, extension, size,
                VersionStatus.READY, DocumentStatus.ACTIVE);
    }

    private Document persistDocument(
            String title,
            User owner,
            Department department,
            DocumentType type,
            String extension,
            long size,
            VersionStatus versionStatus,
            DocumentStatus documentStatus
    ) {

        FileEntity file = TestFixtures.file(
                title + "." + extension,
                "object-key-" + UUID.randomUUID()
        );
        file.setFileSize(size);
        file.setExtension(extension);
        file = fileRepository.save(file);

        Document document = TestFixtures.document(title, rootFolder, owner, department, type);
        document.setStatus(documentStatus);
        document = documentRepository.save(document);

        DocumentVersion version = versionRepository.save(
                TestFixtures.version(document, file, 1, versionStatus)
        );

        document.setCurrentVersion(version);
        documentRepository.save(document);

        entityManager.flush();
        entityManager.clear();

        return document;
    }

    private void overrideVersionCreatedAt(Document document, LocalDateTime createdAt) {

        entityManager.createQuery(
                        "UPDATE DocumentVersion v SET v.createdAt = :createdAt WHERE v.document.id = :documentId"
                )
                .setParameter("createdAt", createdAt)
                .setParameter("documentId", document.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }
}
