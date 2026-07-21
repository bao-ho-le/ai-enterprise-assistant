package com.enterprise.aiassistant.backend.ai.usage.helper;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;

public class AIUsageLogSpecifications {

    private AIUsageLogSpecifications() {}

    public static AIUsageLogFilterRequest fromDateFilter(LocalDateTime from) {
        var filter = new AIUsageLogFilterRequest();
        filter.setFromDate(from);
        return filter;
    }

    public static double calculateSuccessRate(long successCount, long totalRequests) {
        return totalRequests == 0
                ? 0
                : (double) successCount / totalRequests * 100;
    }

    public static double calculateSuccessRate(List<AIUsageLog> logs) {
        if (logs.isEmpty()) {
            return 0;
        }
        long successCount = logs.stream()
                .filter(l -> l.getStatus() == AIUsageStatus.SUCCESS)
                .count();
        return (double) successCount / logs.size() * 100;
    }

    public static Specification<AIUsageLog> byFilter(AIUsageLogFilterRequest filter) {
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
            if (StringUtils.hasText(filter.getFeatureType())) {
                // ✅ Sửa: convert String -> enum ConversationType và query đúng field "conversationType"
                try {
                    var conversationType = ConversationType.valueOf(filter.getFeatureType());
                    predicates = cb.and(predicates,
                            cb.equal(root.get("conversationType"), conversationType));
                } catch (IllegalArgumentException ex) {
                    // Giá trị featureType không khớp enum nào -> không match record nào
                    predicates = cb.and(predicates, cb.disjunction());
                }
            }
            if (StringUtils.hasText(filter.getModel())) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("model"), filter.getModel()));
            }
            if (filter.getStatus() != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), filter.getStatus()));
            }
            return predicates;
        };
    }
}