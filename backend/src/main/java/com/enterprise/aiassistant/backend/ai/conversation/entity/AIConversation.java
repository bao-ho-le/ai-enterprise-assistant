package com.enterprise.aiassistant.backend.ai.conversation.entity;

import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.entity.GenerationRun;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ai_conversations",
        indexes = {
                @Index(
                        name = "idx_ai_conversation_type",
                        columnList = "conversation_type"),
                @Index(
                        name = "idx_ai_conversation_created_at",
                        columnList = "created_at")
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

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "conversation_type", length = 50)
    private ConversationType conversationType;

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

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIConversationDocument> conversationDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIUsageLog> usageLogs = new ArrayList<>();

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GeneratedContent> generatedContents = new ArrayList<>();

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GenerationRun> generationRuns = new ArrayList<>();
}
