#!/bin/bash
set -e
cd src/main/java/com/enterprise/aiassistant/backend/ai/usage

mv helper/AIUsageLogSpecifications.java helper/UsageHelpful.java

cat > helper/UsageHelpful.java << 'EOF'
package com.enterprise.aiassistant.backend.ai.usage.helper;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;

@Component
public class UsageHelpful {

    public AIUsageLogFilterRequest fromDateFilter(LocalDateTime from) {
        var filter = new AIUsageLogFilterRequest();
        filter.setFromDate(from);
        return filter;
    }

    public double calculateSuccessRate(long successCount, long totalRequests) {
        return totalRequests == 0
                ? 0
                : (double) successCount / totalRequests * 100;
    }

    public double calculateSuccessRate(List<AIUsageLog> logs) {
        if (logs.isEmpty()) {
            return 0;
        }
        long successCount = logs.stream()
                .filter(l -> l.getStatus() == AIUsageStatus.SUCCESS)
                .count();
        return (double) successCount / logs.size() * 100;
    }

    public Specification<AIUsageLog> byFilter(AIUsageLogFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filter.getFromDate() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }

            if (StringUtils.hasText(filter.getModel())) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("model"), filter.getModel()));
            }
            if (filter.getConversationType() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("conversationType"), filter.getConversationType()));
            }
            if (filter.getStatus() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), filter.getStatus()));
            }
            return predicates;
        };
    }
}
EOF

cat > service/AIUsageLogServiceImpl.java << 'EOF'
package com.enterprise.aiassistant.backend.ai.usage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.helper.UsageHelpful;
import com.enterprise.aiassistant.backend.ai.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIUsageLogServiceImpl implements AIUsageLogService {

    private final AIUsageLogMapper mapper;
    private final AIUsageLogRepository repository;
    private final UsageHelpful specifications;

    @Override
    @Transactional
    public void logAiUsage(AIUsageLogRequest request) {
        AIUsageLog entity = mapper.toEntity(request);
        repository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        return repository.findAll(specifications.byFilter(filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfLast7Days = LocalDate.now().minusDays(6).atStartOfDay();

        var todayLogs = repository.findAll(
                specifications.byFilter(specifications.fromDateFilter(startOfToday))
        );
        var last7DayLogs = repository.findAll(
                specifications.byFilter(specifications.fromDateFilter(startOfLast7Days))
        );

        return mapper.toSummaryResponse(todayLogs, last7DayLogs);
    }
}
EOF

cat > mapper/AIUsageLogMapper.java << 'EOF'
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
import com.enterprise.aiassistant.backend.ai.usage.helper.UsageHelpful;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AIUsageLogMapper {

    private final UsageHelpful specifications;

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
                .todaySuccessRate(specifications.calculateSuccessRate(todayLogs))
                .last7DayRequests(last7DayLogs.size())
                .last7DayTokens(sumTokens(last7DayLogs))
                .last7DayCost(sumCost(last7DayLogs))
                .last7DaySuccessRate(specifications.calculateSuccessRate(last7DayLogs))
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
EOF

echo "DONE: 3 file đã được ghi đè/đổi tên."
