package com.enterprise.aiassistant.backend.document.mapper;

import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDetailResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentMoveResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentRestoreResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DocumentMapper}: field-by-field mapping of every response type plus the
 * download response headers.
 */
class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void shouldMapUploadRequestToDocument() {

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle("Quarterly report");
        request.setDescription("Q1 numbers");
        request.setDocumentType(DocumentType.REPORT);

        Folder folder = Folder.builder().id(3L).name("root").build();
        User owner = User.builder().id(7L).fullName("Alice").build();
        Department department = Department.builder().id(2L).name("IT").build();

        Document document = mapper.toDocument(request, folder, owner, department);

        assertThat(document.getTitle()).isEqualTo("Quarterly report");
        assertThat(document.getDescription()).isEqualTo("Q1 numbers");
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.REPORT);
        assertThat(document.getFolder()).isSameAs(folder);
        assertThat(document.getOwner()).isSameAs(owner);
        assertThat(document.getDepartment()).isSameAs(department);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
    }

    @Test
    void shouldMapDocumentVersion() {

        Document document = Document.builder().id(1L).build();
        FileEntity file = FileEntity.builder().id(9L).originalFilename("a.pdf").build();

        DocumentVersion version = mapper.toDocumentVersion(document, file, 4, "note");

        assertThat(version.getDocument()).isSameAs(document);
        assertThat(version.getFile()).isSameAs(file);
        assertThat(version.getVersionNumber()).isEqualTo(4);
        assertThat(version.getChangeNote()).isEqualTo("note");
        assertThat(version.getStatus()).isEqualTo(VersionStatus.PENDING);
    }

    @Test
    void shouldMapUploadResponseUsingCurrentVersion() {

        DocumentVersion version = DocumentVersion.builder().id(11L).versionNumber(2).build();
        Document document = Document.builder()
                .id(1L)
                .title("report")
                .currentVersion(version)
                .status(DocumentStatus.ACTIVE)
                .build();
        FileEntity file = FileEntity.builder().originalFilename("report.pdf").build();

        DocumentUploadResponse response = mapper.toUploadResponse(document, file);

        assertThat(response.getDocumentId()).isEqualTo(1L);
        assertThat(response.getCurrentVersionId()).isEqualTo(11L);
        assertThat(response.getTitle()).isEqualTo("report");
        assertThat(response.getFilename()).isEqualTo("report.pdf");
        assertThat(response.getVersionNumber()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
    }

    @Test
    void shouldMapNewVersionResponse() {

        Document document = Document.builder().id(1L).build();
        DocumentVersion version = DocumentVersion.builder()
                .id(12L)
                .versionNumber(3)
                .changeNote("fix")
                .status(VersionStatus.READY)
                .build();
        FileEntity file = FileEntity.builder().originalFilename("v3.pdf").build();

        UploadNewVersionResponse response = mapper.toUploadNewVersionResponse(document, version, file);

        assertThat(response.getDocumentId()).isEqualTo(1L);
        assertThat(response.getCurrentVersionId()).isEqualTo(12L);
        assertThat(response.getFilename()).isEqualTo("v3.pdf");
        assertThat(response.getVersionNumber()).isEqualTo(3);
        assertThat(response.getChangeNote()).isEqualTo("fix");
        assertThat(response.getStatus()).isEqualTo("READY");
    }

    @Test
    void shouldMapRestoreResponse() {

        LocalDateTime deletedAt = LocalDateTime.of(2025, 1, 2, 3, 4);

        Document document = Document.builder()
                .id(5L)
                .title("restored")
                .documentType(DocumentType.CONTRACT)
                .status(DocumentStatus.ACTIVE)
                .deletedAt(deletedAt)
                .build();

        DocumentRestoreResponse response = mapper.toRestoreResponse(document);

        assertThat(response.getDocumentId()).isEqualTo(5L);
        assertThat(response.getTitle()).isEqualTo("restored");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.CONTRACT);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(response.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void shouldMapMetadataUpdateResponse() {

        Document document = Document.builder()
                .id(6L)
                .title("new title")
                .description("new description")
                .documentType(DocumentType.FORM)
                .build();

        DocumentUpdateMetadataResponse response = mapper.toUpdateMetadataReponse(document);

        assertThat(response.getDocumentId()).isEqualTo(6L);
        assertThat(response.getTitle()).isEqualTo("new title");
        assertThat(response.getDescription()).isEqualTo("new description");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.FORM);
    }

    @Test
    void shouldMapMoveResponse() {

        Document document = Document.builder().id(8L).build();
        Folder folder = Folder.builder().id(4L).build();

        DocumentMoveResponse response = mapper.toDocumentMoveResponse(document, folder);

        assertThat(response.getDocumentId()).isEqualTo(8L);
        assertThat(response.getFolderId()).isEqualTo(4L);
    }

    @Test
    void shouldMapDownloadResource() {

        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        FileEntity file = FileEntity.builder()
                .originalFilename("file.pdf")
                .mimeType("application/pdf")
                .fileSize(3L)
                .build();

        DocumentDownloadResource downloadResource = mapper.toDocumentDownloadResource(resource, file);

        assertThat(downloadResource.getResource()).isSameAs(resource);
        assertThat(downloadResource.getOriginalFilename()).isEqualTo("file.pdf");
        assertThat(downloadResource.getMimeType()).isEqualTo("application/pdf");
        assertThat(downloadResource.getFileSize()).isEqualTo(3L);
    }

    @Test
    void shouldMapDocumentInfoWithOwner() {

        LocalDateTime created = LocalDateTime.of(2024, 9, 1, 10, 0);

        Document document = Document.builder()
                .title("t")
                .description("d")
                .status(DocumentStatus.ACTIVE)
                .documentType(DocumentType.REPORT)
                .createdAt(created)
                .updatedAt(created.plusDays(1))
                .owner(User.builder().id(3L).fullName("Owner Name").build())
                .build();

        DocumentDetailResponse.DocumentInfo info = mapper.toDocumentInfo(document);

        assertThat(info.title()).isEqualTo("t");
        assertThat(info.description()).isEqualTo("d");
        assertThat(info.status()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(info.documentType()).isEqualTo(DocumentType.REPORT);
        assertThat(info.createdAt()).isEqualTo(created);
        assertThat(info.updatedAt()).isEqualTo(created.plusDays(1));
        assertThat(info.ownerId()).isEqualTo(3L);
        assertThat(info.ownerName()).isEqualTo("Owner Name");
    }

    @Test
    void shouldTolerateMissingOwnerInDocumentInfo() {

        DocumentDetailResponse.DocumentInfo info = mapper.toDocumentInfo(
                Document.builder().title("orphan").build());

        assertThat(info.ownerId()).isNull();
        assertThat(info.ownerName()).isNull();
    }

    @Test
    void shouldMapCurrentVersionInfo() {

        LocalDateTime uploaded = LocalDateTime.of(2024, 8, 8, 8, 8);

        DocumentVersion version = DocumentVersion.builder()
                .id(21L)
                .versionNumber(2)
                .status(VersionStatus.READY)
                .createdAt(uploaded)
                .build();
        FileEntity file = FileEntity.builder()
                .originalFilename("v2.docx")
                .extension("docx")
                .fileSize(2048L)
                .build();

        DocumentDetailResponse.CurrentVersionInfo info = mapper.toCurrentVersionInfo(version, file);

        assertThat(info.versionId()).isEqualTo(21L);
        assertThat(info.versionNumber()).isEqualTo(2);
        assertThat(info.versionStatus()).isEqualTo(VersionStatus.READY);
        assertThat(info.fileName()).isEqualTo("v2.docx");
        assertThat(info.extension()).isEqualTo("docx");
        assertThat(info.fileSize()).isEqualTo(2048L);
        assertThat(info.uploadedDate()).isEqualTo(uploaded);
    }

    @Test
    void shouldMapVersionHistoryNewestFirst() {

        DocumentVersion v1 = versionWithNumber(1, "a.pdf");
        DocumentVersion v3 = versionWithNumber(3, "c.pdf");
        DocumentVersion v2 = versionWithNumber(2, "b.pdf");

        List<DocumentDetailResponse.VersionHistoryItem> history =
                mapper.toVersionHistory(List.of(v1, v3, v2));

        assertThat(history).extracting(DocumentDetailResponse.VersionHistoryItem::versionNumber)
                .containsExactly(3, 2, 1);
        assertThat(history.get(0).fileName()).isEqualTo("c.pdf");
        assertThat(history.get(2).fileName()).isEqualTo("a.pdf");
    }

    @Test
    void shouldMapAdvancedInfo() {

        FileEntity file = FileEntity.builder()
                .storageProvider("MINIO")
                .bucketName("files")
                .objectKey("key-1")
                .checksum("abc123")
                .build();

        DocumentDetailResponse.AdvancedInfo info = mapper.toAdvancedInfo(file);

        assertThat(info.storageProvider()).isEqualTo("MINIO");
        assertThat(info.bucketName()).isEqualTo("files");
        assertThat(info.objectKey()).isEqualTo("key-1");
        assertThat(info.checksum()).isEqualTo("abc123");
    }

    @Test
    void shouldSetContentDispositionAndContentLengthOnDownload() {

        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        DocumentDownloadResource downloadResource = DocumentDownloadResource.builder()
                .resource(new ByteArrayResource(content))
                .originalFilename("báo cáo.pdf")
                .mimeType("application/pdf")
                .fileSize((long) content.length)
                .build();

        ResponseEntity<Resource> response = mapper.toDownloadResponse(downloadResource);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment;")
                .contains("filename*=");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getBody()).isSameAs(downloadResource.getResource());
    }

    @Test
    void shouldOmitContentLengthAndFallBackToOctetStreamWhenMissing() {

        DocumentDownloadResource downloadResource = DocumentDownloadResource.builder()
                .resource(new ByteArrayResource(new byte[]{1}))
                .originalFilename("file.bin")
                .mimeType("garbage mime")
                .fileSize(null)
                .build();

        ResponseEntity<Resource> response = mapper.toDownloadResponse(downloadResource);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(-1L);
    }

    @Test
    void shouldExposeListResponseFieldsUsedByTheQueryProjection() {

        DocumentListResponse response = DocumentListResponse.builder()
                .id(1L)
                .title("t")
                .folderId(2L)
                .ownerName("owner")
                .departmentName("dept")
                .deletedByName("deleter")
                .build();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFolderId()).isEqualTo(2L);
        assertThat(response.getOwnerName()).isEqualTo("owner");
        assertThat(response.getDepartmentName()).isEqualTo("dept");
        assertThat(response.getDeletedByName()).isEqualTo("deleter");
    }

    // ===================== helpers =====================

    private static DocumentVersion versionWithNumber(int number, String filename) {

        return DocumentVersion.builder()
                .id((long) number)
                .versionNumber(number)
                .status(VersionStatus.READY)
                .changeNote("note " + number)
                .createdAt(LocalDateTime.of(2024, 1, number, 0, 0))
                .file(FileEntity.builder().originalFilename(filename).build())
                .build();
    }
}
