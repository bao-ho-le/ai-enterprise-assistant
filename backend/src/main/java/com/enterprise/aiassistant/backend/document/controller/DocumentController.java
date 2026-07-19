package com.enterprise.aiassistant.backend.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;

import com.enterprise.aiassistant.backend.document.helper.DocumentHelper;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("${api.prefix}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;


    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestPart("request") DocumentUploadRequest request
    ) {
        return ResponseEntity.ok(documentService.upload(file, request));
    }

    @PostMapping(
            value = "/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadNewVersionResponse> uploadNewVersion(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            @RequestPart("request") UploadNewVersionRequest request
    ) {
        return ResponseEntity.ok(
                documentService.uploadNewVersion(documentId, file, request)
        );
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<DocumentUpdateMetadataResponse> updateDocumentMetadata(
            @PathVariable Long documentId,
            @RequestBody DocumentUpdateMetadataRequest request
    ) {

        return ResponseEntity.ok(
                documentService.updateDocumentMetadata(documentId, request)
        );
    }


    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentId}/{versionId}/download")
    public ResponseEntity<Resource> downloadSelectedVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {

        DocumentDownloadResource documentDownloadResource =
                documentService.downloadSelectedVersion(documentId, versionId);

        return documentMapper.toDownloadResponse(documentDownloadResource);

    }

    @GetMapping
    public Page<DocumentListResponse> getDocuments(
            DocumentFilterRequest filter,

            @PageableDefault(page = 0, size = 10)
            Pageable pageable
    ){

        return documentService.getDocuments(
                filter,
                pageable
        );
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(
            @PathVariable Long documentId
    ) {
        return ResponseEntity.ok(documentService.getDocumentDetail(documentId));
    }

    @GetMapping("/check-title")
    public boolean checkTitle(@RequestParam String title) {
        return documentService.existsByTitle(title);
    }
}
