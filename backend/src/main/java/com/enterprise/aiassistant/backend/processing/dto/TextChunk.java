package com.enterprise.aiassistant.backend.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    private Integer chunkIndex;

    private String content;

    private Integer pageNumber;

    private Integer startChar;

    private Integer endChar;

    private Integer tokenCount;
}