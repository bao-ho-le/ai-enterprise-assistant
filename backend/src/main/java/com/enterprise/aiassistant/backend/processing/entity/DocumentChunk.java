package com.enterprise.aiassistant.backend.processing.entity;

import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_chunks",
        indexes = {
                @Index(name = "idx_document_chunks_version_id", columnList = "document_version_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_chunks_version_chunk_index",
                        columnNames = {"document_version_id", "chunk_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Version tài liệu mà chunk này thuộc về.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    /**
     * Thứ tự chunk trong document version.
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * Nội dung text của chunk.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Số trang nếu extract được từ PDF/DOCX.
     * MVP có thể để null.
     */
    @Column(name = "page_number")
    private Integer pageNumber;

    /**
     * Vị trí bắt đầu của chunk trong full text.
     */
    @Column(name = "start_char")
    private Integer startChar;

    /**
     * Vị trí kết thúc của chunk trong full text.
     */
    @Column(name = "end_char")
    private Integer endChar;

    /**
     * Số token ước lượng hoặc tính thật.
     * MVP có thể ước lượng content.length() / 4.
     */
    @Column(name = "token_count")
    private Integer tokenCount;

//    /**
//     * Embedding vector.
//     *
//     * MVP hiện tại có thể lưu dạng TEXT hoặc để nullable.
//     * Sau này nếu dùng pgvector thật thì đổi sang vector(1536)
//     * bằng migration/Flyway.
//     */
//    @Column(name = "embedding", columnDefinition = "TEXT")
//    private String embedding;
//
//    /**
//     * Tên model embedding.
//     * Ví dụ: text-embedding-3-small, bge-m3.
//     * MVP chưa embedding thật thì có thể null.
//     */
//    @Column(name = "embedding_model", length = 255)
//    private String embeddingModel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}