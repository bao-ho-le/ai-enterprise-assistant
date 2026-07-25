package com.enterprise.aiassistant.backend.generated.entity;

import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "generated_content",
        indexes = {
                @Index(
                        name = "idx_generated_content_conversation_id",
                        columnList = "ai_conversation_id"
                ),
                @Index(
                        name = "idx_generated_content_type",
                        columnList = "generated_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Tạm thời lưu ID để module generated chưa phụ thuộc
     * trực tiếp vào AIConversation entity.
     *
     * Sau khi AIConversation được triển khai, có thể đổi thành:
     *
     * @ManyToOne(fetch = FetchType.LAZY, optional = false)
     * @JoinColumn(name = "ai_conversation_id", nullable = false)
     * private AIConversation aiConversation;
     */
    @Column(name = "ai_conversation_id", nullable = false)
    private Long aiConversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", nullable = false, length = 50)
    private GeneratedDocumentType generatedType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}