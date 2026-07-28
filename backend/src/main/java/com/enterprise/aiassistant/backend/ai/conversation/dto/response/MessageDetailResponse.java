package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MessageDetailResponse {

    private Long id;

    private AIMessageRole role;

    private String content;

    private LocalDateTime createdAt;

    private List<MessageSourceResponse> sources;
}
