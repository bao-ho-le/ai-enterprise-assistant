package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageSourceResponse {
    private Long chunkId;

    private String documentTitle;

    private Integer pageNumber;

    private Double score;
}
