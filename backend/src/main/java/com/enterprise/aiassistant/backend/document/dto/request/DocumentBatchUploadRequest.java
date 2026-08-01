package com.enterprise.aiassistant.backend.document.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class DocumentBatchUploadRequest {

    private List<DocumentUploadRequest> documents;

}
