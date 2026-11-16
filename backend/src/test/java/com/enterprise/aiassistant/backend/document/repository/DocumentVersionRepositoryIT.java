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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Derived version queries against real PostgreSQL, including version-number ordering.
 */
class DocumentVersionRepositoryIT extends AbstractPostgresIT {

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

    private Document document;

    @BeforeEach
    void setUp() {

        Department department = departmentRepository.save(TestFixtures.department("IT"));
        User owner = userRepository.save(TestFixtures.user("alice", Role.EMPLOYEE, department));
        Folder folder = folderRepository.save(TestFixtures.folder("root", null, owner, department));

        document = TestFixtures.document("versioned", folder, owner, department, DocumentType.OTHER);
        document.setStatus(DocumentStatus.ACTIVE);
        document = documentRepository.save(document);
    }

    @Test
    void shouldCountVersionsOfDocument() {

        persistVersion(1);
        persistVersion(2);

        assertThat(versionRepository.countByDocumentId(document.getId())).isEqualTo(2);
        assertThat(versionRepository.countByDocumentId(-1L)).isZero();
    }

    @Test
    void shouldReturnLatestVersionByNumber() {

        persistVersion(1);
        persistVersion(3);
        persistVersion(2);

        DocumentVersion latest = versionRepository
                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElseThrow();

        assertThat(latest.getVersionNumber()).isEqualTo(3);
    }

    @Test
    void shouldReturnEmptyWhenDocumentHasNoVersions() {

        assertThat(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId()))
                .isEmpty();
    }

    @Test
    void shouldIgnoreVersionsOfOtherDocuments() {

        persistVersion(5);

        Document other = TestFixtures.document("other", document.getFolder(), document.getOwner(),
                document.getDepartment(), DocumentType.REPORT);
        other.setStatus(DocumentStatus.ACTIVE);
        documentRepository.save(other);

        assertThat(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(other.getId()))
                .isEmpty();
        assertThat(versionRepository.countByDocumentId(other.getId())).isZero();
    }

    @Test
    void shouldPersistStatusAndChangeNote() {

        DocumentVersion version = persistVersion(7);
        version.setStatus(VersionStatus.FAILED);
        version.setErrorMessage("extraction failed");
        versionRepository.saveAndFlush(version);

        entityManager.clear();

        DocumentVersion reloaded = versionRepository.findById(version.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(VersionStatus.FAILED);
        assertThat(reloaded.getChangeNote()).isEqualTo("change note 7");
        assertThat(reloaded.getErrorMessage()).isEqualTo("extraction failed");
    }

    private DocumentVersion persistVersion(int number) {

        FileEntity file = fileRepository.save(TestFixtures.file(
                "v" + number + ".pdf",
                "object-" + UUID.randomUUID()
        ));

        DocumentVersion version = versionRepository.save(
                TestFixtures.version(document, file, number, VersionStatus.PENDING)
        );

        entityManager.flush();
        entityManager.clear();

        return version;
    }
}
