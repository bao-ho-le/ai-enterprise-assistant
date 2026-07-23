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

    public int calculateTotalTokens(Integer inputTokens, Integer outputTokens) {
        int in = inputTokens != null ? inputTokens : 0;
        int out = outputTokens != null ? outputTokens : 0;
        return in + out;
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
