package com.enterprise.aiassistant.backend.document.dto.request;

import lombok.Data;

@Data
public class MoveDocumentRequest {

    // null = chuyển document về thư mục gốc (root)
    private Long folderId;
}
