package com.enterprise.aiassistant.backend.ai.usage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageLogResponse {

    private Long id;
    private LocalDateTime createdAt;
    private String featureType;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private AIUsageStatus status;
}