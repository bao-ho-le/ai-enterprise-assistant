package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FolderRepositoryCustom {

    // folderId = null nghĩa là lấy các document ở thư mục gốc (root)
    Page<DocumentListResponse> getDocumentsInFolder(Long folderId, Pageable pageable);
}
