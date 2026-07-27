package com.enterprise.aiassistant.backend.ai.conversation.entity;

import com.enterprise.aiassistant.backend.ai.conversation.enums.AIMessageRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ai_messages",
        indexes = {
                @Index(name = "idx_ai_message_conversation_id", columnList = "ai_conversation_id")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIMessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "aiMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AIMessageSource> sources = new ArrayList<>();
}
