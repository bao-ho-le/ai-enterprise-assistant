package com.enterprise.aiassistant.backend.ai.usage.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageSummaryResponse {

    private long totalRequests;
    private long todayRequests;

    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokens;

    private BigDecimal estimatedCostThisMonth;

    private long successCount;
    private long errorCount;
    private double successRate; 
}