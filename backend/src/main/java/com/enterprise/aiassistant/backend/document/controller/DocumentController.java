package com.enterprise.aiassistant.backend.document.controller;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;

import com.enterprise.aiassistant.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

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

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadCurrentVersion(
            @PathVariable Long documentId
    ) {

        DocumentDownloadResource downloadResource =
                documentService.downloadCurrentVersion(documentId);

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(resolveMediaType(downloadResource.mimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        downloadResource.originalFilename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                );

        if (downloadResource.fileSize() != null) {
            responseBuilder.contentLength(downloadResource.fileSize());
        }

        return responseBuilder.body(downloadResource.resource());
    }

    private MediaType resolveMediaType(String mimeType) {

        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
