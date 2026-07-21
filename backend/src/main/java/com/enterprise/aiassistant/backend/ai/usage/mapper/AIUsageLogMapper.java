package com.enterprise.aiassistant.backend.ai.usage.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.usage.helper.AIUsageLogSpecifications;

@Component
public class AIUsageLogMapper {

    public AIUsageLog toEntity(AIUsageLogRequest request) {
        return AIUsageLog.builder()
                .conversationType(request.getConversationType())
                .model(request.getModel())
                .inputTokens(request.getInputTokens() != null ? request.getInputTokens() : 0)
                .outputTokens(request.getOutputTokens() != null ? request.getOutputTokens() : 0)
                .estimatedCost(request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO)
                .status(request.getStatus())
                .errorMessage(request.getErrorMessage())
                .build();
    }

    public AIUsageLog toEntity(
            ConversationType conversationType,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal estimatedCost,
            AIUsageStatus status,
            String errorMessage
    ) {
        return AIUsageLog.builder()
                .conversationType(conversationType)
                .model(model)
                .inputTokens(inputTokens != null ? inputTokens : 0)
                .outputTokens(outputTokens != null ? outputTokens : 0)
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    // ➕ Method còn thiếu — nguyên nhân chính gây lỗi biên dịch ở ServiceImpl
    public AIUsageLogResponse toResponse(AIUsageLog entity) {
        return AIUsageLogResponse.builder()
                .createdAt(entity.getCreatedAt())
                .conversationType(entity.getConversationType())
                .model(entity.getModel())
                .inputTokens(entity.getInputTokens())
                .outputTokens(entity.getOutputTokens())
                .totalTokens(entity.getTotalTokens())
                .estimatedCost(entity.getEstimatedCost())
                .status(entity.getStatus())
                .build();
    }

    public AIUsageSummaryResponse toSummaryResponse(
            List<AIUsageLog> todayLogs,
            List<AIUsageLog> last7DayLogs
    ) {
        return AIUsageSummaryResponse.builder()
                .todayRequest(todayLogs.size())
                .todayToken(sumTokens(todayLogs))
                .todayCost(sumCost(todayLogs))
                .todaySuccessRate(AIUsageLogSpecifications.calculateSuccessRate(todayLogs))
                .last7DayRequests(last7DayLogs.size())
                .last7DayTokens(sumTokens(last7DayLogs))
                .last7DayCost(sumCost(last7DayLogs))
                .last7DaySuccessRate(AIUsageLogSpecifications.calculateSuccessRate(last7DayLogs))
                .build();
    }

    private long sumTokens(List<AIUsageLog> logs) {
        return logs.stream().mapToLong(AIUsageLog::getTotalTokens).sum();
    }

    private BigDecimal sumCost(List<AIUsageLog> logs) {
        return logs.stream()
                .map(AIUsageLog::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}