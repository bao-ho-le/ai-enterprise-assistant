package com.enterprise.aiassistant.backend.processing.service;

import com.enterprise.aiassistant.backend.ai.embedding.service.EmbeddingService;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.vectorstore.service.VectorStoreService;
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

import java.util.ArrayList;
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

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStoreService;

    private final ProcessingMapper processingMapper;

    private final ProcessingHelper processingHelper;


    @Transactional
    public void process(Long versionId) {

        // Declared outside the try so the catch block can always reach it (and the
        // in-flight processingStep) — previously findById()/validateStatus() ran
        // BEFORE the try/catch, so a NOT_FOUND or invalid-status exception at that
        // point bypassed handleFailed() entirely, leaving the version stuck at
        // whatever status it already had (typically PENDING).
        DocumentVersion version = null;

        try {

            version = documentVersionRepository.findById(versionId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.DOCUMENT_VERSION_NOT_FOUND));

            processingHelper.validateStatus(version);

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

            List<DocumentChunk> savedChunks = new ArrayList<>();

            for (TextChunk textChunk: listTextChunk){
                DocumentChunk newDocumentChunk = processingMapper.toDocumentChunk(version, textChunk);
                savedChunks.add(documentChunkRepository.save(newDocumentChunk));
            }


            /*
             * STEP 3:
             * EMBEDDING
             */

            version.setProcessingStep(ProcessingStep.EMBEDDING);

            // ponytail: sequential embedding calls (one per chunk); parallelize with a
            // bounded executor if large documents make this a throughput bottleneck.
            List<VectorPoint> vectorPoints = savedChunks.stream()
                    .map(chunk -> processingMapper.toVectorPoint(
                            chunk,
                            embeddingService.embed(chunk.getContent())
                    ))
                    .toList();

            vectorStoreService.upsert(vectorPoints);

            version.setStatus(VersionStatus.READY);

            version.setProcessingStep(null);

        } catch (BusinessException e) {

            handleFailureUnlessAlreadySucceeded(versionId, version, e);
            throw e;


        } catch (Exception e) {

            handleFailureUnlessAlreadySucceeded(versionId, version, e);

            throw new BusinessException(
                    ErrorCode.DOCUMENT_PROCESSING_FAILED,
                    ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                    e
            );
        }
    }

    /**
     * validateStatus() throws DOCUMENT_VERSION_INVALID_STATUS when the version is
     * already READY — that guard exists to stop a stray duplicate trigger from
     * reprocessing a finished version, and must never itself flip that same version
     * to FAILED. Every other failure path (not found, transient I/O, extraction,
     * chunking, anything unexpected) always marks FAILED, using the versionId
     * parameter rather than version.getId() so this can't NullPointerException when
     * `version` is still null (i.e. the version wasn't even found).
     */
    private void handleFailureUnlessAlreadySucceeded(
            Long versionId,
            DocumentVersion version,
            Exception exception
    ) {
        boolean alreadySucceeded = version != null && version.getStatus() == VersionStatus.READY;

        if (!alreadySucceeded) {
            ProcessingStep failedStep = version != null ? version.getProcessingStep() : null;
            processingHelper.handleFailed(versionId, exception, failedStep);
        }
    }

}
