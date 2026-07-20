package com.enterprise.aiassistant.backend.ai.usage.helper;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;

public class AIUsageLogSpecifications {

    private AIUsageLogSpecifications() {}

    // Filter dùng để lấy các log kể từ đầu tháng (phục vụ getSummary()).
    public static AIUsageLogFilterRequest monthFilter(LocalDateTime from) {
        var filter = new AIUsageLogFilterRequest();
        filter.setFromDate(from);
        return filter;
    }

    // Tỉ lệ % request thành công, tránh chia cho 0.
    public static double calculateSuccessRate(long successCount, long totalRequests) {
        return totalRequests == 0
                ? 0
                : (double) successCount / totalRequests * 100;
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
                predicates = cb.and(predicates,
                        cb.equal(root.get("featureType"), filter.getFeatureType()));
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