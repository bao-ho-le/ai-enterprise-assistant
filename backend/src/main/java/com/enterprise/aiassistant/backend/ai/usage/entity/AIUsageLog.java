package com.enterprise.aiassistant.backend.ai.usage.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    @Column(name = "conversation_type", nullable = false, length = 50)
    private ConversationType conversationType;

    // "gpt-4o" | "claude-sonnet" | "gemini-pro" ...
    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens ;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens ;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AIUsageStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    
}