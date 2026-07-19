package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentRepositoryCustom {

    Page<DocumentListResponse> filterDocuments(
            DocumentFilterRequest filter,
            Pageable pageable
    );

}