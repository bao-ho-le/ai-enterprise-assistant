package com.enterprise.aiassistant.backend.processing.orchestration.event;

import java.util.List;

public record DocumentVersionCreatedBatchEvent(
        List<Long> versionIds
) {
}
