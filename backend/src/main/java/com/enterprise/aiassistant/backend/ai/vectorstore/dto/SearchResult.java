package com.enterprise.aiassistant.backend.ai.vectorstore.dto;

import lombok.Builder;
import lombok.Data;



@Data
@Builder
public class SearchResult {

    private String pointId;

    private Double score;

    private VectorPayload payload;

}