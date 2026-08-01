package com.enterprise.aiassistant.backend.processing.event;

import java.util.List;

public record DocumentVersionCreatedBatchEvent(
        List<Long> versionIds
) {
}
