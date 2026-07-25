package com.enterprise.aiassistant.backend.ai.conversation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSourceResponse {

    private Long id;

    private Long documentChunkId;

    private Long documentId;

    private String documentTitle;

    private Integer chunkIndex;

    private Integer pageNumber;

    private String contentSnippet;

    private Double similarityScore;
}
