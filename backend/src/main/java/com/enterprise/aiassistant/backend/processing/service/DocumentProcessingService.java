package com.enterprise.aiassistant.backend.processing.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.ProcessingStep;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentService;
import com.enterprise.aiassistant.backend.processing.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.dto.TextChunk;
import com.enterprise.aiassistant.backend.processing.helper.ProcessingHelper;
import com.enterprise.aiassistant.backend.processing.mapper.ProcessingMapper;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {


    private final DocumentVersionRepository documentVersionRepository;

    private final FileStorageService fileStorageService;

    private final DocumentTextRepository documentTextRepository;

    private final TextExtractionService textExtractionService;

    private final ChunkingService chunkingService;

    private final DocumentChunkRepository documentChunkRepository;

    private final ProcessingMapper processingMapper;

    private final ProcessingHelper processingHelper;


    @Transactional
    public void process(Long versionId) {


        DocumentVersion version = documentVersionRepository.findById(versionId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.DOCUMENT_VERSION_NOT_FOUND));

        processingHelper.validateStatus(version);

        try {

            // update PROCESSING
            version.setStatus(VersionStatus.PROCESSING);

            // Set trạng thái chưa bị fail
            version.setProcessingStep(null);


            /*
             * STEP 1:
             * TEXT EXTRACTION
             */

            version.setProcessingStep(ProcessingStep.TEXT_EXTRACTING);

            Resource resource = fileStorageService.loadAsResource(
                    version.getFile().getBucketName(),
                    version.getFile().getObjectKey());


            ExtractedText extractedText = textExtractionService.extract(
                            resource,
                            version.getFile().getMimeType()
                    );



            /*
             * SAVE DOCUMENT TEXT
             */

            DocumentText documentText = processingMapper.toDocumentText(version, extractedText);

            documentTextRepository.save(documentText);


            /*
             * STEP 2:
             * CHUNKING
             */

            version.setProcessingStep(ProcessingStep.CHUNKING);

            List<TextChunk> listTextChunk = chunkingService.chunk(extractedText.getContent());


            /*
             * Save document_chunks
             */

            for (TextChunk textChunk: listTextChunk){
                DocumentChunk newDocumentChunk = processingMapper.toDocumentChunk(version, textChunk);
                documentChunkRepository.save(newDocumentChunk);
            }

            version.setStatus(VersionStatus.READY);

            version.setProcessingStep(null);

        } catch (BusinessException e) {

            processingHelper.handleFailed(version.getId(), e, version.getProcessingStep());
            throw e;


        } catch (Exception e) {

            processingHelper.handleFailed(version.getId(), e, version.getProcessingStep());

            throw new BusinessException(
                    ErrorCode.DOCUMENT_PROCESSING_FAILED,
                    ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                    e
            );
        }
    }

}
