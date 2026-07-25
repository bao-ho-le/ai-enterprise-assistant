package com.enterprise.aiassistant.backend.ai.conversation.entity;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ai_conversations",
        indexes = {
                @Index(name = "idx_ai_conversation_type", columnList = "conversation_type"),
                @Index(name = "idx_ai_conversation_status", columnList = "status"),
                @Index(name = "idx_ai_conversation_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 50)
    private ConversationType conversationType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<AIMessage> messages = new ArrayList<>();

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<AIConversationDocument> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
