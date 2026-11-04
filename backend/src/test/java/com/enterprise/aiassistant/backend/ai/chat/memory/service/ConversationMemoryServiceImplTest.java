package com.enterprise.aiassistant.backend.ai.chat.memory.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.entity.ConversationMemory;
import com.enterprise.aiassistant.backend.ai.chat.memory.helper.ConversationMemoryHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.mapper.MemoryMapper;
import com.enterprise.aiassistant.backend.ai.chat.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceImplTest {

    @Mock
    private ConversationMemoryRepository conversationMemoryRepository;

    @Mock
    private ChatAssistant chatAssistant;

    @Mock
    private LLMHelper llmHelper;

    // Service khai báo 2 field cùng kiểu ConversationMemoryHelper, Mockito truyền mock này vào cả hai
    @Mock
    private ConversationMemoryHelper memoryHelper;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private MemoryMapper memoryMapper;

    @InjectMocks
    private ConversationMemoryServiceImpl service;

    private AIConversation  conversation;

    private ConversationMemory memory;

    @BeforeEach
    void setUp() {
        conversation = AIConversation.builder().id(1L).build();

        memory = ConversationMemory.builder()
                .conversation(conversation)
                .summarizedContext("Tóm tắt cũ")
                .pendingContext("Lượt cũ")
                .build();
    }

    // ===================== buildMemoryContext =====================

    @Test
    void buildMemoryContext_noMemory_returnsEmptyContext() {
        when(conversationMemoryRepository.findByConversationId(1L)).thenReturn(Optional.empty());
        when(memoryHelper.emptyContext()).thenReturn("No previous conversation context.");

        String result = service.buildMemoryContext(1L);

        assertThat(result).isEqualTo("No previous conversation context.");
    }

    @Test
    void buildMemoryContext_hasMemory_buildsFromBothParts() {
        when(conversationMemoryRepository.findByConversationId(1L)).thenReturn(Optional.of(memory));
        when(memoryHelper.buildContext("Tóm tắt cũ", "Lượt cũ")).thenReturn("Context đã ghép");

        String result = service.buildMemoryContext(1L);

        assertThat(result).isEqualTo("Context đã ghép");
    }

    @Test
    void buildMemoryContext_invalidId_doesNotQueryRepository() {
        // Hàm void không dùng được when(...), phải viết doThrow(...).when(mock).hàm(...)
        doThrow(new AIConversationException(ErrorCode.CONVERSATION_ID_INVALID))
                .when(aiConversationHelper).validateConversationId(0L);

        assertThatThrownBy(() -> service.buildMemoryContext(0L))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_INVALID);

        verifyNoInteractions(conversationMemoryRepository);
    }

    // ===================== appendTurn =====================

    @Test
    void appendTurn_noMemoryYet_createsAndSavesNewMemory() {
        ConversationMemory newMemory = ConversationMemory.builder().conversation(conversation).build();

        when(conversationMemoryRepository.findByConversationIdForUpdate(1L)).thenReturn(Optional.empty());
        when(memoryMapper.toEntity(conversation)).thenReturn(newMemory);
        when(memoryHelper.appendTurn("", "Hỏi", "Đáp")).thenReturn("Lượt mới");
        when(memoryHelper.calculateCharacterCount("", "Lượt mới")).thenReturn(8);

        service.appendTurn(conversation, "Hỏi", "Đáp");

        // Bắt object thật sự được truyền vào save() để kiểm tra nội dung của nó
        ArgumentCaptor<ConversationMemory> captor = ArgumentCaptor.forClass(ConversationMemory.class);
        verify(conversationMemoryRepository).save(captor.capture());

        ConversationMemory saved = captor.getValue();
        assertThat(saved.getConversation()).isSameAs(conversation);
        assertThat(saved.getPendingContext()).isEqualTo("Lượt mới");
        assertThat(saved.getContextCharacterCount()).isEqualTo(8);
    }

    @Test
    void appendTurn_existingMemory_appendsToPendingContext() {
        when(conversationMemoryRepository.findByConversationIdForUpdate(1L)).thenReturn(Optional.of(memory));
        when(memoryHelper.appendTurn("Lượt cũ", "Hỏi", "Đáp")).thenReturn("Lượt cũ + lượt mới");
        when(memoryHelper.calculateCharacterCount("Tóm tắt cũ", "Lượt cũ + lượt mới")).thenReturn(28);

        service.appendTurn(conversation, "Hỏi", "Đáp");

        assertThat(memory.getPendingContext()).isEqualTo("Lượt cũ + lượt mới");
        assertThat(memory.getContextCharacterCount()).isEqualTo(28);
        verify(memoryMapper, never()).toEntity(any());
        verify(conversationMemoryRepository).save(memory);
    }

    // TODO (bạn tự viết): appendTurn_underLimit_doesNotCallLlm
    // Memory đã có sẵn (biến memory), needsSummarization trả false (mock mặc định đã trả false,
    // không cần stub) → service KHÔNG được gọi LLM để nén, nhưng vẫn phải save(memory).
    // Gợi ý: stub giống test appendTurn_existingMemory_appendsToPendingContext, rồi dùng
    // verify(llmHelper, never()).call(any()) và verify(conversationMemoryRepository).save(memory).
    @Test
    void appendTurn_underLimit_doesNotCallLlm() {
        when(conversationMemoryRepository.findByConversationIdForUpdate(1L)).thenReturn(Optional.of(memory));
        when(memoryHelper.appendTurn("Lượt cũ", "Hỏi", "Đáp")).thenReturn("Lượt cũ" + "Lượt mới");
        when(memoryHelper.calculateCharacterCount("Tóm tắt cũ", "Lượt cũ" + "Lượt mới")).thenReturn(28);
        when(memoryHelper.needsSummarization(28)).thenReturn(false);

        service.appendTurn(conversation, "Hỏi", "Đáp");

        verify(llmHelper, never()).call(any());
        verify(conversationMemoryRepository).save(memory);
    }

    @Test
    void appendTurn_reachesLimit_compressesMemory() {
        stubExistingMemoryReachingLimit();
        when(llmHelper.call(any())).thenReturn("  Tóm tắt mới  ");

        service.appendTurn(conversation, "Hỏi", "Đáp");

        assertThat(memory.getSummarizedContext()).isEqualTo("Tóm tắt mới");
        assertThat(memory.getPendingContext()).isEmpty();
        assertThat(memory.getContextCharacterCount()).isEqualTo("Tóm tắt mới".length());
        verify(conversationMemoryRepository).save(memory);
    }

    @Test
    void appendTurn_compressionFails_keepsMemoryAndStillSaves() {
        stubExistingMemoryReachingLimit();
        when(llmHelper.call(any())).thenThrow(new LLMException(ErrorCode.LLM_GENERATION_FAILED));

        service.appendTurn(conversation, "Hỏi", "Đáp");

        // Nén lỗi thì không được mất dữ liệu: giữ nguyên tóm tắt cũ và các lượt chưa nén
        assertThat(memory.getSummarizedContext()).isEqualTo("Tóm tắt cũ");
        assertThat(memory.getPendingContext()).isEqualTo("Lượt cũ + lượt mới");
        verify(conversationMemoryRepository).save(memory);
    }

    @Test
    void appendTurn_blankSummary_keepsMemory() {
        stubExistingMemoryReachingLimit();
        when(llmHelper.call(any())).thenReturn("   ");

        service.appendTurn(conversation, "Hỏi", "Đáp");

        assertThat(memory.getSummarizedContext()).isEqualTo("Tóm tắt cũ");
        assertThat(memory.getPendingContext()).isEqualTo("Lượt cũ + lượt mới");
    }

    @Test
    void appendTurn_invalidTurn_doesNotTouchRepository() {
        doThrow(new AIConversationException(ErrorCode.MESSAGE_CONTENT_REQUIRED))
                .when(memoryHelper).validateTurn(conversation, "Hỏi", " ");

        assertThatThrownBy(() -> service.appendTurn(conversation, "Hỏi", " "))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_CONTENT_REQUIRED);

        verifyNoInteractions(conversationMemoryRepository);
    }

    // ===================== test data =====================

    private void stubExistingMemoryReachingLimit() {
        when(conversationMemoryRepository.findByConversationIdForUpdate(1L)).thenReturn(Optional.of(memory));
        when(memoryHelper.appendTurn("Lượt cũ", "Hỏi", "Đáp")).thenReturn("Lượt cũ + lượt mới");
        when(memoryHelper.calculateCharacterCount("Tóm tắt cũ", "Lượt cũ + lượt mới")).thenReturn(16_000);
        when(memoryHelper.needsSummarization(16_000)).thenReturn(true);
    }
}
