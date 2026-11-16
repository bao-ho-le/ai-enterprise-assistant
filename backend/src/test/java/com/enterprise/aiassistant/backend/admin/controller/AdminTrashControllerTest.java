package com.enterprise.aiassistant.backend.admin.controller;

import com.enterprise.aiassistant.backend.admin.trash.controller.AdminTrashController;
import com.enterprise.aiassistant.backend.admin.trash.dto.TrashItemResponse;
import com.enterprise.aiassistant.backend.admin.trash.service.AdminTrashService;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminTrashController} delegation and response codes.
 */
class AdminTrashControllerTest {

    private final AdminTrashService adminTrashService = mock(AdminTrashService.class);

    private final DocumentService documentService = mock(DocumentService.class);

    private final FolderService folderService = mock(FolderService.class);

    private final AdminTrashController controller =
            new AdminTrashController(adminTrashService, documentService, folderService);

    @Test
    void shouldReturnTrashPage() {

        Pageable pageable = PageRequest.of(0, 20);
        Page<TrashItemResponse> expected = new PageImpl<>(List.of());

        when(adminTrashService.getTrashItems("kw", "DOCUMENT", pageable)).thenReturn(expected);

        assertThat(controller.getTrashItems("kw", "DOCUMENT", pageable)).isSameAs(expected);
    }

    @Test
    void shouldRestoreDocumentWithNoContent() {

        ResponseEntity<Void> response = controller.restoreDocument(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(documentService).restoreDocument(1L);
    }

    @Test
    void shouldRestoreFolderWithNoContent() {

        ResponseEntity<Void> response = controller.restoreFolder(2L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(folderService).restoreFolder(2L);
    }

    @Test
    void shouldPermanentlyDeleteFolderWithNoContent() {

        ResponseEntity<Void> response = controller.permanentlyDeleteFolder(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(folderService).hardDeleteFolder(3L);
    }
}
