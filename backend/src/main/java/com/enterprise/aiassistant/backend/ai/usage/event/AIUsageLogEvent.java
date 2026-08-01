package com.enterprise.aiassistant.backend.ai.usage.event;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;

public record AIUsageLogEvent(AIUsageLogRequest request) {
}
