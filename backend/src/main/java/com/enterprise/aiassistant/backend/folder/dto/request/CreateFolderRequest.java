package com.enterprise.aiassistant.backend.folder.dto.request;

import lombok.Data;

@Data
public class CreateFolderRequest {

    private String name;

    // null = tạo ở thư mục gốc (root)
    private Long parentId;
}
