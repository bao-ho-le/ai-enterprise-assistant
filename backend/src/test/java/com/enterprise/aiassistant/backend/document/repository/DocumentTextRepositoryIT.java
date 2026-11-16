package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One-to-one document text persistence against PostgreSQL, including the unique version
 * constraint and large TEXT payloads.
 */
class DocumentTextRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private DocumentTextRepository textRepository;

    @Autowired
    private DocumentVersionRepository versionRepository;

    @Autowired
    private DocumentRepository documentRepository;

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

    private DocumentVersion version;

    @BeforeEach
    void setUp() {

        Department department = departmentRepository.save(TestFixtures.department("IT"));
        User owner = userRepository.save(TestFixtures.user("alice", Role.EMPLOYEE, department));
        Folder folder = folderRepository.save(TestFixtures.folder("root", null, owner, department));

        Document document = TestFixtures.document("textual", folder, owner, department, DocumentType.OTHER);
        document.setStatus(DocumentStatus.ACTIVE);
        document = documentRepository.save(document);

        FileEntity file = fileRepository.save(TestFixtures.file(
                "textual.txt", "object-" + UUID.randomUUID()));

        version = versionRepository.save(TestFixtures.version(document, file, 1, VersionStatus.READY));
    }

    @Test
    void shouldFindTextByVersionEntityAndId() {

        DocumentText saved = persistText("full extracted text");
        flushAndClear();

        assertThat(textRepository.findByDocumentVersion(version).orElseThrow().getContent())
                .isEqualTo("full extracted text");
        assertThat(textRepository.findByDocumentVersionId(version.getId()).orElseThrow().getId())
                .isEqualTo(saved.getId());
        assertThat(textRepository.existsByDocumentVersionId(version.getId())).isTrue();
    }

    @Test
    void shouldReturnEmptyWhenVersionHasNoText() {

        assertThat(textRepository.findByDocumentVersionId(version.getId())).isEmpty();
        assertThat(textRepository.existsByDocumentVersionId(version.getId())).isFalse();
    }

    @Test
    void shouldPersistLargeTextAndExtractionMethod() {

        String large = "lorem ipsum ".repeat(20_000);

        DocumentText saved = textRepository.save(DocumentText.builder()
                .documentVersion(version)
                .content(large)
                .extractionMethod(ExtractionMethod.DIRECT_TEXT)
                .language("en")
                .build());
        flushAndClear();

        DocumentText reloaded = textRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getContent()).hasSize(large.length());
        assertThat(reloaded.getExtractionMethod()).isEqualTo(ExtractionMethod.DIRECT_TEXT);
        assertThat(reloaded.getLanguage()).isEqualTo("en");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectSecondTextForSameVersion() {

        persistText("first");
        flushAndClear();

        DocumentText duplicate = DocumentText.builder()
                .documentVersion(versionRepository.findById(version.getId()).orElseThrow())
                .content("second")
                .extractionMethod(ExtractionMethod.DIRECT_TEXT)
                .build();

        assertThatThrownBy(() -> textRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteTextByVersion() {

        persistText("to delete");
        flushAndClear();

        textRepository.deleteByDocumentVersionId(version.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(textRepository.existsByDocumentVersionId(version.getId())).isFalse();
    }

    private DocumentText persistText(String content) {

        return textRepository.save(DocumentText.builder()
                .documentVersion(version)
                .content(content)
                .extractionMethod(ExtractionMethod.DIRECT_TEXT)
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
