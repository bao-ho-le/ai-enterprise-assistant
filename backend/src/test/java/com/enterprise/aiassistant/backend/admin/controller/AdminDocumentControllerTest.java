package com.enterprise.aiassistant.backend.admin.controller;

import com.enterprise.aiassistant.backend.admin.document.controller.AdminDocumentController;
import com.enterprise.aiassistant.backend.admin.document.service.AdminDocumentService;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDetailResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentRestoreResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentShareResponse;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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
 * Unit tests for {@link AdminDocumentController} delegation and response codes.
 */
class AdminDocumentControllerTest {

    private final AdminDocumentService adminDocumentService = mock(AdminDocumentService.class);

    private final AdminDocumentController controller =
            new AdminDocumentController(adminDocumentService, new DocumentMapper());

    @Test
    void shouldReturnAdminDocumentPage() {

        Pageable pageable = PageRequest.of(0, 20);
        DocumentFilterRequest filter = new DocumentFilterRequest();
        Page<DocumentListResponse> expected = new PageImpl<>(List.of());

        when(adminDocumentService.getDocuments(filter, pageable)).thenReturn(expected);

        assertThat(controller.getDocuments(filter, pageable)).isSameAs(expected);
    }

    @Test
    void shouldReturnDocumentDetail() {

        DocumentDetailResponse expected = DocumentDetailResponse.builder().build();
        when(adminDocumentService.getDocumentDetail(1L)).thenReturn(expected);

        assertThat(controller.getDocumentDetail(1L).getBody()).isSameAs(expected);
    }

    @Test
    void shouldReturnShareList() {

        List<DocumentShareResponse> expected = List.of();
        when(adminDocumentService.getDocumentShares(2L)).thenReturn(expected);

        assertThat(controller.getDocumentShares(2L).getBody()).isSameAs(expected);
    }

    @Test
    void shouldStreamDownload() {

        Resource resource = new ByteArrayResource(new byte[]{7});

        when(adminDocumentService.downloadSelectedVersion(3L, 4L)).thenReturn(
                DocumentDownloadResource.builder()
                        .resource(resource)
                        .originalFilename("v1.pdf")
                        .mimeType("application/pdf")
                        .fileSize(1L)
                        .build());

        ResponseEntity<Resource> response = controller.downloadSelectedVersion(3L, 4L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(resource);
    }

    @Test
    void shouldDeleteDocumentWithNoContent() {

        ResponseEntity<Void> response = controller.deleteDocument(5L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(adminDocumentService).deleteDocument(5L);
    }

    @Test
    void shouldRestoreDocument() {

        DocumentRestoreResponse expected = DocumentRestoreResponse.builder().documentId(6L).build();
        when(adminDocumentService.restoreDocument(6L)).thenReturn(expected);

        assertThat(controller.restoreDocument(6L).getBody()).isSameAs(expected);
    }
}
