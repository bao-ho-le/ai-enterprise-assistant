package com.enterprise.aiassistant.backend.processing.orchestration.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.helper.EmbeddingHelper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageContext;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
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
import com.enterprise.aiassistant.backend.processing.chunking.service.ChunkingService;
import com.enterprise.aiassistant.backend.processing.extraction.dto.ExtractedText;
import com.enterprise.aiassistant.backend.processing.extraction.helper.ExtractionHelper;
import com.enterprise.aiassistant.backend.processing.extraction.service.TextExtractionService;
import com.enterprise.aiassistant.backend.processing.orchestration.helper.ProcessingHelper;
import com.enterprise.aiassistant.backend.processing.orchestration.mapper.ProcessingMapper;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    // Giới hạn số đoạn của 1 request batchEmbedContents của Gemini: mỗi lô là đúng 1 request và 1 dòng usage log.
    private static final int EMBEDDING_BATCH_SIZE = 100;

    private final DocumentVersionRepository documentVersionRepository;

    private final FileStorageService fileStorageService;

    private final DocumentTextRepository documentTextRepository;

    private final TextExtractionService textExtractionService;

    private final ExtractionHelper extractionHelper;

    private final ChunkingService chunkingService;

    private final DocumentChunkRepository documentChunkRepository;

    private final EmbeddingModel embeddingModel;

    private final EmbeddingHelper embeddingHelper;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final VectorStoreHelper vectorStoreHelper;

    private final VectorStoreMapper vectorStoreMapper;

    private final ProcessingMapper processingMapper;

    private final ProcessingHelper processingHelper;


    @Transactional
    public void process(Long versionId) {

        // Khai báo ngoài try để catch luôn truy cập được documentVersion,
        // tránh bị kẹt trạng thái PENDING nếu lỗi xảy ra trước khi vào try.
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

            extractionHelper.validateExtractedText(extractedText);


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

            List<TextSegment> segments = chunkingService.chunk(
                    extractedText,
                    version.getFile().getExtension()
            );


            /*
             * Save document_chunks, rồi gắn id của chunk vừa lưu vào segment để ghi lên Qdrant
             */

            String embeddingModelName = embeddingHelper.getModelName();

            for (TextSegment segment : segments) {
                DocumentChunk savedChunk = documentChunkRepository.save(processingMapper.toDocumentChunk(version, segment));
                vectorStoreMapper.addChunkIdentity(segment, savedChunk, embeddingModelName);
            }


            /*
             * STEP 3:
             * EMBEDDING
             */

            version.setProcessingStep(ProcessingStep.EMBEDDING);

            List<Embedding> embeddings = new ArrayList<>();

            // Mỗi lô là một lượt gọi embedding, AIUsageListener ghi một dòng usage log cho từng lô.
            try (AIUsageContext ignored = AIUsageContext.open(ConversationType.DOCUMENT_INDEXING, null, null)) {

                for (int from = 0; from < segments.size(); from += EMBEDDING_BATCH_SIZE) {

                    List<TextSegment> batch = segments.subList(from, Math.min(from + EMBEDDING_BATCH_SIZE, segments.size()));

                    embeddings.addAll(embeddingHelper.call(() -> embeddingModel.embedAll(batch)).content());
                }
            }

            vectorStoreHelper.call(ErrorCode.VECTOR_UPSERT_FAILED, () -> {
                embeddingStore.addAll(
                        segments.stream().map(vectorStoreMapper::pointId).toList(), // Lấy UUID của từng segment
                        embeddings, // Lấy kiểu vector số
                        segments    // Lấy kiểu gốc, ở đây là TextSegment
                );

                return null; // ở đây phải return do Supplier<T> luôn cần trả về 1 giá trị, nói chung cũng không cần để ý chổ này
            });

            version.setStatus(VersionStatus.READY);

            version.setProcessingStep(null);

        } catch (BusinessException e) {

            processingHelper.handleFailureUnlessAlreadySucceeded(versionId, version, e);
            throw e;


        } catch (Exception e) {

            processingHelper.handleFailureUnlessAlreadySucceeded(versionId, version, e);

            throw new BusinessException(
                    ErrorCode.DOCUMENT_PROCESSING_FAILED,
                    ErrorCode.DOCUMENT_PROCESSING_FAILED.getMessage(),
                    e
            );
        }
    }


}
