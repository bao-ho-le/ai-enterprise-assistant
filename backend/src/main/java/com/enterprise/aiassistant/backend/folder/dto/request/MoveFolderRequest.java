package com.enterprise.aiassistant.backend.folder.dto.request;

import lombok.Data;

@Data
public class MoveFolderRequest {

    // Bắt buộc: muốn đưa folder về ngoài cùng thì truyền id của root
    private Long targetParentId;
}
