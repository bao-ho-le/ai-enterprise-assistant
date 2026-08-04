package com.enterprise.aiassistant.backend.folder.entity;

import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "folders",
        indexes = {
                @Index(
                        name = "idx_folder_parent_id",
                        columnList = "parent_id"),
                @Index(
                        name = "idx_folder_status",
                        columnList = "status"),
                @Index(
                        name = "idx_folder_created_at",
                        columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    // null = root folder (không có cha)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private FolderStatus status = FolderStatus.ACTIVE;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(
            mappedBy = "parent",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Folder> children = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
