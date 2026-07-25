package com.enterprise.aiassistant.backend.ai.conversation.entity;

import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ai_messages",
        indexes = {
                @Index(name = "idx_ai_message_conversation_id", columnList = "ai_conversation_id"),
                @Index(name = "idx_ai_message_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_conversation_id", nullable = false)
    private AIConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AIMessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "message",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<AIMessageSource> sources = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
