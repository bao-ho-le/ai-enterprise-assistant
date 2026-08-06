package com.enterprise.aiassistant.backend.folder.dto.request;

import lombok.Data;

@Data
public class CreateFolderRequest {

    private String name;

    // Bắt buộc: chỉ root mới không có cha, và root do hệ thống tự tạo
    private Long parentId;
}
