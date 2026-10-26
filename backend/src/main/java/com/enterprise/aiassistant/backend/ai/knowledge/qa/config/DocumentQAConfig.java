package com.enterprise.aiassistant.backend.ai.knowledge.qa.config;

import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.DocumentQAAssistant;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

// Lắp luồng RAG cho Document QA: tìm đoạn liên quan trong các tài liệu đính kèm rồi nối vào prompt.
// Câu hỏi đã được QuestionRewriter viết lại trước khi vào đây.
@Configuration
public class DocumentQAConfig {

    // Khoá của InvocationParameters mà DocumentQAServiceImpl truyền vào mỗi lần gọi DocumentQAAssistant.
    public static final String QUESTION = "question";
    public static final String DOCUMENT_VERSION_IDS = "documentVersionIds";

    // Chỉ lấy tối đa 5 chunks có độ liên quan cao nhất trong các tài liệu đính kèm.
    private static final int CHAT_TOP_K = 5;

    @Bean
    public DocumentQAAssistant documentQAAssistant(
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            VectorStoreHelper vectorStoreHelper,
            PromptBuilderService promptBuilderService
    ) {

        // Lọc theo version ngay trong Qdrant, không lấy top-K toàn kho rồi mới lọc (có thể mất hết kết quả
        // khi tài liệu khác điểm cao hơn); ngưỡng điểm vẫn như semantic search.
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(CHAT_TOP_K)
                .minScore(vectorStoreHelper.minScore())
                .dynamicFilter(query -> metadataKey(VectorStoreMapper.DOCUMENT_VERSION_ID).isIn(documentVersionIds(query)))
                .build();

        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                // LangChain4j mặc định lấy cả user message (toàn bộ prompt document-qa.txt) đi tìm;
                // đổi về đúng câu hỏi.
                .queryTransformer(query -> List.of(Query.from(query.metadata().invocationParameters().get(QUESTION), query.metadata())))
                // Không có tài liệu đọc được thì không embed/tìm (bộ lọc isIn không nhận danh sách rỗng),
                // prompt vẫn ghi "No relevant passages were found.".
                .queryRouter(query -> documentVersionIds(query).isEmpty() ? List.of() : List.of(contentRetriever))
                // Nối đoạn trích sau dòng "Document excerpts:", cách nhau "---", giữ dòng trống cuối như prompt
                // cũ. Không dùng DefaultContentInjector vì nó chèn thêm "Answer using the following information:".
                .contentInjector((contents, userMessage) -> UserMessage.from(
                        ((UserMessage) userMessage).singleText()
                                + promptBuilderService.buildDocumentExcerpts(
                                        contents.stream().map(content -> content.textSegment().text()).toList())
                                + "\n"
                ))
                .build();

        return AiServices.builder(DocumentQAAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }

    private static List<Long> documentVersionIds(Query query) {
        return query.metadata().invocationParameters().get(DOCUMENT_VERSION_IDS);
    }
}
