package com.enterprise.aiassistant.backend.document.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DocumentHelper {

    private final FileUploadProperties fileUploadProperties;

    private final DocumentVersionRepository versionRepository;

    private final DocumentMapper documentMapper;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "application/pdf",
                    "application/msword", // .doc
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
            );

    public void validateFile(MultipartFile file) {
        if (file == null) {
            throw new BusinessException(
                    ErrorCode.FILE_REQUIRED
            );
        }

        if (file.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.EMPTY_FILE
            );
        }

        if (file.getSize() > fileUploadProperties.getMaxSize().toBytes()) {
            throw new BusinessException(
                    ErrorCode.FILE_TOO_LARGE
            );
        }

        validateContentType(file);
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE
            );
        }
    }

    public void validateRequest(DocumentUploadRequest request) {
        if (request == null) {
            throw new DocumentException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new DocumentException(
                    ErrorCode.DOCUMENT_TITLE_REQUIRED
            );
        }
    }

    private int getNextVersionNumber(Document document) {
        return versionRepository
                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    public DocumentVersion createNewVersion(Document document, FileEntity newFile, String changeNote) {
        int nextVersion = getNextVersionNumber(document);

        return documentMapper.toDocumentVersion(
                document,
                newFile,
                nextVersion,
                changeNote
        );
    }

    public void validateDocumentId(Long documentId){

        if(documentId == null){

            throw new DocumentException(
                    ErrorCode.DOCUMENT_ID_REQUIRED
            );
        }
    }

    public void validateVersionRequest(
            UploadNewVersionRequest request
    ){

        if(request == null){

            throw new BusinessException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }

        if(request.getChangeNote() != null &&
                request.getChangeNote().length() > 255){

            throw new BusinessException(
                    ErrorCode.CHANGE_NOTE_TOO_LONG
            );
        }
    }

    public void validateDocumentStatus(Document document){

        if(document.getStatus() == DocumentStatus.DELETED){

            throw new DocumentException(
                    ErrorCode.DOCUMENT_DELETED
            );
        }
    }

    public void validateUpdateMetadataRequest(
            DocumentUpdateMetadataRequest request
    ){

        if(request == null){

            throw new BusinessException(
                    ErrorCode.REQUEST_REQUIRED
            );
        }
    }

    public void validateMetadata(
            DocumentUpdateMetadataRequest request
    ){

        // Validate title
        if(request.getTitle() != null){

            if(request.getTitle().isBlank()){

                throw new BusinessException(
                        ErrorCode.DOCUMENT_TITLE_REQUIRED
                );
            }


            if(request.getTitle().length() > 255){

                throw new BusinessException(
                        ErrorCode.TITLE_TOO_LONG
                );
            }
        }


        // Validate description
        if(request.getDescription() != null){

            if(request.getDescription().length() > 1000){

                throw new BusinessException(
                        ErrorCode.DESCRIPTION_TOO_LONG
                );
            }
        }
    }

    public static MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public void validateDocumentVersion(Long versionId){
        if(versionId == null){
            throw new DocumentException(
                    ErrorCode.DOCUMENT_VERSION_NOT_FOUND
            );
        }
    }
    public void validateFileStorageMetadata(FileEntity file){

        if (file == null) {
        throw new FileStorageException(ErrorCode.FILE_NOT_FOUND);
    }

        if (file.getBucketName() == null
                || file.getBucketName().isBlank()
                || file.getObjectKey() == null
                || file.getObjectKey().isBlank()) {
            throw new DocumentException(
                    ErrorCode.FILE_STORAGE_METADATA_INVALID
            );
        }
    }
}





