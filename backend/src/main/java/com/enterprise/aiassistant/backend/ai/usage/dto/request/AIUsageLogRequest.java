package com.enterprise.aiassistant.backend.ai.usage.dto.request;

import java.math.BigDecimal;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO dùng nội bộ, các module AI khác (write-email, summary, write-report, document-qa)
// build request này sau mỗi lần gọi model rồi truyền vào AIUsageLogService.log(...).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageLogRequest {

    private ConversationType conversationType;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCost;
    private AIUsageStatus status;
    private String errorMessage;
}