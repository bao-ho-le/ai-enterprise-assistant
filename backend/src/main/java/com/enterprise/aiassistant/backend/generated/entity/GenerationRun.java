package com.enterprise.aiassistant.backend.generated.entity;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedType;
import com.enterprise.aiassistant.backend.generated.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "generation_runs",
        indexes = {
                @Index(name = "idx_generation_run_conversation_id", columnList = "ai_conversation_id"),
                @Index(name = "idx_generation_run_content_id", columnList = "generated_content_id"),
                @Index(name = "idx_generation_run_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_conversation_id", nullable = false)
    private AIConversation aiConversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", nullable = false)
    private GeneratedType generatedType;

    @Column(length = 500)
    private String title;

    @Column(name = "user_prompt", columnDefinition = "text")
    private String userPrompt;

    // No JsonNode/@Convert infra in this project; stored as raw JSON text like other text columns.
    @Column(name = "input_data", columnDefinition = "json")
    private String inputData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_content_id")
    private GeneratedContent generatedContent;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
