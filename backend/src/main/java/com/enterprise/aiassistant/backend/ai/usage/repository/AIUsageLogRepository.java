package com.enterprise.aiassistant.backend.ai.usage.repository;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface AIUsageLogRepository
        extends JpaRepository<AIUsageLog, Long>, JpaSpecificationExecutor<AIUsageLog> {

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    long countByStatus(AIUsageStatus status);
}