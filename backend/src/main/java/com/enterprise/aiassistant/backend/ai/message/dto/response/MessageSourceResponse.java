package com.enterprise.aiassistant.backend.ai.message.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageSourceResponse {

    private Long chunkId;

    private Long documentChunkId;

    private String documentTitle;

    private Integer pageNumber;

    private Double score;

}
