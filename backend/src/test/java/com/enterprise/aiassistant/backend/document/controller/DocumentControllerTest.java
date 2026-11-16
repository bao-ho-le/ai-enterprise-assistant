package com.enterprise.aiassistant.backend.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentBatchUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.MoveDocumentRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDetailResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentMoveResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentRestoreResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentController} response codes and delegation. Sharing endpoints are
 * intentionally out of scope.
 */
class DocumentControllerTest {

    private final DocumentService documentService = mock(DocumentService.class);

    private final DocumentController controller =
            new DocumentController(documentService, new DocumentMapper());

    @Test
    void shouldReturnUploadedDocuments() {

        List<MultipartFile> files = List.of(pdf());
        DocumentBatchUploadRequest request = new DocumentBatchUploadRequest();
        List<DocumentUploadResponse> expected = List.of(DocumentUploadResponse.builder()
                .documentId(1L)
                .build());

        when(documentService.upload(files, request)).thenReturn(expected);

        ResponseEntity<List<DocumentUploadResponse>> response = controller.upload(files, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void shouldReturnNewVersionResponse() {

        MultipartFile file = pdf();
        UploadNewVersionRequest request = new UploadNewVersionRequest();
        UploadNewVersionResponse expected = UploadNewVersionResponse.builder().versionNumber(2).build();

        when(documentService.uploadNewVersion(5L, file, request)).thenReturn(expected);

        ResponseEntity<UploadNewVersionResponse> response =
                controller.uploadNewVersion(5L, file, request);

        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void shouldReturnUpdatedMetadata() {

        DocumentUpdateMetadataRequest request = new DocumentUpdateMetadataRequest();
        DocumentUpdateMetadataResponse expected = DocumentUpdateMetadataResponse.builder()
                .documentId(3L)
                .build();

        when(documentService.updateDocumentMetadata(3L, request)).thenReturn(expected);

        assertThat(controller.updateDocumentMetadata(3L, request).getBody()).isSameAs(expected);
    }

    @Test
    void shouldReturnNoContentAfterSoftDelete() {

        ResponseEntity<Void> response = controller.deleteDocument(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
        verify(documentService).deleteDocument(9L);
    }

    @Test
    void shouldReturnRestoredDocument() {

        DocumentRestoreResponse expected = DocumentRestoreResponse.builder().documentId(4L).build();
        when(documentService.restoreDocument(4L)).thenReturn(expected);

        assertThat(controller.restoreDocument(4L).getBody()).isSameAs(expected);
    }

    @Test
    void shouldReturnMovedDocument() {

        MoveDocumentRequest request = new MoveDocumentRequest();
        DocumentMoveResponse expected = DocumentMoveResponse.builder().documentId(6L).folderId(2L).build();

        when(documentService.moveDocument(6L, request)).thenReturn(expected);

        assertThat(controller.moveDocument(6L, request).getBody()).isSameAs(expected);
    }

    @Test
    void shouldStreamSelectedVersionDownload() {

        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(documentService.downloadSelectedVersion(1L, 2L)).thenReturn(
                DocumentDownloadResource.builder()
                        .resource(resource)
                        .originalFilename("report.pdf")
                        .mimeType("application/pdf")
                        .fileSize(3L)
                        .build());

        ResponseEntity<Resource> response = controller.downloadSelectedVersion(1L, 2L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("report.pdf");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(3);
    }

    @Test
    void shouldReturnFilteredPage() {

        Pageable pageable = PageRequest.of(0, 10);
        DocumentFilterRequest filter = new DocumentFilterRequest();
        Page<DocumentListResponse> expected = new PageImpl<>(List.of());

        when(documentService.getDocuments(filter, pageable)).thenReturn(expected);

        assertThat(controller.getDocuments(filter, pageable)).isSameAs(expected);
    }

    @Test
    void shouldReturnDocumentDetail() {

        DocumentDetailResponse expected = DocumentDetailResponse.builder().build();
        when(documentService.getDocumentDetail(8L)).thenReturn(expected);

        ResponseEntity<DocumentDetailResponse> response = controller.getDocumentDetail(8L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void shouldReportTitleExistence() {

        when(documentService.existsByTitle("taken")).thenReturn(true);

        assertThat(controller.checkTitle("taken")).isTrue();
        verify(documentService).existsByTitle(eq("taken"));
    }

    private static MultipartFile pdf() {
        return new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});
    }
}
