package com.enterprise.aiassistant.backend.document.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.config.FileUploadProperties;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentHelper}: request validation, status guards, version numbering
 * and media-type resolution. No Spring context, no database.
 */
class DocumentHelperTest {

    private static final int MAX_FILES = 10;

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentVersionRepository versionRepository = mock(DocumentVersionRepository.class);

    private final DocumentMapper documentMapper = mock(DocumentMapper.class);

    private DocumentHelper helper;

    @BeforeEach
    void setUp() {
        helper = new DocumentHelper(new FileUploadProperties(), versionRepository, documentMapper);
    }

    // ===================== files =====================

    @Nested
    @DisplayName("file validation")
    class FileValidation {

        @Test
        void shouldRejectNullFileList() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFiles(null)),
                    ErrorCode.FILE_REQUIRED
            );
        }

        @Test
        void shouldRejectEmptyFileList() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFiles(List.of())),
                    ErrorCode.EMPTY_FILE
            );
        }

        @Test
        void shouldRejectMoreThanTenFiles() {

            List<MultipartFile> files = java.util.stream.IntStream.range(0, MAX_FILES + 1)
                    .mapToObj(index -> pdfFile("file-" + index + ".pdf"))
                    .map(MultipartFile.class::cast)
                    .toList();

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFiles(files)),
                    ErrorCode.TOO_MANY_FILES
            );
        }

        @Test
        void shouldAcceptExactlyTenValidFiles() {

            List<MultipartFile> files = java.util.stream.IntStream.range(0, MAX_FILES)
                    .mapToObj(index -> pdfFile("file-" + index + ".pdf"))
                    .map(MultipartFile.class::cast)
                    .toList();

            assertThat(catchThrowable(() -> helper.validateFiles(files))).isNull();
        }

        @Test
        void shouldRejectNullFile() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFile(null)),
                    ErrorCode.FILE_REQUIRED
            );
        }

        @Test
        void shouldRejectEmptyFile() {

            MockMultipartFile emptyFile = new MockMultipartFile(
                    "files", "empty.pdf", "application/pdf", new byte[0]);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFile(emptyFile)),
                    ErrorCode.EMPTY_FILE
            );
        }

        @Test
        void shouldRejectFileLargerThanFiftyMegabytes() {

            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(MAX_FILE_SIZE + 1);
            when(file.getContentType()).thenReturn("application/pdf");

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFile(file)),
                    ErrorCode.FILE_TOO_LARGE
            );
        }

        @Test
        void shouldAcceptFileExactlyAtTheSizeLimit() {

            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(MAX_FILE_SIZE);
            when(file.getContentType()).thenReturn("application/pdf");

            assertThat(catchThrowable(() -> helper.validateFile(file))).isNull();
        }

        @Test
        void shouldRejectUnsupportedContentType() {

            MockMultipartFile video = new MockMultipartFile(
                    "files", "movie.mp4", "video/mp4", new byte[]{1, 2, 3});

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFile(video)),
                    ErrorCode.UNSUPPORTED_FILE_TYPE
            );
        }

        @Test
        void shouldFailWithNullPointerExceptionWhenContentTypeIsMissing() {

            MockMultipartFile noContentType = new MockMultipartFile(
                    "files", "file.pdf", null, new byte[]{1});

            // Known production issue (documented in docs/TEST_COVERAGE_REPORT.md):
            // DocumentHelper.validateContentType() calls ALLOWED_TYPES.contains(contentType)
            // on an immutable Set.of(...), which throws NullPointerException for a null
            // content type instead of raising ErrorCode.UNSUPPORTED_FILE_TYPE.
            // The test pins the current behaviour so a fix is a deliberate change.
            assertThat(catchThrowable(() -> helper.validateFile(noContentType)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldAcceptEverySupportedContentType() {

            List<String> supported = List.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain"
            );

            supported.forEach(contentType -> {
                MockMultipartFile file = new MockMultipartFile("files", "doc", contentType, new byte[]{1});
                assertThat(catchThrowable(() -> helper.validateFile(file)))
                        .as(contentType)
                        .isNull();
            });
        }
    }

    // ===================== batch metadata =====================

    @Nested
    @DisplayName("batch request validation")
    class BatchValidation {

        @Test
        void shouldRejectMissingMetadataList() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateBatchRequest(List.of(pdfFile("a.pdf")), null)),
                    ErrorCode.DOCUMENTS_METADATA_REQUIRED
            );
        }

        @Test
        void shouldRejectEmptyMetadataList() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateBatchRequest(List.of(pdfFile("a.pdf")), List.of())),
                    ErrorCode.DOCUMENTS_METADATA_REQUIRED
            );
        }

        @Test
        void shouldRejectFileCountMismatch() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateBatchRequest(
                            List.of(pdfFile("a.pdf"), pdfFile("b.pdf")),
                            List.of(uploadRequest("only one", DocumentType.REPORT))
                    )),
                    ErrorCode.FILE_METADATA_MISMATCH
            );
        }

        @Test
        void shouldRejectBlankTitle() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateBatchRequest(
                            List.of(pdfFile("a.pdf")),
                            List.of(uploadRequest("   ", DocumentType.REPORT))
                    )),
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }

        @Test
        void shouldRejectMissingDocumentType() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateBatchRequest(
                            List.of(pdfFile("a.pdf")),
                            List.of(uploadRequest("title", null))
                    )),
                    ErrorCode.DOCUMENT_TYPE_REQUIRED
            );
        }

        @Test
        void shouldAcceptMatchingFilesAndMetadata() {

            assertThat(catchThrowable(() -> helper.validateBatchRequest(
                    List.of(pdfFile("a.pdf"), pdfFile("b.pdf")),
                    List.of(
                            uploadRequest("a", DocumentType.REPORT),
                            uploadRequest("b", DocumentType.CONTRACT)
                    )
            ))).isNull();
        }

        @Test
        void shouldRejectNullSingleRequest() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateRequest(null)),
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        @Test
        void shouldRejectSingleRequestWithoutType() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateRequest(uploadRequest("title", null))),
                    ErrorCode.INVALID_DOCUMENT_TYPE
            );
        }

        @Test
        void shouldRejectSingleRequestWithBlankTitle() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateRequest(uploadRequest(" ", DocumentType.REPORT))),
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }
    }

    // ===================== identifier / status =====================

    @Nested
    @DisplayName("identifier and status validation")
    class IdentifierAndStatus {

        @Test
        void shouldRejectNullDocumentId() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentId(null)),
                    ErrorCode.DOCUMENT_ID_REQUIRED
            );
        }

        @Test
        void shouldRejectZeroAndNegativeDocumentId() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentId(0L)),
                    ErrorCode.DOCUMENT_ID_INVALID
            );

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentId(-5L)),
                    ErrorCode.DOCUMENT_ID_INVALID
            );
        }

        @Test
        void shouldAcceptPositiveDocumentId() {

            assertThat(catchThrowable(() -> helper.validateDocumentId(1L))).isNull();
        }

        @Test
        void shouldRejectNullVersionId() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentVersion(null)),
                    ErrorCode.DOCUMENT_VERSION_NOT_FOUND
            );
        }

        @Test
        void shouldRejectDeletedDocument() {

            Document deleted = document(DocumentStatus.DELETED);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentStatus(deleted)),
                    ErrorCode.DOCUMENT_DELETED
            );
        }

        @Test
        void shouldAcceptActiveDocument() {

            assertThat(catchThrowable(() -> helper.validateDocumentStatus(document(DocumentStatus.ACTIVE))))
                    .isNull();
        }

        @Test
        void shouldRejectRestoreWhenDocumentIsNotDeleted() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateDocumentIsDeleted(document(DocumentStatus.ACTIVE))),
                    ErrorCode.DOCUMENT_NOT_DELETED
            );
        }

        @Test
        void shouldAcceptRestoreWhenDocumentIsDeleted() {

            assertThat(catchThrowable(() -> helper.validateDocumentIsDeleted(document(DocumentStatus.DELETED))))
                    .isNull();
        }
    }

    // ===================== filter =====================

    @Nested
    @DisplayName("filter validation")
    class FilterValidation {

        @Test
        void shouldIgnoreNullFilter() {

            assertThat(catchThrowable(() -> helper.validateFilter(null))).isNull();
        }

        @Test
        void shouldRejectUnknownSortOption() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setSort("alphabetical");

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(request)),
                    ErrorCode.INVALID_SORT_OPTION
            );
        }

        @Test
        void shouldAcceptSortOptionInAnyCase() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setSort("NEwest");

            assertThat(catchThrowable(() -> helper.validateFilter(request))).isNull();
        }

        @Test
        void shouldRejectInvertedDateRange() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setFromDate(LocalDateTime.of(2025, 5, 1, 0, 0));
            request.setToDate(LocalDateTime.of(2025, 4, 1, 0, 0));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(request)),
                    ErrorCode.INVALID_DATE_RANGE
            );
        }

        @Test
        void shouldRejectNegativeSizeBounds() {

            DocumentFilterRequest negativeMin = new DocumentFilterRequest();
            negativeMin.setMinSize(-1L);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(negativeMin)),
                    ErrorCode.INVALID_FILE_SIZE
            );

            DocumentFilterRequest negativeMax = new DocumentFilterRequest();
            negativeMax.setMaxSize(-1L);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(negativeMax)),
                    ErrorCode.INVALID_FILE_SIZE
            );
        }

        @Test
        void shouldRejectInvertedSizeRange() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setMinSize(1000L);
            request.setMaxSize(10L);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(request)),
                    ErrorCode.INVALID_FILE_SIZE_RANGE
            );
        }

        @Test
        void shouldRejectKeywordLongerThan255Characters() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setKeyword("x".repeat(256));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFilter(request)),
                    ErrorCode.KEYWORD_TOO_LONG
            );
        }

        @Test
        void shouldAcceptKeywordOfExactly255Characters() {

            DocumentFilterRequest request = new DocumentFilterRequest();
            request.setKeyword("x".repeat(255));

            assertThat(catchThrowable(() -> helper.validateFilter(request))).isNull();
        }
    }

    // ===================== version request / metadata =====================

    @Nested
    @DisplayName("version and metadata request validation")
    class RequestValidation {

        @Test
        void shouldRejectNullVersionRequest() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateVersionRequest(null)),
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        @Test
        void shouldRejectChangeNoteLongerThan255Characters() {

            UploadNewVersionRequest request = new UploadNewVersionRequest();
            request.setChangeNote("n".repeat(256));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateVersionRequest(request)),
                    ErrorCode.CHANGE_NOTE_TOO_LONG
            );
        }

        @Test
        void shouldRejectBlankTitleInVersionRequest() {

            UploadNewVersionRequest request = new UploadNewVersionRequest();
            request.setTitle("   ");

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateVersionRequest(request)),
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }

        @Test
        void shouldRejectTooLongTitleAndDescription() {

            UploadNewVersionRequest longTitle = new UploadNewVersionRequest();
            longTitle.setTitle("t".repeat(256));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateVersionRequest(longTitle)),
                    ErrorCode.TITLE_TOO_LONG
            );

            UploadNewVersionRequest longDescription = new UploadNewVersionRequest();
            longDescription.setDescription("d".repeat(1001));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateVersionRequest(longDescription)),
                    ErrorCode.DESCRIPTION_TOO_LONG
            );
        }

        @Test
        void shouldAcceptVersionRequestWithOnlyChangeNote() {

            UploadNewVersionRequest request = new UploadNewVersionRequest();
            request.setChangeNote("fixed typo");

            assertThat(catchThrowable(() -> helper.validateVersionRequest(request))).isNull();
        }

        @Test
        void shouldRejectNullUpdateMetadataRequest() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateUpdateMetadataRequest(null)),
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        @Test
        void shouldValidatePresentMetadataFieldsOnly() {

            DocumentUpdateMetadataRequest empty = new DocumentUpdateMetadataRequest();

            assertThat(catchThrowable(() -> helper.validateMetadata(empty))).isNull();

            DocumentUpdateMetadataRequest blankTitle = new DocumentUpdateMetadataRequest();
            blankTitle.setTitle("");

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateMetadata(blankTitle)),
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );

            DocumentUpdateMetadataRequest longDescription = new DocumentUpdateMetadataRequest();
            longDescription.setDescription("d".repeat(1001));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateMetadata(longDescription)),
                    ErrorCode.DESCRIPTION_TOO_LONG
            );
        }
    }

    // ===================== storage metadata / media type =====================

    @Nested
    @DisplayName("storage metadata and media type")
    class StorageMetadata {

        @Test
        void shouldRejectMissingFileEntity() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFileStorageMetadata(null)),
                    ErrorCode.FILE_NOT_FOUND
            );
        }

        @Test
        void shouldRejectIncompleteStorageMetadata() {

            FileEntity blankBucket = FileEntity.builder().bucketName(" ").objectKey("key").build();

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFileStorageMetadata(blankBucket)),
                    ErrorCode.FILE_STORAGE_METADATA_INVALID
            );

            FileEntity blankKey = FileEntity.builder().bucketName("bucket").objectKey(null).build();

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> helper.validateFileStorageMetadata(blankKey)),
                    ErrorCode.FILE_STORAGE_METADATA_INVALID
            );
        }

        @Test
        void shouldReturnStorageExceptionTypeForMissingFile() {

            Throwable thrown = catchThrowable(() -> helper.validateFileStorageMetadata(null));

            assertThat(thrown).isInstanceOf(FileStorageException.class);
        }

        @Test
        void shouldAcceptCompleteStorageMetadata() {

            FileEntity file = FileEntity.builder().bucketName("bucket").objectKey("key").build();

            assertThat(catchThrowable(() -> helper.validateFileStorageMetadata(file))).isNull();
        }

        @Test
        void shouldFallBackToOctetStreamForMissingOrInvalidMediaType() {

            assertThat(DocumentHelper.resolveMediaType(null)).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
            assertThat(DocumentHelper.resolveMediaType(" ")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
            assertThat(DocumentHelper.resolveMediaType("not a media type"))
                    .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }

        @Test
        void shouldResolveKnownMediaType() {

            assertThat(DocumentHelper.resolveMediaType("application/pdf"))
                    .isEqualTo(MediaType.APPLICATION_PDF);
        }
    }

    // ===================== version creation =====================

    @Nested
    @DisplayName("version creation")
    class VersionCreation {

        @Test
        void shouldStartAtVersionOneWhenNoVersionExists() {

            Document document = document(DocumentStatus.ACTIVE);
            document.setId(7L);

            FileEntity file = FileEntity.builder().objectKey("key").build();
            DocumentVersion expected = DocumentVersion.builder().versionNumber(1).build();

            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(7L))
                    .thenReturn(Optional.empty());
            when(documentMapper.toDocumentVersion(document, file, 1, "note")).thenReturn(expected);

            assertThat(helper.createNewVersion(document, file, "note")).isSameAs(expected);
        }

        @Test
        void shouldIncrementTheHighestExistingVersionNumber() {

            Document document = document(DocumentStatus.ACTIVE);
            document.setId(9L);

            FileEntity file = FileEntity.builder().objectKey("key").build();
            DocumentVersion current = DocumentVersion.builder().versionNumber(3).build();
            DocumentVersion expected = DocumentVersion.builder().versionNumber(4).build();

            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(9L))
                    .thenReturn(Optional.of(current));
            when(documentMapper.toDocumentVersion(document, file, 4, null)).thenReturn(expected);

            assertThat(helper.createNewVersion(document, file, null)).isSameAs(expected);
            verify(documentMapper).toDocumentVersion(document, file, 4, null);
            verify(documentMapper, never()).toDocumentVersion(any(), any(), eq(1), anyString());
        }
    }

    // ===================== helpers =====================

    private static MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile("files", name, "application/pdf", new byte[]{1, 2, 3});
    }

    private static DocumentUploadRequest uploadRequest(String title, DocumentType type) {

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle(title);
        request.setDocumentType(type);

        return request;
    }

    private static Document document(DocumentStatus status) {

        return Document.builder()
                .id(1L)
                .title("doc")
                .status(status)
                .build();
    }
}
