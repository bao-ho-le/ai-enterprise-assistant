package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AIMessageResponse {

    private Long id;

    private AIMessageRole role;

    private String content;

    private LocalDateTime createdAt;

    private List<MessageSourceResponse> sources;
}
