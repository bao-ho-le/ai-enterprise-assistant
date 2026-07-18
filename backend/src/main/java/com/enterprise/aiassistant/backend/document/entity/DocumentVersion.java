package com.enterprise.aiassistant.backend.document.entity;

import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_versions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"document_id", "version_number"}
        ),
        indexes = {
                @Index(
                        name = "idx_document_version_document_id",
                        columnList = "document_id"),
                @Index(
                        name = "idx_document_version_file_id",
                        columnList = "file_id"),
                @Index(
                        name = "idx_document_version_status",
                        columnList = "status"),
                @Index(
                        name = "idx_document_version_created_at",
                        columnList = "created_at")
        }
)
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, name = "version_number")
    private Integer versionNumber;

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_step")
    private ProcessingStep processingStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VersionStatus status = VersionStatus.PENDING;

    @Column(columnDefinition = "text", name = "change_note")
    private String changeNote;

    @Column(columnDefinition = "text", name = "error_message")
    private String errorMessage;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
