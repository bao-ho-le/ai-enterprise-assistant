package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Derived queries and JPQL of {@link DocumentRepository} against real PostgreSQL.
 */
class DocumentRepositoryIT extends AbstractPostgresIT {

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

    private Department department;

    private User owner;

    private Folder rootFolder;

    private Folder childFolder;

    @BeforeEach
    void setUp() {

        department = departmentRepository.save(TestFixtures.department("IT"));
        owner = userRepository.save(TestFixtures.user("alice", Role.EMPLOYEE, department));
        rootFolder = folderRepository.save(TestFixtures.folder("root", null, owner, department));
        childFolder = folderRepository.save(TestFixtures.folder("child", rootFolder, owner, department));
    }

    @Test
    void shouldDetectExistingTitleCaseSensitively() {

        persist("Quarterly Report", rootFolder, DocumentStatus.ACTIVE);

        assertThat(documentRepository.existsByTitle("Quarterly Report")).isTrue();
        assertThat(documentRepository.existsByTitle("quarterly report")).isFalse();
        assertThat(documentRepository.existsByTitle("missing")).isFalse();
    }

    @Test
    void shouldFindDocumentsByFolderAndStatus() {

        persist("active-in-root", rootFolder, DocumentStatus.ACTIVE);
        persist("deleted-in-root", rootFolder, DocumentStatus.DELETED);
        persist("active-in-child", childFolder, DocumentStatus.ACTIVE);

        assertThat(documentRepository.findByFolderIdAndStatus(rootFolder.getId(), DocumentStatus.ACTIVE))
                .extracting(Document::getTitle)
                .containsExactly("active-in-root");
    }

    @Test
    void shouldFindAllDocumentsInFolderRegardlessOfStatus() {

        persist("active", rootFolder, DocumentStatus.ACTIVE);
        persist("deleted", rootFolder, DocumentStatus.DELETED);

        assertThat(documentRepository.findByFolderId(rootFolder.getId()))
                .extracting(Document::getTitle)
                .containsExactlyInAnyOrder("active", "deleted");
    }

    @Test
    void shouldFindDocumentsWithoutFolder() {

        Document orphan = Document.builder()
                .title("orphan")
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.ACTIVE)
                .owner(owner)
                .department(department)
                .build();

        documentRepository.save(orphan);

        assertThat(documentRepository.findByFolderIsNull())
                .extracting(Document::getTitle)
                .containsExactly("orphan");
    }

    @Test
    void shouldCountByStatusAndDepartment() {

        persist("a", rootFolder, DocumentStatus.ACTIVE);
        persist("b", rootFolder, DocumentStatus.ACTIVE);
        persist("c", rootFolder, DocumentStatus.DELETED);

        assertThat(documentRepository.countByStatus(DocumentStatus.ACTIVE)).isEqualTo(2);
        assertThat(documentRepository.countByDepartmentIdAndStatus(department.getId(), DocumentStatus.ACTIVE))
                .isEqualTo(2);
        assertThat(documentRepository.countByDepartmentIdAndStatus(department.getId(), DocumentStatus.DELETED))
                .isEqualTo(1);
    }

    @Test
    void shouldFindDocumentsByIds() {

        Document first = persist("first", rootFolder, DocumentStatus.ACTIVE);
        Document second = persist("second", rootFolder, DocumentStatus.ACTIVE);

        assertThat(documentRepository.findByIdIn(List.of(first.getId(), second.getId())))
                .extracting(Document::getTitle)
                .containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void shouldReturnOnlyDeletedDocumentsOrderedByDeletionTime() {

        LocalDateTime older = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime newer = LocalDateTime.of(2025, 3, 1, 0, 0);

        Document deletedOlder = persist("deleted-older", rootFolder, DocumentStatus.DELETED);
        Document deletedNewer = persist("deleted-newer", rootFolder, DocumentStatus.DELETED);
        persist("active", rootFolder, DocumentStatus.ACTIVE);

        setDeletedAt(deletedOlder, older);
        setDeletedAt(deletedNewer, newer);

        Page<Document> page = documentRepository.findDeletedDocuments(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Document::getTitle)
                .containsExactly("deleted-newer", "deleted-older");
    }

    @Test
    void shouldFilterDeletedDocumentsByKeywordIgnoringCase() {

        Document matching = persist("Quarterly Report", rootFolder, DocumentStatus.DELETED);
        Document other = persist("Meeting Notes", rootFolder, DocumentStatus.DELETED);

        setDeletedAt(matching, LocalDateTime.of(2025, 1, 1, 0, 0));
        setDeletedAt(other, LocalDateTime.of(2025, 2, 1, 0, 0));

        assertThat(documentRepository.findDeletedDocuments("quarterly", PageRequest.of(0, 10)).getContent())
                .extracting(Document::getTitle)
                .containsExactly("Quarterly Report");
    }

    @Test
    void shouldKeepCurrentVersionReferenceConsistent() {

        Document document = persist("with-version", rootFolder, DocumentStatus.ACTIVE);

        DocumentVersion current = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElseThrow();

        Document reloaded = documentRepository.findById(document.getId()).orElseThrow();

        assertThat(reloaded.getCurrentVersion().getId()).isEqualTo(current.getId());
    }

    // ===================== helpers =====================

    private Document persist(String title, Folder folder, DocumentStatus status) {

        FileEntity file = fileRepository.save(TestFixtures.file(
                title + ".pdf",
                "object-" + UUID.randomUUID()
        ));

        Document document = TestFixtures.document(title, folder, owner, department, DocumentType.OTHER);
        document.setStatus(status);
        document = documentRepository.save(document);

        DocumentVersion version = versionRepository.save(
                TestFixtures.version(document, file, 1, VersionStatus.READY)
        );

        document.setCurrentVersion(version);
        document = documentRepository.save(document);

        entityManager.flush();
        entityManager.clear();

        return document;
    }

    private void setDeletedAt(Document document, LocalDateTime deletedAt) {

        entityManager.createQuery(
                        "UPDATE Document d SET d.deletedAt = :deletedAt WHERE d.id = :id")
                .setParameter("deletedAt", deletedAt)
                .setParameter("id", document.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }
}
