package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentDownloadResource;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUpdateMetadataResponse;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
import com.enterprise.aiassistant.backend.document.dto.response.UploadNewVersionResponse;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.helper.DocumentHelper;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.processing.worker.DocumentProcessingWorker;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.mapper.FileMapper;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.DOCUMENT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService{

    private final FileStorageService fileStorageService;

    private final FileRepository fileRepository;

    private final DocumentRepository documentRepository;

    private final DocumentVersionRepository versionRepository;

    private final DocumentMapper documentMapper;

    private final FileMapper fileMapper;

    private final DocumentHelper documentHelper;

    private final DocumentProcessingWorker documentProcessingWorker;


    @Override
    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            DocumentUploadRequest request
    ) {

        documentHelper.validateFile(file);
        documentHelper.validateRequest(request);

        // 1. Upload file lên MinIO
        StoredFileDto storedFile = fileStorageService.store(file);


        // 2. Tạo FileEntity
        FileEntity newFile = fileMapper.toFileEntity(storedFile);
        fileRepository.save(newFile);


        // 3. Tạo Document
        Document document = documentMapper.toDocument(request);
        documentRepository.save(document);


        // 4. Tạo DocumentVersion
        DocumentVersion version = documentMapper.toDocumentVersion(
                document,
                newFile,
                1,
                ""
        );
        versionRepository.save(version);


        // 5. Update current version
        document.setCurrentVersion(version);
        documentRepository.save(document);


        // Gọi Document Processing Worker
        documentProcessingWorker.submit(version.getId());

        return documentMapper.toUploadResponse(
                document,
                newFile
        );
    }

    @Override
    @Transactional
    public UploadNewVersionResponse uploadNewVersion(
            Long documentId,
            MultipartFile file,
            UploadNewVersionRequest request
    ){

        documentHelper.validateDocumentId(documentId);
        documentHelper.validateFile(file);
        documentHelper.validateVersionRequest(request);


        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        // Upload file mới vào storage
        StoredFileDto storedFile = fileStorageService.store(file);


        // Lưu metadata file
        FileEntity fileEntity = fileMapper.toFileEntity(storedFile);
        fileRepository.save(fileEntity);


        // Tạo version mới
        DocumentVersion newVersion = documentHelper.createNewVersion(
                document,
                fileEntity,
                request.getChangeNote()
        );

        versionRepository.save(newVersion);


        // Update current version
        document.setCurrentVersion(newVersion);
        documentRepository.save(document);


        return documentMapper.toUploadNewVersionResponse(
                document,
                newVersion,
                fileEntity
        );
    }

    @Override
    @Transactional
    public DocumentUpdateMetadataResponse updateDocumentMetadata(
            Long documentId,
            DocumentUpdateMetadataRequest request
    ){

        documentHelper.validateDocumentId(documentId);
        documentHelper.validateUpdateMetadataRequest(request);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));

        // Nếu document đã bị delete thì không cho cập nhật
        documentHelper.validateDocumentStatus(document);

        documentHelper.validateMetadata(request);


        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }

        if (request.getDocumentType() != null) {
            document.setDocumentType(request.getDocumentType());
        }

        documentRepository.save(document);

        return documentMapper.toUpdateMetadataReponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadResource downloadSelectedVersion(Long documentId, Long  versionId) {

        documentHelper.validateDocumentId(documentId);

        documentHelper.validateDocumentVersion(versionId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        DocumentVersion selectedVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));


        FileEntity file = selectedVersion.getFile();

        documentHelper.validateFileStorageMetadata(file);


        Resource resource = fileStorageService.loadAsResource(
                file.getBucketName(),
                file.getObjectKey()
        );

        return documentMapper.toDocumentDownloadResource(resource, file);

    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadProcessingResource(Long versionId) {

        documentHelper.validateDocumentVersion(versionId);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));

        Document document = version.getDocument();

        if (document != null) {
            documentHelper.validateDocumentStatus(document);
        }

        FileEntity fileEntity = version.getFile();

        if (fileEntity == null) {
            throw new DocumentException(ErrorCode.FILE_NOT_FOUND);
        }

        return fileStorageService.loadAsResource(
                fileEntity.getBucketName(),
                fileEntity.getObjectKey()
        );
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {

        documentHelper.validateDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND));

        documentHelper.validateDocumentStatus(document);

        document.setStatus(DocumentStatus.DELETED);
        document.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(document);
    }

    @Override
    public boolean existsByTitle(String title) {
        return documentRepository.existsByTitle(title);
    }
}


