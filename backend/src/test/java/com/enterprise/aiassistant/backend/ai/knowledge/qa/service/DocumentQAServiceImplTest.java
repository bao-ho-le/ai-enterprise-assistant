package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.mapper.VectorStoreMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.config.DocumentQAConfig;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentQAServiceImplTest {

    @Mock
    private AIConversationDocumentRepository conversationDocumentRepository;

    @Mock
    private AIMessageRepository messageRepository;

    @Mock
    private AIMessageSourceRepository messageSourceRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    // Dùng mapper thật: nó chỉ đọc metadata của Content, không có gì cần giả lập
    @Spy
    private VectorStoreMapper vectorStoreMapper = new VectorStoreMapper();

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private DocumentQAAssistant documentQAAssistant;

    @Mock
    private LLMHelper llmHelper;

    @Mock
    private AIMessageMapper messageMapper;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private DocumentAuthorizationService documentAuthorizationService;

    @InjectMocks
    private DocumentQAServiceImpl service;

    @Captor
    private ArgumentCaptor<InvocationParameters> parametersCaptor;

    @Captor
    private ArgumentCaptor<List<AIMessageSource>> sourcesCaptor;

    private static final String QUESTION = "Hợp đồng lao động có mấy loại?";

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    private final Document document1 = Document.builder().id(100L).build();
    private final Document document2 = Document.builder().id(200L).build();

    private final AIConversationDocument link1 = AIConversationDocument.builder()
            .documentVersion(DocumentVersion.builder().id(1L).document(document1).build())
            .build();
    private final AIConversationDocument link2 = AIConversationDocument.builder()
            .documentVersion(DocumentVersion.builder().id(2L).document(document2).build())
            .build();

    private final AIMessage assistantMessage = AIMessage.builder().id(11L).content("Có 2 loại").build();

    // Bảo mật: user bị thu hồi quyền đọc tài liệu 200 sau khi đã gắn nó vào hội thoại.
    // Version 2 (thuộc tài liệu 200) không được gửi xuống bước tìm kiếm của LLM.
    //
    // Danh sách version được đóng gói trong lambda truyền cho llmHelper.call(...). Nếu chỉ
    // thenReturn thì lambda không bao giờ chạy và không thấy được service gửi gì. thenAnswer
    // lấy lambda ra và chạy nó, nhờ đó documentQAAssistant.answer(...) thật sự được gọi.
    @Test
    void answer_revokedDocument_excludesItsVersionFromRetrieval() {
        when(conversationDocumentRepository.findByAiConversationIdWithDocument(1L)).thenReturn(List.of(link1, link2));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document1, document2)))
                .thenReturn(Set.of(100L));
        when(conversationMemoryService.buildMemoryContext(1L)).thenReturn("USER: Xin chào");
        when(llmHelper.call(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(documentQAAssistant.answer(eq(QUESTION), eq("USER: Xin chào"), any()))
                .thenReturn(answerWith(List.of()));
        stubSaveAssistantMessage();

        assertThat(service.answer(conversation, QUESTION)).isSameAs(assistantMessage);

        verify(documentQAAssistant).answer(eq(QUESTION), eq("USER: Xin chào"), parametersCaptor.capture());
        InvocationParameters parameters = parametersCaptor.getValue();
        assertThat(parameters.<List<Long>>get(DocumentQAConfig.DOCUMENT_VERSION_IDS)).containsExactly(1L);
        assertThat(parameters.<String>get(DocumentQAConfig.QUESTION)).isEqualTo(QUESTION);
    }

    @Test
    void answer_llmReturnsSources_savesEachSourceWithChunkAndScore() {
        DocumentChunk chunk = DocumentChunk.builder().id(7L).build();
        // Đoạn tài liệu LLM đã dùng: metadata chứa chunkId/versionId, relevance score 0.9
        TextSegment segment = TextSegment.from("Có 2 loại hợp đồng",
                Metadata.from(Map.of(VectorStoreMapper.CHUNK_ID, 7L, VectorStoreMapper.DOCUMENT_VERSION_ID, 1L)));
        Content hit = Content.from(segment, Map.of(ContentMetadata.SCORE, 0.9));

        stubReadableDocuments();
        when(llmHelper.call(any())).thenReturn(answerWith(List.of(hit)));
        stubSaveAssistantMessage();
        when(documentChunkRepository.getReferenceById(7L)).thenReturn(chunk);

        service.answer(conversation, QUESTION);

        verify(messageSourceRepository).saveAll(sourcesCaptor.capture());
        assertThat(sourcesCaptor.getValue()).singleElement().satisfies(source -> {
            assertThat(source.getAiMessage()).isSameAs(assistantMessage);
            assertThat(source.getDocumentChunk()).isSameAs(chunk);
            assertThat(source.getChunkId()).isEqualTo(7L);
            assertThat(source.getDocumentVersionId()).isEqualTo(1L);
            // relevance 0.9 = (cosine + 1) / 2 → cosine = 0.8
            assertThat(source.getScore()).isCloseTo(0.8, within(1e-9));
        });
    }

    @Test
    void answer_noRelevantSources_savesAnswerWithoutSources() {
        stubReadableDocuments();
        when(llmHelper.call(any())).thenReturn(answerWith(List.of()));
        stubSaveAssistantMessage();

        assertThat(service.answer(conversation, QUESTION)).isSameAs(assistantMessage);

        verifyNoInteractions(messageSourceRepository);
    }

    // Tài liệu đã bị xóa mềm: dừng ngay, không tốn lượt gọi LLM
    @Test
    void answer_attachedDocumentDeleted_throwsBeforeCallingLlm() {
        when(conversationDocumentRepository.findByAiConversationIdWithDocument(1L)).thenReturn(List.of(link1));
        doThrow(new AIConversationException(ErrorCode.ATTACHED_DOCUMENT_DELETED))
                .when(aiConversationHelper).validateAttachedDocumentsNotDeleted(List.of(link1));

        assertThatThrownBy(() -> service.answer(conversation, QUESTION))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ATTACHED_DOCUMENT_DELETED);

        verifyNoInteractions(llmHelper, messageRepository);
    }

    // LLM lỗi thì trả null cho AIMessageServiceImpl tự xử lý, không lưu câu trả lời rỗng
    @Test
    void answer_llmFails_returnsNullAndSavesNothing() {
        stubReadableDocuments();
        when(llmHelper.call(any())).thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        assertThat(service.answer(conversation, QUESTION)).isNull();

        verifyNoInteractions(messageRepository, messageSourceRepository);
    }

    // ===================== Helper =====================

    private void stubReadableDocuments() {
        when(conversationDocumentRepository.findByAiConversationIdWithDocument(1L)).thenReturn(List.of(link1, link2));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document1, document2)))
                .thenReturn(Set.of(100L, 200L));
    }

    private void stubSaveAssistantMessage() {
        AIMessage unsaved = AIMessage.builder().content("Có 2 loại").build();
        when(messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, "Có 2 loại")).thenReturn(unsaved);
        when(messageRepository.save(unsaved)).thenReturn(assistantMessage);
    }

    private Result<String> answerWith(List<Content> sources) {
        return Result.<String>builder().content("Có 2 loại").sources(sources).build();
    }
}
