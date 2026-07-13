package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
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
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.mapper.FileMapper;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository versionRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private DocumentHelper documentHelper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    // ========================================================================
    // Helper methods
    // ========================================================================

    private MultipartFile createMockFile() {
        return mock(MultipartFile.class);
    }

    private DocumentUploadRequest createUploadRequest() {
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle("Test Document");
        request.setDescription("Test Description");
        request.setDocumentType(DocumentType.REPORT);
        return request;
    }

    private Document createDocument(Long id, DocumentStatus status) {
        return Document.builder()
                .id(id)
                .title("Test Document")
                .description("Test Description")
                .documentType(DocumentType.REPORT)
                .status(status)
                .build();
    }

    private DocumentVersion createDocumentVersion(Long id, int versionNumber) {
        return DocumentVersion.builder()
                .id(id)
                .versionNumber(versionNumber)
                .status(VersionStatus.PENDING)
                .changeNote("Initial upload")
                .build();
    }

    private StoredFileDto createStoredFileDto() {
        return StoredFileDto.builder()
                .originalName("test.pdf")
                .storedName("uuid-test.pdf")
                .bucket("documents")
                .objectKey("documents/uuid-test.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .build();
    }

    private FileEntity createFileEntity(Long id) {
        return FileEntity.builder()
                .id(id)
                .originalFilename("test.pdf")
                .storedFilename("uuid-test.pdf")
                .bucketName("documents")
                .objectKey("documents/uuid-test.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .storageProvider("minio")
                .build();
    }

    private UploadNewVersionRequest createVersionRequest() {
        UploadNewVersionRequest request = new UploadNewVersionRequest();
        request.setChangeNote("Updated version");
        return request;
    }

    private DocumentUpdateMetadataRequest createUpdateMetadataRequest() {
        DocumentUpdateMetadataRequest request = new DocumentUpdateMetadataRequest();
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setDocumentType(DocumentType.CONTRACT);
        return request;
    }

    // ========================================================================
    // Tests for upload()
    // ========================================================================

    @Test
    void upload_validRequest_shouldReturnResponse() {
        // Given
        MultipartFile file = createMockFile();
        DocumentUploadRequest request = createUploadRequest();
        StoredFileDto storedFile = createStoredFileDto();
        FileEntity fileEntity = createFileEntity(1L);
        Document document = createDocument(1L, DocumentStatus.ACTIVE);
        DocumentVersion version = createDocumentVersion(1L, 1);
        DocumentUploadResponse expectedResponse = DocumentUploadResponse.builder()
                .documentId(1L)
                .currentVersionId(1L)
                .title("Test Document")
                .filename("test.pdf")
                .versionNumber(1)
                .status(DocumentStatus.ACTIVE)
                .build();

        when(fileStorageService.store(file)).thenReturn(storedFile);
        when(fileMapper.toFileEntity(storedFile)).thenReturn(fileEntity);
        when(documentMapper.toDocument(request)).thenReturn(document);
        when(documentMapper.toDocumentVersion(document, fileEntity, 1, "")).thenReturn(version);
        when(documentMapper.toUploadResponse(document, fileEntity)).thenReturn(expectedResponse);

        // When
        DocumentUploadResponse response = documentService.upload(file, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Document");
        assertThat(response.getFilename()).isEqualTo("test.pdf");
        assertThat(response.getVersionNumber()).isEqualTo(1);

        // Business behavior: validation được gọi
        verify(documentHelper).validateFile(file);
        verify(documentHelper).validateRequest(request);

        // Business behavior: file được upload lên storage
        verify(fileStorageService).store(file);

        // Business behavior: các aggregate được persist
        verify(fileRepository).save(any(FileEntity.class));
        verify(versionRepository).save(any(DocumentVersion.class));
        verify(documentRepository, times(2)).save(document);

        // Business behavior: response được mapping
        verify(documentMapper).toUploadResponse(document, fileEntity);
    }

    @Test
    void upload_validateFileThrowsException_shouldNotProceed() {
        // Given
        MultipartFile file = createMockFile();
        DocumentUploadRequest request = createUploadRequest();
        doThrow(new BusinessException(ErrorCode.FILE_REQUIRED))
                .when(documentHelper).validateFile(file);

        // When & Then
        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_REQUIRED);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void upload_validateRequestThrowsException_shouldNotProceed() {
        // Given
        MultipartFile file = createMockFile();
        DocumentUploadRequest request = createUploadRequest();
        doThrow(new DocumentException(ErrorCode.DOCUMENT_TITLE_REQUIRED))
                .when(documentHelper).validateRequest(request);

        // When & Then
        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_TITLE_REQUIRED);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void upload_storageServiceThrowsException_shouldNotSaveToDatabase() {
        // Given
        MultipartFile file = createMockFile();
        DocumentUploadRequest request = createUploadRequest();
        when(fileStorageService.store(file))
                .thenThrow(new FileStorageException("MinIO connection failed", new RuntimeException()));

        // When & Then
        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(FileStorageException.class);

        verifyNoInteractions(fileRepository, documentRepository, versionRepository);
    }

    @Test
    void upload_fileRepositorySaveThrowsException_shouldPropagate() {
        // Given
        MultipartFile file = createMockFile();
        DocumentUploadRequest request = createUploadRequest();
        StoredFileDto storedFile = createStoredFileDto();
        FileEntity fileEntity = createFileEntity(1L);

        when(fileStorageService.store(file)).thenReturn(storedFile);
        when(fileMapper.toFileEntity(storedFile)).thenReturn(fileEntity);
        when(fileRepository.save(fileEntity)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> documentService.upload(file, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verifyNoInteractions(documentRepository, versionRepository);
    }

    // ========================================================================
    // Tests for uploadNewVersion()
    // ========================================================================

    @Test
    void uploadNewVersion_validRequest_shouldReturnResponse() {
        // Given
        Long documentId = 1L;
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();
        Document document = createDocument(1L, DocumentStatus.ACTIVE);
        StoredFileDto storedFile = createStoredFileDto();
        FileEntity fileEntity = createFileEntity(2L);
        DocumentVersion newVersion = createDocumentVersion(2L, 2);
        UploadNewVersionResponse expectedResponse = UploadNewVersionResponse.builder()
                .documentId(1L)
                .currentVersionId(2L)
                .filename("test.pdf")
                .versionNumber(2)
                .changeNote("Updated version")
                .status("PENDING")
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(fileStorageService.store(file)).thenReturn(storedFile);
        when(fileMapper.toFileEntity(storedFile)).thenReturn(fileEntity);
        when(documentHelper.createNewVersion(document, fileEntity, "Updated version")).thenReturn(newVersion);
        when(documentMapper.toUploadNewVersionResponse(document, newVersion, fileEntity)).thenReturn(expectedResponse);

        // When
        UploadNewVersionResponse response = documentService.uploadNewVersion(documentId, file, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isEqualTo(1L);
        assertThat(response.getVersionNumber()).isEqualTo(2);
        assertThat(response.getChangeNote()).isEqualTo("Updated version");

        // Business behavior: file mới được upload
        verify(fileStorageService).store(file);

        // Business behavior: version mới được lưu
        verify(versionRepository).save(any(DocumentVersion.class));

        // Business behavior: current version được update
        assertThat(document.getCurrentVersion()).isEqualTo(newVersion);

        // Business behavior: response được trả về
        verify(documentMapper).toUploadNewVersionResponse(document, newVersion, fileEntity);
    }

    @Test
    void uploadNewVersion_documentIdNull_shouldThrowException() {
        // Given
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();
        doThrow(new DocumentException(ErrorCode.DOCUMENT_ID_REQUIRED))
                .when(documentHelper).validateDocumentId(null);

        // When & Then
        assertThatThrownBy(() -> documentService.uploadNewVersion(null, file, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_ID_REQUIRED);

        verifyNoInteractions(documentRepository, fileStorageService);
    }

    @Test
    void uploadNewVersion_documentNotFound_shouldThrowException() {
        // Given
        Long documentId = 99L;
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();

        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentService.uploadNewVersion(documentId, file, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadNewVersion_deletedDocument_shouldThrowException() {
        // Given
        Long documentId = 1L;
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();
        Document document = createDocument(1L, DocumentStatus.DELETED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doThrow(new DocumentException(ErrorCode.DOCUMENT_DELETED))
                .when(documentHelper).validateDocumentStatus(document);

        // When & Then
        assertThatThrownBy(() -> documentService.uploadNewVersion(documentId, file, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_DELETED);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadNewVersion_storageServiceThrowsException_shouldNotSaveVersion() {
        // Given
        Long documentId = 1L;
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();
        Document document = createDocument(1L, DocumentStatus.ACTIVE);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(fileStorageService.store(file))
                .thenThrow(new FileStorageException("Storage unavailable", new RuntimeException()));

        // When & Then
        assertThatThrownBy(() -> documentService.uploadNewVersion(documentId, file, request))
                .isInstanceOf(FileStorageException.class);

        verifyNoInteractions(fileRepository, versionRepository);
    }

    @Test
    void uploadNewVersion_createNewVersionThrowsException_shouldNotUpdateDocument() {
        // Given
        Long documentId = 1L;
        MultipartFile file = createMockFile();
        UploadNewVersionRequest request = createVersionRequest();
        Document document = createDocument(1L, DocumentStatus.ACTIVE);
        StoredFileDto storedFile = createStoredFileDto();
        FileEntity fileEntity = createFileEntity(2L);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(fileStorageService.store(file)).thenReturn(storedFile);
        when(fileMapper.toFileEntity(storedFile)).thenReturn(fileEntity);
        when(documentHelper.createNewVersion(document, fileEntity, "Updated version"))
                .thenThrow(new BusinessException(ErrorCode.DOCUMENT_VERSION_CREATION_FAILED));

        // When & Then
        assertThatThrownBy(() -> documentService.uploadNewVersion(documentId, file, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_VERSION_CREATION_FAILED);

        verifyNoInteractions(versionRepository);
        verify(documentRepository, never()).save(any());
    }

    // ========================================================================
    // Tests for updateDocumentMetadata()
    // ========================================================================

    @Test
    void updateDocumentMetadata_validRequest_shouldUpdateAllFields() {
        // Given
        Long documentId = 1L;
        DocumentUpdateMetadataRequest request = createUpdateMetadataRequest();
        Document document = createDocument(1L, DocumentStatus.ACTIVE);
        DocumentUpdateMetadataResponse expectedResponse = DocumentUpdateMetadataResponse.builder()
                .documentId(1L)
                .title("New Title")
                .description("New Description")
                .documentType(DocumentType.CONTRACT)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentMapper.toUpdateMetadataReponse(document)).thenReturn(expectedResponse);

        // When
        DocumentUpdateMetadataResponse response = documentService.updateDocumentMetadata(documentId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getDescription()).isEqualTo("New Description");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.CONTRACT);

        // Business behavior: document entity được update đúng
        assertThat(document.getTitle()).isEqualTo("New Title");
        assertThat(document.getDescription()).isEqualTo("New Description");
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.CONTRACT);

        // Business behavior: document được save
        verify(documentRepository).save(document);

        // Business behavior: response mapper được gọi
        verify(documentMapper).toUpdateMetadataReponse(document);
    }

    @Test
    void updateDocumentMetadata_partialUpdateOnlyTitle_shouldUpdateOnlyTitle() {
        // Given
        Long documentId = 1L;
        DocumentUpdateMetadataRequest request = new DocumentUpdateMetadataRequest();
        request.setTitle("Only New Title");
        // description and documentType are left null
        Document document = createDocument(1L, DocumentStatus.ACTIVE);
        document.setDescription("Original Description");
        document.setDocumentType(DocumentType.REPORT);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentMapper.toUpdateMetadataReponse(document)).thenReturn(
                DocumentUpdateMetadataResponse.builder()
                        .documentId(1L)
                        .title("Only New Title")
                        .description("Original Description")
                        .documentType(DocumentType.REPORT)
                        .build()
        );

        // When
        DocumentUpdateMetadataResponse response = documentService.updateDocumentMetadata(documentId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Only New Title");
        assertThat(response.getDescription()).isEqualTo("Original Description");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.REPORT);

        // Business behavior: chỉ title được update, các field khác giữ nguyên
        assertThat(document.getTitle()).isEqualTo("Only New Title");
        assertThat(document.getDescription()).isEqualTo("Original Description");
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.REPORT);

        verify(documentRepository).save(document);
        verify(documentMapper).toUpdateMetadataReponse(document);
    }

    @Test
    void updateDocumentMetadata_documentIdNull_shouldThrowException() {
        // Given
        DocumentUpdateMetadataRequest request = createUpdateMetadataRequest();
        doThrow(new DocumentException(ErrorCode.DOCUMENT_ID_REQUIRED))
                .when(documentHelper).validateDocumentId(null);

        // When & Then
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(null, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_ID_REQUIRED);

        verifyNoInteractions(documentRepository);
    }

    @Test
    void updateDocumentMetadata_requestNull_shouldThrowException() {
        // Given
        Long documentId = 1L;
        doThrow(new BusinessException(ErrorCode.REQUEST_REQUIRED))
                .when(documentHelper).validateUpdateMetadataRequest(null);

        // When & Then
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(documentId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);

        verifyNoInteractions(documentRepository);
    }

    @Test
    void updateDocumentMetadata_documentNotFound_shouldThrowException() {
        // Given
        Long documentId = 99L;
        DocumentUpdateMetadataRequest request = createUpdateMetadataRequest();

        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(documentId, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateDocumentMetadata_deletedDocument_shouldThrowException() {
        // Given
        Long documentId = 1L;
        DocumentUpdateMetadataRequest request = createUpdateMetadataRequest();
        Document document = createDocument(1L, DocumentStatus.DELETED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doThrow(new DocumentException(ErrorCode.DOCUMENT_DELETED))
                .when(documentHelper).validateDocumentStatus(document);

        // When & Then
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(documentId, request))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_DELETED);

        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void updateDocumentMetadata_validateMetadataThrowsException_shouldNotSave() {
        // Given
        Long documentId = 1L;
        DocumentUpdateMetadataRequest request = createUpdateMetadataRequest();
        Document document = createDocument(1L, DocumentStatus.ACTIVE);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doThrow(new BusinessException(ErrorCode.DOCUMENT_TITLE_REQUIRED))
                .when(documentHelper).validateMetadata(request);

        // When & Then
        assertThatThrownBy(() -> documentService.updateDocumentMetadata(documentId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_TITLE_REQUIRED);

        verify(documentRepository, never()).save(any());
        verifyNoInteractions(documentMapper);
    }
}
