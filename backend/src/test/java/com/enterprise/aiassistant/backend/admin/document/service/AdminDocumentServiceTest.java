package com.enterprise.aiassistant.backend.admin.document.service;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDetailResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentRestoreResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentShareResponse;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the admin facade delegates to {@link DocumentService} without adding filtering,
 * and that admin listing uses the unrestricted admin query.
 */
class AdminDocumentServiceTest {

    private final DocumentService documentService = mock(DocumentService.class);

    private final AdminDocumentService adminDocumentService = new AdminDocumentService(documentService);

    @Test
    void shouldDelegateListingToAdminQuery() {

        Pageable pageable = PageRequest.of(0, 20);
        DocumentFilterRequest filter = new DocumentFilterRequest();
        Page<DocumentListResponse> expected = new PageImpl<>(List.of());

        when(documentService.getDocumentsForAdmin(filter, pageable)).thenReturn(expected);

        assertThat(adminDocumentService.getDocuments(filter, pageable)).isSameAs(expected);
        verify(documentService).getDocumentsForAdmin(filter, pageable);
    }

    @Test
    void shouldDelegateDetail() {

        DocumentDetailResponse expected = DocumentDetailResponse.builder().build();
        when(documentService.getDocumentDetail(5L)).thenReturn(expected);

        assertThat(adminDocumentService.getDocumentDetail(5L)).isSameAs(expected);
    }

    @Test
    void shouldDelegateShareListing() {

        List<DocumentShareResponse> expected = List.of();
        when(documentService.getDocumentShares(6L)).thenReturn(expected);

        assertThat(adminDocumentService.getDocumentShares(6L)).isSameAs(expected);
    }

    @Test
    void shouldDelegateDownload() {

        DocumentDownloadResource expected = DocumentDownloadResource.builder().build();
        when(documentService.downloadSelectedVersion(7L, 8L)).thenReturn(expected);

        assertThat(adminDocumentService.downloadSelectedVersion(7L, 8L)).isSameAs(expected);
    }

    @Test
    void shouldDelegateDelete() {

        adminDocumentService.deleteDocument(9L);

        verify(documentService).deleteDocument(9L);
        verify(documentService, org.mockito.Mockito.never()).restoreDocument(any());
    }

    @Test
    void shouldDelegateRestore() {

        DocumentRestoreResponse expected = DocumentRestoreResponse.builder().documentId(10L).build();
        when(documentService.restoreDocument(10L)).thenReturn(expected);

        assertThat(adminDocumentService.restoreDocument(10L)).isSameAs(expected);
        verify(documentService).restoreDocument(eq(10L));
    }
}
