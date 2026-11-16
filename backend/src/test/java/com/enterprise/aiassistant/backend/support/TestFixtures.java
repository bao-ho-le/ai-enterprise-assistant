package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Role;

/**
 * Reusable entity builders for integration tests.
 *
 * <p>Only entity state is built here; persistence is done by the test through the real
 * repositories so that Hibernate/PostgreSQL behaviour is exercised.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Department department(String name) {

        return Department.builder()
                .name(name)
                .description("test department " + name)
                .build();
    }

    public static User user(String username, Role role, Department department) {

        return User.builder()
                .username(username)
                .email(username + "@test.local")
                .fullName("Test " + username)
                .password("not-used")
                .role(role)
                .department(department)
                .enabled(true)
                .build();
    }

    public static Folder folder(String name, Folder parent, User owner, Department department) {

        return Folder.builder()
                .name(name)
                .parent(parent)
                .owner(owner)
                .department(department)
                .status(FolderStatus.ACTIVE)
                .build();
    }

    public static FileEntity file(String originalFilename, String objectKey) {

        String extension = null;
        int dot = originalFilename.lastIndexOf('.');

        if (dot >= 0 && dot < originalFilename.length() - 1) {
            extension = originalFilename.substring(dot + 1).toLowerCase();
        }

        return FileEntity.builder()
                .originalFilename(originalFilename)
                .storedFilename(objectKey)
                .storageProvider("MINIO")
                .bucketName(MinioContainerSupport.BUCKET)
                .objectKey(objectKey)
                .mimeType("application/pdf")
                .extension(extension)
                .fileSize(1024L)
                .build();
    }

    public static Document document(
            String title,
            Folder folder,
            User owner,
            Department department,
            DocumentType type
    ) {

        return Document.builder()
                .title(title)
                .description("description of " + title)
                .folder(folder)
                .owner(owner)
                .department(department)
                .documentType(type)
                .status(DocumentStatus.ACTIVE)
                .build();
    }

    public static DocumentVersion version(
            Document document,
            FileEntity file,
            int versionNumber,
            VersionStatus status
    ) {

        return DocumentVersion.builder()
                .document(document)
                .file(file)
                .versionNumber(versionNumber)
                .changeNote("change note " + versionNumber)
                .status(status)
                .build();
    }
}
