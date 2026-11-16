package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentBatchUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentFilterRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.MoveDocumentRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
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
import com.enterprise.aiassistant.backend.document.helper.DocumentHelper;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedBatchEvent;
import com.enterprise.aiassistant.backend.processing.orchestration.event.DocumentVersionCreatedEvent;
import com.enterprise.aiassistant.backend.storage.config.FileUploadProperties;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.mapper.FileMapper;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Orchestration tests for {@link DocumentServiceImpl}. The real {@link DocumentHelper} and
 * {@link DocumentMapper} are used so validation and mapping participate; repositories and
 * external services are mocked. Document sharing methods are out of scope.
 */
class DocumentServiceImplTest {

    private final FileStorageService fileStorageService = mock(FileStorageService.class);

    private final FileRepository fileRepository = mock(FileRepository.class);

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);

    private final DocumentVersionRepository versionRepository = mock(DocumentVersionRepository.class);

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final FolderService folderService = mock(FolderService.class);

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final DocumentAuthorizationService documentAuthorizationService =
            mock(DocumentAuthorizationService.class);

    private final DocumentAccessRepository documentAccessRepository = mock(DocumentAccessRepository.class);

    private final UserRepository userRepository = mock(UserRepository.class);

    private final DocumentMapper documentMapper = new DocumentMapper();

    private final FileMapper fileMapper = new FileMapper();

    private DocumentHelper documentHelper;

    private DocumentServiceImpl service;

    private final AtomicLong sequence = new AtomicLong(100);

    private Department itDepartment;

    private User owner;

    private Folder rootFolder;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(fileMapper, "bucket", "test-files");
        ReflectionTestUtils.setField(fileMapper, "provider", "MINIO");

        documentHelper = new DocumentHelper(new FileUploadProperties(), versionRepository, documentMapper);

        service = new DocumentServiceImpl(
                fileStorageService,
                fileRepository,
                documentRepository,
                versionRepository,
                documentMapper,
                fileMapper,
                documentHelper,
                eventPublisher,
                folderService,
                currentUserService,
                documentAuthorizationService,
                documentAccessRepository,
                userRepository
        );

        itDepartment = Department.builder().id(2L).name("IT").build();
        owner = User.builder().id(10L).username("alice").fullName("Alice").department(itDepartment).build();
        rootFolder = Folder.builder().id(1L).name("root").build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(folderService.resolveTargetFolder(any())).thenReturn(rootFolder);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
        when(versionRepository.save(any(DocumentVersion.class)))
                .thenAnswer(invocation -> assignId(invocation.getArgument(0)));
        when(fileRepository.save(any(FileEntity.class)))
                .thenAnswer(invocation -> assignId(invocation.getArgument(0)));
    }

    // ===================== upload =====================

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        void shouldPersistFileDocumentVersionAndPublishBatchEvent() {

            MultipartFile file = pdfFile("report.pdf");

            when(fileStorageService.store(file)).thenReturn(
                    StoredFileDto.builder()
                            .originalName("report.pdf")
                            .storedName("uuid-report.pdf")
                            .objectKey("uuid-report.pdf")
                            .bucket("test-files")
                            .contentType("application/pdf")
                            .size(3L)
                            .build()
            );
            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(anyLong()))
                    .thenReturn(Optional.empty());

            List<DocumentUploadResponse> responses = service.upload(
                    List.of(file),
                    batchRequest(uploadRequest("Quarterly report", DocumentType.REPORT))
            );

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getTitle()).isEqualTo("Quarterly report");
            assertThat(responses.get(0).getFilename()).isEqualTo("report.pdf");
            assertThat(responses.get(0).getVersionNumber()).isEqualTo(1);
            assertThat(responses.get(0).getStatus()).isEqualTo(DocumentStatus.ACTIVE);
            assertThat(responses.get(0).getCurrentVersionId()).isNotNull();

            ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getBucketName()).isEqualTo("test-files");
            assertThat(fileCaptor.getValue().getStorageProvider()).isEqualTo("MINIO");
            assertThat(fileCaptor.getValue().getExtension()).isEqualTo("pdf");

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(DocumentVersionCreatedBatchEvent.class);
            assertThat(((DocumentVersionCreatedBatchEvent) eventCaptor.getValue()).versionIds())
                    .containsExactly(responses.get(0).getCurrentVersionId());
        }

        @Test
        void shouldUseConfiguredFolderWhenProvided() {

            Folder target = Folder.builder().id(7L).name("contracts").build();
            when(folderService.resolveTargetFolder(7L)).thenReturn(target);
            when(fileStorageService.store(any())).thenReturn(storedFile("a.pdf"));
            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(anyLong()))
                    .thenReturn(Optional.empty());

            DocumentUploadRequest item = uploadRequest("in folder", DocumentType.OTHER);
            item.setFolderId(7L);

            service.upload(List.of(pdfFile("a.pdf")), batchRequest(item));

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, org.mockito.Mockito.atLeastOnce()).save(documentCaptor.capture());
            assertThat(documentCaptor.getAllValues())
                    .anySatisfy(document -> assertThat(document.getFolder()).isSameAs(target));
        }

        @Test
        void shouldRejectEmptyFileListBeforeTouchingStorage() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.upload(
                            List.of(),
                            batchRequest(uploadRequest("t", DocumentType.REPORT)))),
                    ErrorCode.EMPTY_FILE
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldRejectMismatchedMetadataBeforeTouchingStorage() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.upload(
                            List.of(pdfFile("a.pdf"), pdfFile("b.pdf")),
                            batchRequest(uploadRequest("only one", DocumentType.REPORT)))),
                    ErrorCode.FILE_METADATA_MISMATCH
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldPropagateAuthorizationFailureBeforeUploading() {

            doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                    .when(documentAuthorizationService).requireCreate();

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.upload(
                            List.of(pdfFile("a.pdf")),
                            batchRequest(uploadRequest("t", DocumentType.REPORT)))),
                    ErrorCode.PERMISSION_DENIED
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldPublishOneVersionIdPerUploadedFile() {

            when(fileStorageService.store(any())).thenReturn(storedFile("x.pdf"));
            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(anyLong()))
                    .thenReturn(Optional.empty());

            List<DocumentUploadResponse> responses = service.upload(
                    List.of(pdfFile("a.pdf"), pdfFile("b.pdf")),
                    batchRequest(
                            uploadRequest("a", DocumentType.REPORT),
                            uploadRequest("b", DocumentType.CONTRACT)
                    )
            );

            assertThat(responses).hasSize(2);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(((DocumentVersionCreatedBatchEvent) eventCaptor.getValue()).versionIds())
                    .hasSize(2);
        }
    }

    // ===================== new version =====================

    @Nested
    @DisplayName("upload new version")
    class UploadNewVersion {

        @Test
        void shouldCreateNextVersionAndPublishSingleVersionEvent() {

            Document document = existingDocument(50L, VersionStatus.READY);
            DocumentVersion previous = DocumentVersion.builder().id(60L).versionNumber(1).build();

            when(documentRepository.findById(50L)).thenReturn(Optional.of(document));
            when(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(50L))
                    .thenReturn(Optional.of(previous));
            when(fileStorageService.store(any())).thenReturn(storedFile("v2.pdf"));

            UploadNewVersionRequest request = new UploadNewVersionRequest();
            request.setChangeNote("updated figures");
            request.setTitle("New title");

            UploadNewVersionResponse response = service.uploadNewVersion(50L, pdfFile("v2.pdf"), request);

            assertThat(response.getVersionNumber()).isEqualTo(2);
            assertThat(response.getChangeNote()).isEqualTo("updated figures");
            assertThat(response.getStatus()).isEqualTo(VersionStatus.PENDING.name());
            assertThat(document.getCurrentVersion().getVersionNumber()).isEqualTo(2);
            assertThat(document.getTitle()).isEqualTo("New title");

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(DocumentVersionCreatedEvent.class);
            assertThat(((DocumentVersionCreatedEvent) eventCaptor.getValue()).versionId())
                    .isEqualTo(response.getCurrentVersionId());
        }

        @Test
        void shouldKeepExistingMetadataWhenRequestFieldsAreNull() {

            Document document = existingDocument(51L, VersionStatus.READY);
            document.setTitle("Original");
            document.setDescription("Original description");
            document.setDocumentType(DocumentType.REPORT);

            when(documentRepository.findById(51L)).thenReturn(Optional.of(document));
            when(fileStorageService.store(any())).thenReturn(storedFile("v2.pdf"));

            service.uploadNewVersion(51L, pdfFile("v2.pdf"), new UploadNewVersionRequest());

            assertThat(document.getTitle()).isEqualTo("Original");
            assertThat(document.getDescription()).isEqualTo("Original description");
            assertThat(document.getDocumentType()).isEqualTo(DocumentType.REPORT);
        }

        @Test
        void shouldRejectMissingDocument() {

            when(documentRepository.findById(404L)).thenReturn(Optional.empty());

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.uploadNewVersion(
                            404L, pdfFile("a.pdf"), new UploadNewVersionRequest())),
                    ErrorCode.DOCUMENT_NOT_FOUND
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldRejectNullDocumentId() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.uploadNewVersion(
                            null, pdfFile("a.pdf"), new UploadNewVersionRequest())),
                    ErrorCode.DOCUMENT_ID_REQUIRED
            );
        }

        @Test
        void shouldRejectNewVersionForDeletedDocument() {

            Document deleted = existingDocument(52L, VersionStatus.READY);
            deleted.setStatus(DocumentStatus.DELETED);

            when(documentRepository.findById(52L)).thenReturn(Optional.of(deleted));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.uploadNewVersion(
                            52L, pdfFile("a.pdf"), new UploadNewVersionRequest())),
                    ErrorCode.DOCUMENT_DELETED
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldPropagateAuthorizationFailure() {

            Document document = existingDocument(53L, VersionStatus.READY);
            when(documentRepository.findById(53L)).thenReturn(Optional.of(document));
            doThrow(new AuthorizationException(ErrorCode.ACCESS_DENIED))
                    .when(documentAuthorizationService).requireUpdate(document);

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.uploadNewVersion(
                            53L, pdfFile("a.pdf"), new UploadNewVersionRequest())),
                    ErrorCode.ACCESS_DENIED
            );

            verifyNoInteractions(fileStorageService);
        }
    }

    // ===================== metadata / move =====================

    @Nested
    @DisplayName("metadata and move")
    class MetadataAndMove {

        @Test
        void shouldUpdateOnlyProvidedMetadataFields() {

            Document document = existingDocument(70L, VersionStatus.READY);
            document.setTitle("Old");
            document.setDescription("Old description");
            when(documentRepository.findById(70L)).thenReturn(Optional.of(document));

            DocumentUpdateMetadataRequest request = new DocumentUpdateMetadataRequest();
            request.setDescription("New description");

            DocumentUpdateMetadataResponse response = service.updateDocumentMetadata(70L, request);

            assertThat(response.getTitle()).isEqualTo("Old");
            assertThat(response.getDescription()).isEqualTo("New description");
            assertThat(document.getDocumentType()).isEqualTo(DocumentType.REPORT);
        }

        @Test
        void shouldRejectMetadataUpdateForDeletedDocument() {

            Document deleted = existingDocument(71L, VersionStatus.READY);
            deleted.setStatus(DocumentStatus.DELETED);
            when(documentRepository.findById(71L)).thenReturn(Optional.of(deleted));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.updateDocumentMetadata(
                            71L, new DocumentUpdateMetadataRequest())),
                    ErrorCode.DOCUMENT_DELETED
            );
        }

        @Test
        void shouldRejectNullMetadataRequest() {

            Document document = existingDocument(72L, VersionStatus.READY);
            when(documentRepository.findById(72L)).thenReturn(Optional.of(document));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.updateDocumentMetadata(72L, null)),
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        @Test
        void shouldMoveDocumentToResolvedFolder() {

            Document document = existingDocument(73L, VersionStatus.READY);
            when(documentRepository.findById(73L)).thenReturn(Optional.of(document));

            MoveDocumentRequest request = new MoveDocumentRequest();
            request.setFolderId(null);

            DocumentMoveResponse response = service.moveDocument(73L, request);

            assertThat(response.getDocumentId()).isEqualTo(73L);
            assertThat(response.getFolderId()).isEqualTo(rootFolder.getId());
            verify(folderService).resolveTargetFolder(null);
            assertThat(document.getFolder()).isSameAs(rootFolder);
        }

        @Test
        void shouldReportTitleExistence() {

            when(documentRepository.existsByTitle("taken")).thenReturn(true);
            when(documentRepository.existsByTitle("free")).thenReturn(false);

            assertThat(service.existsByTitle("taken")).isTrue();
            assertThat(service.existsByTitle("free")).isFalse();
        }
    }

    // ===================== delete / restore =====================

    @Nested
    @DisplayName("soft delete and restore")
    class DeleteAndRestore {

        @Test
        void shouldSoftDeleteDocumentAndKeepStoredFile() {

            Document document = existingDocument(80L, VersionStatus.READY);
            when(documentRepository.findById(80L)).thenReturn(Optional.of(document));

            service.deleteDocument(80L);

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.DELETED);
            assertThat(document.getDeletedAt()).isNotNull();
            assertThat(document.getDeletedBy()).isSameAs(owner);
            verify(documentRepository).save(document);
            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldRejectDeletingAnAlreadyDeletedDocument() {

            Document deleted = existingDocument(81L, VersionStatus.READY);
            deleted.setStatus(DocumentStatus.DELETED);
            when(documentRepository.findById(81L)).thenReturn(Optional.of(deleted));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.deleteDocument(81L)),
                    ErrorCode.DOCUMENT_DELETED
            );
        }

        @Test
        void shouldRejectDeletingMissingDocument() {

            when(documentRepository.findById(82L)).thenReturn(Optional.empty());

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.deleteDocument(82L)),
                    ErrorCode.DOCUMENT_NOT_FOUND
            );
        }

        @Test
        void shouldRestoreDeletedDocumentAndClearAuditColumns() {

            Document document = existingDocument(83L, VersionStatus.READY);
            document.setStatus(DocumentStatus.DELETED);
            document.setDeletedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
            document.setDeletedBy(owner);
            when(documentRepository.findById(83L)).thenReturn(Optional.of(document));

            DocumentRestoreResponse response = service.restoreDocument(83L);

            assertThat(response.getDocumentId()).isEqualTo(83L);
            assertThat(response.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
            assertThat(document.getDeletedAt()).isNull();
            assertThat(document.getDeletedBy()).isNull();
        }

        @Test
        void shouldRejectRestoringActiveDocument() {

            Document active = existingDocument(84L, VersionStatus.READY);
            when(documentRepository.findById(84L)).thenReturn(Optional.of(active));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.restoreDocument(84L)),
                    ErrorCode.DOCUMENT_NOT_DELETED
            );
        }
    }

    // ===================== download / processing resource =====================

    @Nested
    @DisplayName("download and processing resource")
    class Downloads {

        @Test
        void shouldReturnSelectedVersionResource() {

            Document document = existingDocument(90L, VersionStatus.READY);
            FileEntity file = FileEntity.builder()
                    .id(91L)
                    .bucketName("test-files")
                    .objectKey("key-1")
                    .originalFilename("v1.pdf")
                    .mimeType("application/pdf")
                    .fileSize(3L)
                    .build();
            DocumentVersion version = DocumentVersion.builder()
                    .id(92L)
                    .document(document)
                    .file(file)
                    .versionNumber(1)
                    .status(VersionStatus.READY)
                    .build();

            Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

            when(documentRepository.findById(90L)).thenReturn(Optional.of(document));
            when(versionRepository.findById(92L)).thenReturn(Optional.of(version));
            when(fileStorageService.loadAsResource("test-files", "key-1")).thenReturn(resource);

            DocumentDownloadResource downloaded = service.downloadSelectedVersion(90L, 92L);

            assertThat(downloaded.getResource()).isSameAs(resource);
            assertThat(downloaded.getOriginalFilename()).isEqualTo("v1.pdf");
            assertThat(downloaded.getFileSize()).isEqualTo(3L);
        }

        @Test
        void shouldRejectVersionThatBelongsToAnotherDocument() {

            Document document = existingDocument(93L, VersionStatus.READY);
            Document otherDocument = existingDocument(94L, VersionStatus.READY);

            DocumentVersion foreignVersion = DocumentVersion.builder()
                    .id(95L)
                    .document(otherDocument)
                    .file(FileEntity.builder().bucketName("b").objectKey("k").build())
                    .versionNumber(1)
                    .build();

            when(documentRepository.findById(93L)).thenReturn(Optional.of(document));
            when(versionRepository.findById(95L)).thenReturn(Optional.of(foreignVersion));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.downloadSelectedVersion(93L, 95L)),
                    ErrorCode.DOCUMENT_VERSION_NOT_FOUND
            );

            verifyNoInteractions(fileStorageService);
        }

        @Test
        void shouldRejectMissingVersion() {

            Document document = existingDocument(96L, VersionStatus.READY);
            when(documentRepository.findById(96L)).thenReturn(Optional.of(document));
            when(versionRepository.findById(97L)).thenReturn(Optional.empty());

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.downloadSelectedVersion(96L, 97L)),
                    ErrorCode.DOCUMENT_VERSION_NOT_FOUND
            );
        }

        @Test
        void shouldRejectDownloadWithoutStorageMetadata() {

            Document document = existingDocument(98L, VersionStatus.READY);
            DocumentVersion version = DocumentVersion.builder()
                    .id(99L)
                    .document(document)
                    .file(FileEntity.builder().originalFilename("f.pdf").build())
                    .versionNumber(1)
                    .build();

            when(documentRepository.findById(98L)).thenReturn(Optional.of(document));
            when(versionRepository.findById(99L)).thenReturn(Optional.of(version));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.downloadSelectedVersion(98L, 99L)),
                    ErrorCode.FILE_STORAGE_METADATA_INVALID
            );
        }

        @Test
        void shouldLoadProcessingResourceForVersion() {

            Document document = existingDocument(100L, VersionStatus.READY);
            FileEntity file = FileEntity.builder().bucketName("test-files").objectKey("key-2").build();
            DocumentVersion version = DocumentVersion.builder()
                    .id(101L)
                    .document(document)
                    .file(file)
                    .versionNumber(1)
                    .build();

            Resource resource = new ByteArrayResource(new byte[]{4});

            when(versionRepository.findById(101L)).thenReturn(Optional.of(version));
            when(fileStorageService.loadAsResource("test-files", "key-2")).thenReturn(resource);

            assertThat(service.loadProcessingResource(101L)).isSameAs(resource);
        }

        @Test
        void shouldRejectProcessingResourceWhenVersionHasNoFile() {

            DocumentVersion version = DocumentVersion.builder().id(102L).versionNumber(1).build();
            when(versionRepository.findById(102L)).thenReturn(Optional.of(version));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.loadProcessingResource(102L)),
                    ErrorCode.FILE_NOT_FOUND
            );
        }

        @Test
        void shouldRejectProcessingResourceForDeletedDocument() {

            Document deleted = existingDocument(103L, VersionStatus.READY);
            deleted.setStatus(DocumentStatus.DELETED);
            DocumentVersion version = DocumentVersion.builder()
                    .id(104L)
                    .document(deleted)
                    .file(FileEntity.builder().bucketName("b").objectKey("k").build())
                    .versionNumber(1)
                    .build();

            when(versionRepository.findById(104L)).thenReturn(Optional.of(version));

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.loadProcessingResource(104L)),
                    ErrorCode.DOCUMENT_DELETED
            );
        }
    }

    // ===================== listing / detail =====================

    @Nested
    @DisplayName("listing and detail")
    class Listing {

        @Test
        void shouldDelegateListingWithCurrentAccessScope() {

            DocumentAccessScope scope = new DocumentAccessScope(false, 10L, 2L, List.of(5L));
            when(documentAuthorizationService.currentAccessScope()).thenReturn(scope);

            Pageable pageable = PageRequest.of(0, 10);
            Page<DocumentListResponse> page = new PageImpl<>(List.of());

            when(documentRepository.filterDocuments(any(), eq(scope), eq(pageable))).thenReturn(page);

            assertThat(service.getDocuments(new DocumentFilterRequest(), pageable)).isSameAs(page);
        }

        @Test
        void shouldRejectInvalidSortBeforeQuerying() {

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.getDocuments(invalidSortFilter(), PageRequest.of(0, 10))),
                    ErrorCode.INVALID_SORT_OPTION
            );

            verify(documentRepository, never()).filterDocuments(any(), any(), any());
        }

        @Test
        void shouldUseUnrestrictedScopeForAdminListing() {

            DocumentAccessScope scope = new DocumentAccessScope(true, 1L, null, List.of());
            when(documentAuthorizationService.currentAccessScope()).thenReturn(scope);
            when(documentRepository.filterDocuments(any(), eq(scope), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(service.getDocumentsForAdmin(new DocumentFilterRequest(), PageRequest.of(0, 20)))
                    .isNotNull();

            verify(documentRepository).filterDocuments(any(), eq(scope), any());
        }

        @Test
        void shouldMapDocumentDetailIncludingVersionHistory() {

            Document document = existingDocument(110L, VersionStatus.READY);

            DocumentVersion current = document.getCurrentVersion();
            current.setStatus(VersionStatus.READY);

            DocumentVersion older = DocumentVersion.builder()
                    .id(111L)
                    .document(document)
                    .file(FileEntity.builder().originalFilename("v0.pdf").build())
                    .versionNumber(0)
                    .status(VersionStatus.FAILED)
                    .build();

            document.setVersions(List.of(older, current));

            when(documentRepository.findById(110L)).thenReturn(Optional.of(document));

            DocumentDetailResponse detail = service.getDocumentDetail(110L);

            assertThat(detail.documentInfo().title()).isEqualTo("document-110");
            assertThat(detail.currentVersion().versionId()).isEqualTo(current.getId());
            assertThat(detail.versionHistory())
                    .extracting(DocumentDetailResponse.VersionHistoryItem::versionNumber)
                    .containsExactly(1, 0);
            assertThat(detail.advancedInfo().bucketName()).isEqualTo("test-files");
        }

        @Test
        void shouldRejectMissingDocumentDetail() {

            when(documentRepository.findById(404L)).thenReturn(Optional.empty());

            ErrorCodeAssertions.assertBusinessError(
                    catchThrowable(() -> service.getDocumentDetail(404L)),
                    ErrorCode.DOCUMENT_NOT_FOUND
            );
        }
    }

    // ===================== helpers =====================

    private static DocumentFilterRequest invalidSortFilter() {
        DocumentFilterRequest filter = new DocumentFilterRequest();
        filter.setSort("unsupported");
        return filter;
    }

    private Document existingDocument(Long id, VersionStatus versionStatus) {

        FileEntity file = FileEntity.builder()
                .id(sequence.incrementAndGet())
                .bucketName("test-files")
                .objectKey("object-" + id)
                .originalFilename("document-" + id + ".pdf")
                .extension("pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .build();

        Document document = Document.builder()
                .id(id)
                .title("document-" + id)
                .description("description")
                .documentType(DocumentType.REPORT)
                .owner(owner)
                .department(itDepartment)
                .folder(rootFolder)
                .status(DocumentStatus.ACTIVE)
                .build();

        DocumentVersion version = DocumentVersion.builder()
                .id(sequence.incrementAndGet())
                .document(document)
                .file(file)
                .versionNumber(1)
                .status(versionStatus)
                .build();

        document.setCurrentVersion(version);

        return document;
    }

    private static MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile("files", name, "application/pdf", new byte[]{1, 2, 3});
    }

    private static StoredFileDto storedFile(String name) {

        return StoredFileDto.builder()
                .originalName(name)
                .storedName("uuid-" + name)
                .objectKey("uuid-" + name)
                .bucket("test-files")
                .contentType("application/pdf")
                .size(3L)
                .build();
    }

    private static DocumentUploadRequest uploadRequest(String title, DocumentType type) {

        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle(title);
        request.setDescription("description");
        request.setDocumentType(type);

        return request;
    }

    private static DocumentBatchUploadRequest batchRequest(DocumentUploadRequest... documents) {

        DocumentBatchUploadRequest request = new DocumentBatchUploadRequest();

        ReflectionTestUtils.setField(request, "documents", List.of(documents));

        return request;
    }

    private <T> T assignId(T entity) {

        if (entity instanceof Document document && document.getId() == null) {
            document.setId(sequence.incrementAndGet());
        }

        if (entity instanceof DocumentVersion version && version.getId() == null) {
            version.setId(sequence.incrementAndGet());
        }

        if (entity instanceof FileEntity file && file.getId() == null) {
            file.setId(sequence.incrementAndGet());
        }

        return entity;
    }
}
