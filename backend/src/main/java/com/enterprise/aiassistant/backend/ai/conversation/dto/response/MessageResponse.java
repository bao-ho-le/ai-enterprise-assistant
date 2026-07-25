package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;

    private AIMessageRole role;

    private String content;

    private OffsetDateTime createdAt;

    private List<MessageSourceResponse> sources;
}
