package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.dto.request.DocumentUploadRequest;
import com.enterprise.aiassistant.backend.document.dto.response.DocumentUploadResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

        Document document = null;
        DocumentVersion version = null;


        // 3. Nếu có documentId => tạo version mới
        if (request.getDocumentId() != null) {

            document = documentRepository
                    .findById(request.getDocumentId())
                    .orElseThrow(() -> new DocumentException(ErrorCode.DOCUMENT_NOT_FOUND));

            // Update metadata
            document.setTitle(request.getTitle());
            document.setDescription(request.getDescription());
            document.setDocumentType(request.getDocumentType());

            version = createNewVersion(document, newFile, request.getChangeNote());
        }
        // 4. Nếu chưa có documentId => tạo document mới
        else {

            document = documentMapper.toDocument(request);

            documentRepository.save(document);

            version = documentMapper.toDocumentVersion(
                    document,
                    newFile,
                    1,
                    request.getChangeNote()
            );
        }


        // 5. Save version
        versionRepository.save(version);


        // 6. Update current version
        document.setCurrentVersion(version);
        documentRepository.save(document);

        int totalVersions = (int) versionRepository.countByDocumentId(document.getId());
        return documentMapper.toUploadResponse(
                document,
                newFile,
                totalVersions
        );
    }

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
