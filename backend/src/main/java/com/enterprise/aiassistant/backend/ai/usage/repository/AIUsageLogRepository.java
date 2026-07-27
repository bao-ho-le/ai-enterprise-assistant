package com.enterprise.aiassistant.backend.ai.usage.repository;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AIUsageLogRepository
        extends JpaRepository<AIUsageLog, Long>, JpaSpecificationExecutor<AIUsageLog> {

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    long countByStatus(AIUsageStatus status);

    // One row per calendar day (Postgres date cast), oldest first.
    @Query(value = """
            SELECT CAST(created_at AS date)                          AS day,
                   COUNT(*)                                           AS requestCount,
                   COALESCE(SUM(input_tokens), 0)                     AS inputTokens,
                   COALESCE(SUM(output_tokens), 0)                    AS outputTokens,
                   COALESCE(SUM(total_tokens), 0)                     AS totalTokens,
                   COALESCE(SUM(estimated_cost), 0)                   AS cost,
                   COUNT(*) FILTER (WHERE status = 'SUCCESS')         AS successCount,
                   COUNT(*) FILTER (WHERE status = 'FAILED')          AS failedCount
            FROM ai_usage_logs
            WHERE created_at >= :from
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<AIUsageDailyProjection> findDailyStats(@Param("from") LocalDateTime from);

    @Query("SELECT DISTINCT a.model FROM AIUsageLog a ORDER BY a.model")
    List<String> findDistinctModels();

    // AIUsageLog has no back-reference to AIConversation; set the FK directly instead of
    // loading conversation.getUsageLogs() (would pull every past log just to append one row).
    @Modifying
    @Query(value = "UPDATE ai_usage_logs SET ai_conversation_id = :conversationId WHERE id = :usageLogId",
            nativeQuery = true)
    void attachConversation(@Param("usageLogId") Long usageLogId, @Param("conversationId") Long conversationId);
}