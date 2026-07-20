package com.enterprise.aiassistant.backend.ai.usage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.helper.AIUsageLogSpecifications;
import com.enterprise.aiassistant.backend.ai.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIUsageLogServiceImpl implements AIUsageLogService {
    private final AIUsageLogMapper mapper;
    private final AIUsageLogRepository repository;

    @Override
    @Transactional
    public void log(
            String featureType,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal estimatedCost,
            AIUsageStatus status,
            String errorMessage
    ) {
        AIUsageLog entity = mapper.toEntity(
                featureType,
                model,
                inputTokens,
                outputTokens,
                estimatedCost,
                status,
                errorMessage
        );

        repository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        return repository.findAll(AIUsageLogSpecifications.byFilter(filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();

        long totalRequests = repository.count();
        long todayRequests = repository.countByCreatedAtGreaterThanEqual(startOfToday);
        long successCount = repository.countByStatus(AIUsageStatus.SUCCESS);
        long errorCount = repository.countByStatus(AIUsageStatus.FAILED);

        var thisMonthLogs = repository.findAll(
                AIUsageLogSpecifications.byFilter(AIUsageLogSpecifications.monthFilter(startOfMonth))
        );

        long totalInputTokens = thisMonthLogs.stream().mapToLong(AIUsageLog::getInputTokens).sum();
        long totalOutputTokens = thisMonthLogs.stream().mapToLong(AIUsageLog::getOutputTokens).sum();
        BigDecimal costThisMonth = thisMonthLogs.stream()
                .map(AIUsageLog::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double successRate = AIUsageLogSpecifications.calculateSuccessRate(successCount, totalRequests);

        return mapper.toSummaryResponse(
                totalRequests,
                todayRequests,
                totalInputTokens,
                totalOutputTokens,
                costThisMonth,
                successCount,
                errorCount,
                successRate
        );
    }
}