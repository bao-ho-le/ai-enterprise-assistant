package com.enterprise.aiassistant.backend.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${api.prefix}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestPart("request") DocumentUploadRequest request
    ){
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
}
