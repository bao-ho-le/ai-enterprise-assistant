package com.enterprise.aiassistant.backend.folder.dto.request;

import lombok.Data;

@Data
public class MoveFolderRequest {

    // null = di chuyển ra thư mục gốc (root)
    private Long targetParentId;
}
