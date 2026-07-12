package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUpdateMetadataRequest;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.request.UploadNewVersionRequest;
import com.enterprise.aiassistant.backend.document.dto.response.*;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.mapper.DocumentMapper;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.storage.mapper.FileMapper;
import com.enterprise.aiassistant.backend.storage.repository.FileRepository;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @Override
    @Transactional
    public DocumentUploadResponse upload(
            MultipartFile file,
            DocumentUploadRequest request
    ) {

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

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));


        // Upload file mới vào storage
        StoredFileDto storedFile = fileStorageService.store(file);


        // Lưu metadata file
        FileEntity fileEntity = fileMapper.toFileEntity(storedFile);
        fileRepository.save(fileEntity);


        // Tạo version mới
        DocumentVersion newVersion = createNewVersion(
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

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentException(DOCUMENT_NOT_FOUND));


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
    @Transactional
    public Page<DocumentListResponse> getDocuments(DocumentFilterRequest filter, Pageable pageable){

        return documentRepository
                .filterDocuments(filter, pageable);
    }


    // Helper
    private int getNextVersionNumber(Document document) {

        return versionRepository
                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    private DocumentVersion createNewVersion(Document document, FileEntity newFile, String changeNote){
        int nextVersion = getNextVersionNumber(document);

        return documentMapper.toDocumentVersion(
                document,
                newFile,
                nextVersion,
                changeNote
        );
    }

}
