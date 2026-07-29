package com.enterprise.aiassistant.backend.ai.generation.entity;

import com.enterprise.aiassistant.backend.ai.generation.enums.GeneratedDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;

import jakarta.persistence.OneToOne;

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

    // Inverse side only - Generation owns the FK. GeneratedContent is no longer a direct
    // child of AIConversation (item 4); its conversation is reached via generation.aiConversation.
    @OneToOne(mappedBy = "generatedContent", fetch = FetchType.LAZY)
    private Generation generation;

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
