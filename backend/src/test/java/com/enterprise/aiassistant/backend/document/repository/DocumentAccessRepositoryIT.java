package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentAccess;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Derived queries, JPQL and the unique constraint of {@link DocumentAccess} against PostgreSQL.
 */
class DocumentAccessRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private DocumentAccessRepository accessRepository;

    @Autowired
    private DocumentRepository documentRepository;

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

    private User recipient;

    private User other;

    private Document document;

    @BeforeEach
    void setUp() {

        department = departmentRepository.save(TestFixtures.department("IT"));
        owner = userRepository.save(TestFixtures.user("owner", Role.EMPLOYEE, department));
        recipient = userRepository.save(TestFixtures.user("recipient", Role.EMPLOYEE, department));
        other = userRepository.save(TestFixtures.user("other", Role.EMPLOYEE, department));

        Folder folder = folderRepository.save(TestFixtures.folder("root", null, owner, department));

        document = TestFixtures.document("shared-document", folder, owner, department, DocumentType.OTHER);
        document.setStatus(DocumentStatus.ACTIVE);
        document = documentRepository.save(document);
    }

    @Test
    void shouldDetectExplicitAccess() {

        persistAccess(document, recipient, owner);
        flushAndClear();

        assertThat(accessRepository.existsByDocumentIdAndUserId(document.getId(), recipient.getId()))
                .isTrue();
        assertThat(accessRepository.existsByDocumentIdAndUserId(document.getId(), other.getId()))
                .isFalse();
    }

    @Test
    void shouldFindAccessByDocumentAndUser() {

        persistAccess(document, recipient, owner);
        flushAndClear();

        DocumentAccess access = accessRepository
                .findByDocumentIdAndUserId(document.getId(), recipient.getId())
                .orElseThrow();

        assertThat(access.getDocument().getId()).isEqualTo(document.getId());
        assertThat(access.getUser().getId()).isEqualTo(recipient.getId());
        assertThat(access.getGrantedBy().getId()).isEqualTo(owner.getId());
        assertThat(access.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnEmptyForUnknownCombination() {

        assertThat(accessRepository.findByDocumentIdAndUserId(document.getId(), recipient.getId()))
                .isEmpty();
    }

    @Test
    void shouldListAccessesOfDocumentInCreationOrder() {

        persistAccess(document, recipient, owner);
        persistAccess(document, other, owner);
        flushAndClear();

        assertThat(accessRepository.findByDocumentIdOrderByCreatedAtAsc(document.getId()))
                .extracting(access -> access.getUser().getId())
                .containsExactly(recipient.getId(), other.getId());
    }

    @Test
    void shouldReturnDocumentIdsSharedWithUser() {

        Document second = TestFixtures.document("second", document.getFolder(), owner, department,
                DocumentType.REPORT);
        second.setStatus(DocumentStatus.ACTIVE);
        second = documentRepository.save(second);

        persistAccess(document, recipient, owner);
        persistAccess(second, recipient, owner);
        persistAccess(document, other, owner);
        flushAndClear();

        assertThat(accessRepository.findDocumentIdsSharedWithUser(recipient.getId()))
                .containsExactlyInAnyOrder(document.getId(), second.getId());
        assertThat(accessRepository.findDocumentIdsSharedWithUser(owner.getId())).isEmpty();
    }

    @Test
    void shouldDeleteAllAccessesOfDocument() {

        persistAccess(document, recipient, owner);
        persistAccess(document, other, owner);
        flushAndClear();

        accessRepository.deleteByDocumentId(document.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(accessRepository.findByDocumentIdOrderByCreatedAtAsc(document.getId())).isEmpty();
    }

    @Test
    void shouldEnforceUniqueDocumentUserPair() {

        persistAccess(document, recipient, owner);
        flushAndClear();

        DocumentAccess duplicate = DocumentAccess.builder()
                .document(documentRepository.findById(document.getId()).orElseThrow())
                .user(userRepository.findById(recipient.getId()).orElseThrow())
                .grantedBy(owner)
                .build();

        assertThatThrownBy(() -> {
            accessRepository.saveAndFlush(duplicate);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void shouldSearchSharedDocumentsByFilters() {

        persistAccess(document, recipient, owner);
        flushAndClear();

        Page<DocumentAccess> all = accessRepository.searchSharedDocuments(
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(all.getTotalElements()).isEqualTo(1);

        assertThat(accessRepository.searchSharedDocuments(
                "shared-doc", null, null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(accessRepository.searchSharedDocuments(
                "missing", null, null, null, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(accessRepository.searchSharedDocuments(
                null, owner.getId(), null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(accessRepository.searchSharedDocuments(
                null, null, department.getId(), null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(accessRepository.searchSharedDocuments(
                null, null, null, recipient.getId(), PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(accessRepository.searchSharedDocuments(
                null, null, null, other.getId(), PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    // ===================== helpers =====================

    private DocumentAccess persistAccess(Document target, User user, User grantedBy) {

        return accessRepository.save(DocumentAccess.builder()
                .document(target)
                .user(user)
                .grantedBy(grantedBy)
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @SuppressWarnings("unused")
    private static List<Long> idsOf(List<DocumentAccess> accesses) {
        return accesses.stream().map(DocumentAccess::getId).toList();
    }
}
