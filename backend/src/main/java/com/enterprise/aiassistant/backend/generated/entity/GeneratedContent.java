package com.enterprise.aiassistant.backend.generated.entity;

import com.enterprise.aiassistant.backend.generated.enums.GeneratedType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "generated_content",
        indexes = {
                @Index(name = "idx_generated_content_conversation_id", columnList = "ai_conversation_id"),
                @Index(name = "idx_generated_content_type", columnList = "generated_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", nullable = false)
    private GeneratedType generatedType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Unidirectional: GenerationRun has no back-reference, nothing navigates child -> parent.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "generated_content_id")
    @Builder.Default
    private List<GenerationRun> generationRuns = new ArrayList<>();
}
