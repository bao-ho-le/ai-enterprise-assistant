package com.enterprise.aiassistant.backend.ai.usage.entity;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_usage_logs",
        indexes = {
                @Index(name = "idx_ai_usage_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_usage_feature_type", columnList = "feature_type"),
                @Index(name = "idx_ai_usage_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "WRITE_EMAIL" | "WRITE_REPORT" | "SUMMARY" | "DOCUMENT_QA"
    @Column(name = "feature_type", nullable = false, length = 50)
    private String featureType;

    // "gpt-4o" | "claude-sonnet" | "gemini-pro" ...
    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    @Builder.Default
    private Integer inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    @Builder.Default
    private Integer outputTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AIUsageStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (inputTokens != null && outputTokens != null) {
            totalTokens = inputTokens + outputTokens;
        }
    }
}