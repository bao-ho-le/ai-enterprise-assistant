package com.enterprise.aiassistant.backend.admin.trash.service;

import com.enterprise.aiassistant.backend.admin.trash.dto.TrashItemResponse;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminTrashService}: permission gate, merging documents and folders,
 * ordering by deletion time and in-memory pagination.
 */
class AdminTrashServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);

    private final FolderRepository folderRepository = mock(FolderRepository.class);

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private AdminTrashService service;

    @BeforeEach
    void setUp() {
        service = new AdminTrashService(documentRepository, folderRepository, currentUserService);
    }

    @Test
    void shouldRequireDocumentDeletePermissionAndSkipRepositoriesWhenDenied() {

        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.DOCUMENT_DELETE);

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.getTrashItems(null, null, PageRequest.of(0, 20))),
                ErrorCode.PERMISSION_DENIED
        );

        verifyNoInteractions(documentRepository, folderRepository);
    }

    @Test
    void shouldMergeDocumentsAndFoldersWhenTypeIsAbsent() {

        LocalDateTime deletedAt = LocalDateTime.of(2025, 4, 1, 12, 0);

        when(documentRepository.findDeletedDocuments(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedDocument("doc", deletedAt))));
        when(folderRepository.findByStatusOrderByDeletedAtDesc(eq(FolderStatus.DELETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedFolder("folder", deletedAt))));

        List<TrashItemResponse> items = service.getTrashItems(null, null, PageRequest.of(0, 20)).getContent();

        assertThat(items).extracting(TrashItemResponse::getType)
                .containsExactlyInAnyOrder("DOCUMENT", "FOLDER");
        assertThat(items).extracting(TrashItemResponse::getName)
                .containsExactlyInAnyOrder("doc", "folder");
    }

    @Test
    void shouldQueryOnlyDocumentsWhenTypeIsDocument() {

        when(documentRepository.findDeletedDocuments(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedDocument("doc", LocalDateTime.now()))));

        assertThat(service.getTrashItems(null, "DOCUMENT", PageRequest.of(0, 20)).getContent())
                .extracting(TrashItemResponse::getType)
                .containsExactly("DOCUMENT");

        verifyNoInteractions(folderRepository);
    }

    @Test
    void shouldQueryOnlyFoldersWhenTypeIsFolderIgnoringCase() {

        when(folderRepository.findByStatusAndNameContainingIgnoreCaseOrderByDeletedAtDesc(
                eq(FolderStatus.DELETED), eq("thesis"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedFolder("thesis", LocalDateTime.now()))));

        assertThat(service.getTrashItems("  thesis  ", "folder", PageRequest.of(0, 20)).getContent())
                .extracting(TrashItemResponse::getType)
                .containsExactly("FOLDER");

        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldPassNullKeywordToRepositoriesWhenKeywordIsBlank() {

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getTrashItems("   ", "DOCUMENT", PageRequest.of(0, 20));

        verify(documentRepository).findDeletedDocuments(isNull(), any(Pageable.class));
    }

    @Test
    void shouldOrderByDeletionTimeNewestFirstWithNullsLast() {

        LocalDateTime oldest = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime newest = LocalDateTime.of(2025, 1, 1, 0, 0);

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        deletedDocument("oldest", oldest),
                        deletedDocument("undefined", null),
                        deletedDocument("newest", newest)
                )));

        assertThat(service.getTrashItems(null, "DOCUMENT", PageRequest.of(0, 20)).getContent())
                .extracting(TrashItemResponse::getName)
                .containsExactly("newest", "oldest", "undefined");
    }

    @Test
    void shouldPaginateMergedItemsInMemory() {

        LocalDateTime base = LocalDateTime.of(2025, 1, 1, 0, 0);

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        deletedDocument("d1", base.plusDays(1)),
                        deletedDocument("d2", base.plusDays(2)),
                        deletedDocument("d3", base.plusDays(3))
                )));

        Page<TrashItemResponse> page = service.getTrashItems(null, "DOCUMENT", PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(TrashItemResponse::getName).containsExactly("d1");
    }

    @Test
    void shouldReturnEmptyPageWhenOffsetExceedsAvailableItems() {

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedDocument("only", LocalDateTime.now()))));

        Page<TrashItemResponse> page = service.getTrashItems(null, "DOCUMENT", PageRequest.of(5, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldMapOwnerDepartmentAndDeleterNames() {

        User owner = User.builder().id(1L).fullName("Owner Name").build();
        User deleter = User.builder().id(2L).fullName("Deleter Name").build();
        Department department = Department.builder().id(3L).name("IT").build();

        Document document = Document.builder()
                .id(10L)
                .title("audited doc")
                .documentType(DocumentType.REPORT)
                .status(DocumentStatus.DELETED)
                .owner(owner)
                .department(department)
                .deletedBy(deleter)
                .deletedAt(LocalDateTime.of(2025, 2, 3, 4, 5))
                .build();

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(document)));

        TrashItemResponse item = service.getTrashItems(null, "DOCUMENT", PageRequest.of(0, 10))
                .getContent()
                .get(0);

        assertThat(item.getItemId()).isEqualTo(10L);
        assertThat(item.getName()).isEqualTo("audited doc");
        assertThat(item.getOwnerName()).isEqualTo("Owner Name");
        assertThat(item.getDepartmentName()).isEqualTo("IT");
        assertThat(item.getDeletedByName()).isEqualTo("Deleter Name");
        assertThat(item.getDeletedAt()).isEqualTo(LocalDateTime.of(2025, 2, 3, 4, 5));
    }

    @Test
    void shouldTolerateMissingOwnerDepartmentAndDeleter() {

        Document orphan = Document.builder()
                .id(11L)
                .title("orphan")
                .status(DocumentStatus.DELETED)
                .deletedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        when(documentRepository.findDeletedDocuments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orphan)));

        TrashItemResponse item = service.getTrashItems(null, "DOCUMENT", PageRequest.of(0, 10))
                .getContent()
                .get(0);

        assertThat(item.getOwnerName()).isNull();
        assertThat(item.getDepartmentName()).isNull();
        assertThat(item.getDeletedByName()).isNull();
    }

    @Test
    void shouldQueryDeletedFoldersWithoutKeywordFilter() {

        when(folderRepository.findByStatusOrderByDeletedAtDesc(eq(FolderStatus.DELETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deletedFolder("folder", LocalDateTime.now()))));

        service.getTrashItems(null, "FOLDER", PageRequest.of(0, 10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(folderRepository).findByStatusOrderByDeletedAtDesc(
                eq(FolderStatus.DELETED), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().isUnpaged()).isTrue();
        verify(documentRepository, never()).findDeletedDocuments(any(), any());
    }

    // ===================== helpers =====================

    private static Document deletedDocument(String title, LocalDateTime deletedAt) {

        return Document.builder()
                .id((long) title.hashCode())
                .title(title)
                .documentType(DocumentType.REPORT)
                .status(DocumentStatus.DELETED)
                .deletedAt(deletedAt)
                .build();
    }

    private static Folder deletedFolder(String name, LocalDateTime deletedAt) {

        return Folder.builder()
                .id((long) name.hashCode())
                .name(name)
                .status(FolderStatus.DELETED)
                .deletedAt(deletedAt)
                .build();
    }
}
