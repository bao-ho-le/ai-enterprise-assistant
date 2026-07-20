package com.enterprise.aiassistant.backend.ai.usage.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;

public interface AIUsageLogService {

    // Gọi từ các module AI khác (write-email, summary, write-report, document-qa)
    // sau mỗi lần gọi model để ghi log — không expose qua controller.
    void log(
            String featureType,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal estimatedCost,
            AIUsageStatus status,
            String errorMessage
    );

    Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable);

    AIUsageSummaryResponse getSummary();
}