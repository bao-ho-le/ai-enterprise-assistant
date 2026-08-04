package com.enterprise.aiassistant.backend.document.dto.request;

import lombok.Data;

@Data
public class MoveDocumentRequest {

    // null = di chuyển document ra thư mục gốc (root)
    private Long folderId;
}
