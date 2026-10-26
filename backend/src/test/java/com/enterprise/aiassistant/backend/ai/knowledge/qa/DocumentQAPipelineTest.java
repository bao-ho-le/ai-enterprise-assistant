package com.enterprise.aiassistant.backend.ai.knowledge.qa;

import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.helper.VectorStoreHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.config.DocumentQAConfig;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.DocumentQAAssistant;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper.CHUNK_ID;
import static com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper.DOCUMENT_VERSION_ID;
import static org.junit.jupiter.api.Assertions.*;

// Luồng RAG lắp ở DocumentQAConfig, chạy với model giả và store trong bộ nhớ (không gọi Gemini/Qdrant):
// chỉ câu hỏi (không phải cả prompt) được đem đi tìm, chỉ tìm trong version đính kèm và trên ngưỡng điểm,
// đoạn trích được nối vào prompt đúng định dạng cũ, nguồn trích dẫn đọc lại được chunkId/version/cosine.
class DocumentQAPipelineTest {

    private final List<String> embeddedTexts = new ArrayList<>();
    private String sentPrompt;

    // Câu hỏi nào cũng embed thành (1, 0), nên cosine với chunk = thành phần đầu vector của chunk.
    private final EmbeddingModel embeddingModel = new EmbeddingModel() {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            segments.forEach(segment -> embeddedTexts.add(segment.text()));
            return Response.from(segments.stream().map(segment -> Embedding.from(new float[]{1f, 0f})).toList());
        }
    };

    private final ChatModel chatModel = new ChatModel() {
        @Override
        public ChatResponse doChat(ChatRequest request) {
            sentPrompt = ((UserMessage) request.messages().get(0)).singleText();
            return ChatResponse.builder().aiMessage(AiMessage.from("ANSWER")).build();
        }
    };

    private final DocumentQAAssistant documentQAAssistant = createAssistant();
    private final VectorStoreMapper vectorStoreMapper = new VectorStoreMapper();

    @Test
    void searchesAttachedVersionsWithTheQuestionAndKeepsThePromptFormat() {

        Result<String> answer = documentQAAssistant.answer("Q?", "MEM", parameters("Q?", List.of(7L)));

        assertEquals(List.of("Q?"), embeddedTexts);
        assertEquals("ANSWER", answer.content());
        assertTrue(sentPrompt.startsWith("Answer the following question using only the provided document excerpts. Question: Q?\n"),
                () -> "Prompt was changed:\n" + sentPrompt);
        assertTrue(sentPrompt.endsWith("Conversation memory:\nMEM\n\nDocument excerpts:\nCHUNK-1\n---\nCHUNK-2\n"),
                () -> "Prompt was changed:\n" + sentPrompt);

        assertEquals(List.of(1L, 2L), answer.sources().stream().map(vectorStoreMapper::chunkId).toList());
        assertEquals(List.of(7L, 7L), answer.sources().stream().map(vectorStoreMapper::documentVersionId).toList());
        assertEquals(0.9, vectorStoreMapper.cosineScore(answer.sources().get(0)), 1e-6);
    }

    @Test
    void noReadableDocumentSkipsTheSearch() {

        Result<String> answer = documentQAAssistant.answer("Q?", "MEM", parameters("Q?", List.of()));

        assertTrue(embeddedTexts.isEmpty());
        assertTrue(answer.sources().isEmpty());
        assertTrue(sentPrompt.endsWith("Document excerpts:\nNo relevant passages were found.\n"),
                () -> "Prompt was changed:\n" + sentPrompt);
    }

    private DocumentQAAssistant createAssistant() {

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        addChunk(embeddingStore, 1L, 7L, 0.9);
        addChunk(embeddingStore, 2L, 7L, 0.8);
        addChunk(embeddingStore, 3L, 8L, 0.95); // version không đính kèm
        addChunk(embeddingStore, 4L, 7L, 0.5);  // dưới ngưỡng cosine 0.65

        QdrantProperties properties = new QdrantProperties();
        properties.setScoreThreshold(0.65f);

        return new DocumentQAConfig().documentQAAssistant(
                chatModel, embeddingModel, embeddingStore, new VectorStoreHelper(properties), new PromptBuilderService());
    }

    private void addChunk(InMemoryEmbeddingStore<TextSegment> embeddingStore, long chunkId, long versionId, double cosine) {
        embeddingStore.add(
                Embedding.from(new float[]{(float) cosine, (float) Math.sqrt(1 - cosine * cosine)}),
                TextSegment.from("CHUNK-" + chunkId, new Metadata().put(CHUNK_ID, chunkId).put(DOCUMENT_VERSION_ID, versionId))
        );
    }

    private InvocationParameters parameters(String question, List<Long> documentVersionIds) {
        return InvocationParameters.from(Map.of(
                DocumentQAConfig.QUESTION, question,
                DocumentQAConfig.DOCUMENT_VERSION_IDS, documentVersionIds
        ));
    }
}
