package com.enterprise.aiassistant.backend.ai.usage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageDailyResponse {

    private LocalDate date;
    private long requestCount;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private BigDecimal cost;
    private long successCount;
    private long failedCount;
}
