package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chunk persistence behaviour against PostgreSQL: ordering, deletion by version and the
 * (version, chunk_index) unique constraint.
 */
class DocumentChunkRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private DocumentChunkRepository chunkRepository;

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

        Document document = TestFixtures.document("chunked", folder, owner, department, DocumentType.OTHER);
        document.setStatus(DocumentStatus.ACTIVE);
        document = documentRepository.save(document);

        FileEntity file = fileRepository.save(TestFixtures.file(
                "chunked.pdf", "object-" + UUID.randomUUID()));

        version = versionRepository.save(TestFixtures.version(document, file, 1, VersionStatus.READY));
    }

    @Test
    void shouldReturnChunksOrderedByIndex() {

        persistChunk(2, "third");
        persistChunk(0, "first");
        persistChunk(1, "second");
        flushAndClear();

        assertThat(chunkRepository.findByDocumentVersionIdOrderByChunkIndexAsc(version.getId()))
                .extracting(DocumentChunk::getContent)
                .containsExactly("first", "second", "third");
    }

    @Test
    void shouldReturnEmptyListWhenVersionHasNoChunks() {

        assertThat(chunkRepository.findByDocumentVersionIdOrderByChunkIndexAsc(version.getId()))
                .isEmpty();
    }

    @Test
    void shouldDeleteChunksOfVersionOnly() {

        DocumentVersion otherVersion = persistOtherVersion();

        persistChunk(0, "keep");
        chunkRepository.save(DocumentChunk.builder()
                .documentVersion(otherVersion)
                .chunkIndex(0)
                .content("other")
                .build());
        flushAndClear();

        chunkRepository.deleteByDocumentVersionId(version.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(chunkRepository.findByDocumentVersionIdOrderByChunkIndexAsc(version.getId()))
                .isEmpty();
        assertThat(chunkRepository.findByDocumentVersionIdOrderByChunkIndexAsc(otherVersion.getId()))
                .hasSize(1);
    }

    @Test
    void shouldPersistChunkMetadata() {

        DocumentChunk chunk = chunkRepository.save(DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(5)
                .content("metadata chunk")
                .pageNumber(3)
                .startChar(100)
                .endChar(200)
                .tokenCount(25)
                .build());
        entityManager.flush();
        entityManager.clear();

        DocumentChunk reloaded = chunkRepository.findById(chunk.getId()).orElseThrow();

        assertThat(reloaded.getPageNumber()).isEqualTo(3);
        assertThat(reloaded.getStartChar()).isEqualTo(100);
        assertThat(reloaded.getEndChar()).isEqualTo(200);
        assertThat(reloaded.getTokenCount()).isEqualTo(25);
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldEnforceUniqueChunkIndexPerVersion() {

        persistChunk(1, "first");
        flushAndClear();

        DocumentChunk duplicate = DocumentChunk.builder()
                .documentVersion(versionRepository.findById(version.getId()).orElseThrow())
                .chunkIndex(1)
                .content("duplicate")
                .build();

        assertThatThrownBy(() -> chunkRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ===================== helpers =====================

    private void persistChunk(int index, String content) {

        chunkRepository.save(DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(index)
                .content(content)
                .build());
    }

    private DocumentVersion persistOtherVersion() {

        FileEntity file = fileRepository.save(TestFixtures.file(
                "v2.pdf", "object-" + UUID.randomUUID()));

        return versionRepository.save(
                TestFixtures.version(version.getDocument(), file, 2, VersionStatus.READY));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
