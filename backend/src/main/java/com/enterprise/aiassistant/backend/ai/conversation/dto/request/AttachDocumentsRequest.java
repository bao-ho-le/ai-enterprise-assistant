package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import lombok.Data;

import java.util.List;

// Dùng cho API đính kèm document version vào conversation (chưa implement trong lượt này)
@Data
public class AttachDocumentsRequest {

    private List<Long> documentVersionIds;
}
