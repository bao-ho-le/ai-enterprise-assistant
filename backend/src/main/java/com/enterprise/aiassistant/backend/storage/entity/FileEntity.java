package com.enterprise.aiassistant.backend.storage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "files",
        indexes = {
                @Index(
                        name = "idx_file_checksum",
                        columnList = "checksum"),
                @Index(
                        name = "idx_file_object_key",
                        columnList = "object_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_file_id")
    private FileEntity previewFile;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider;

    @Column(name = "bucket_name", length = 255)
    private String bucketName;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "extension", length = 50)
    private String extension;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum", length = 255)
    private String checksum;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
