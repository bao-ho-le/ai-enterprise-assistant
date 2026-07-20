package com.enterprise.aiassistant.backend.ai.vectorstore.dto;

import lombok.Builder;
import lombok.Data;



@Data
@Builder
public class VectorPoint {

    private String id;

    private float[] vector;

    private VectorPayload payload;

}