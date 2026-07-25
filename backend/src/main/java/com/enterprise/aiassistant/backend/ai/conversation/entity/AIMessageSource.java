package com.enterprise.aiassistant.backend.ai.conversation.entity;

import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "ai_message_sources",
        indexes = {
                @Index(name = "idx_ai_msg_source_message_id", columnList = "ai_message_id"),
                @Index(name = "idx_ai_msg_source_chunk_id", columnList = "document_chunk_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AIMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
