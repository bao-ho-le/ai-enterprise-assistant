package com.enterprise.aiassistant.backend.ai.conversation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "ai_message_sources",
        indexes = {
                @Index(
                        name = "idx_ai_message_sources_message_id",
                        columnList = "ai_message_id"
                ),
                @Index(
                        name = "idx_ai_message_sources_document_version_id",
                        columnList = "document_version_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Assistant message sử dụng source này.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AIMessage message;

    /**
     * Document version chứa chunk được retrieve.
     */
    @Column(name = "document_version_id", nullable = false)
    private Long documentVersionId;

    /**
     * Chunk được retrieve.
     */
    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    /**
     * Similarity score của retriever.
     */
    @Column(name = "score")
    private Double score;
}
