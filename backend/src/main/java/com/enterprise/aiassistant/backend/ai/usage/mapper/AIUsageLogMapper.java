package com.enterprise.aiassistant.backend.ai.usage.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.usage.helper.AiUsageHelper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageDailyProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AIUsageLogMapper {

    private final AiUsageHelper usageHelpful;

    public AIUsageLog toEntity(AIUsageLogRequest request) {
    Integer input = request.getInputTokens();
    Integer output = request.getOutputTokens();

    int inputTokens = input != null ? input : 0;
    int outputTokens = output != null ? output : 0;

        return AIUsageLog.builder()
                .conversationType(request.getConversationType())
                .model(request.getModel())
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(usageHelpful.calculateTotalTokens(inputTokens, outputTokens))
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
        int in = inputTokens != null ? inputTokens : 0;
        int out = outputTokens != null ? outputTokens : 0;

        return AIUsageLog.builder()
                .conversationType(conversationType)
                .model(model)
                .inputTokens(in)
                .outputTokens(out)
                .totalTokens(usageHelpful.calculateTotalTokens(in, out))
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

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
                .todaySuccessRate(usageHelpful.calculateSuccessRate(todayLogs))
                .last7DayRequests(last7DayLogs.size())
                .last7DayTokens(sumTokens(last7DayLogs))
                .last7DayCost(sumCost(last7DayLogs))
                .last7DaySuccessRate(usageHelpful.calculateSuccessRate(last7DayLogs))
                .build();
    }

    public AIUsageDailyResponse toDailyResponse(LocalDate date, AIUsageDailyProjection row) {
        if (row == null) {
            return AIUsageDailyResponse.builder()
                    .date(date)
                    .cost(BigDecimal.ZERO)
                    .build();
        }
        return AIUsageDailyResponse.builder()
                .date(date)
                .requestCount(row.getRequestCount())
                .inputTokens(row.getInputTokens())
                .outputTokens(row.getOutputTokens())
                .totalTokens(row.getTotalTokens())
                .cost(row.getCost())
                .successCount(row.getSuccessCount())
                .failedCount(row.getFailedCount())
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
