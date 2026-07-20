package com.enterprise.aiassistant.backend.ai.usage.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;

@Component
public class AIUsageLogMapper {

    public AIUsageLogResponse toResponse(AIUsageLog entity) {
        return AIUsageLogResponse.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .featureType(entity.getFeatureType())
                .model(entity.getModel())
                .inputTokens(entity.getInputTokens())
                .outputTokens(entity.getOutputTokens())
                .totalTokens(entity.getTotalTokens())
                .estimatedCost(entity.getEstimatedCost())
                .status(entity.getStatus())
                .build();
    }

    public AIUsageLog toEntity(
            String featureType,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal estimatedCost,
            AIUsageStatus status,
            String errorMessage
    ) {
        return AIUsageLog.builder()
                .featureType(featureType)
                .model(model)
                .inputTokens(inputTokens != null ? inputTokens : 0)
                .outputTokens(outputTokens != null ? outputTokens : 0)
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    public AIUsageSummaryResponse toSummaryResponse(
            long totalRequests,
            long todayRequests,
            long totalInputTokens,
            long totalOutputTokens,
            BigDecimal estimatedCostThisMonth,
            long successCount,
            long errorCount,
            double successRate
    ) {
        return AIUsageSummaryResponse.builder()
                .totalRequests(totalRequests)
                .todayRequests(todayRequests)
                .totalInputTokens(totalInputTokens)
                .totalOutputTokens(totalOutputTokens)
                .totalTokens(totalInputTokens + totalOutputTokens)
                .estimatedCostThisMonth(estimatedCostThisMonth)
                .successCount(successCount)
                .errorCount(errorCount)
                .successRate(successRate)
                .build();
    }
}