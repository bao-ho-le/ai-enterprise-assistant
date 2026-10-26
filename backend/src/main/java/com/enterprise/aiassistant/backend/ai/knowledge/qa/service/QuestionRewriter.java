package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import org.springframework.stereotype.Service;

import java.util.List;

// Chạy trước Document QA retrieval: dùng ConversationMemory hiện có (không query lại AIMessage)
// để biến câu phụ thuộc ngữ cảnh (vd: "nói rõ về nó") thành câu hỏi độc lập, KHÔNG trả lời câu hỏi.
// Kế thừa CompressingQueryTransformer của LangChain4j: lớp gốc ghép prompt ({{query}}, {{chatMemory}})
// rồi gọi model; ở đây chỉ đổi nguồn lịch sử sang memory 2 tầng (xem format).
@Service
public class QuestionRewriter extends CompressingQueryTransformer {

    private final ConversationMemoryService conversationMemoryService;
    private final LLMHelper llmHelper;

    private final AIConversationHelper aiConversationHelper;
    private final AIMessageHelper messageHelper;

    public QuestionRewriter(
            ChatModel chatModel,
            ConversationMemoryService conversationMemoryService,
            LLMHelper llmHelper,
            AIConversationHelper aiConversationHelper,
            AIMessageHelper messageHelper
    ) {
        super(chatModel, PromptBuilderService.load("prompts/question-rewrite.txt"));
        this.conversationMemoryService = conversationMemoryService;
        this.llmHelper = llmHelper;
        this.aiConversationHelper = aiConversationHelper;
        this.messageHelper = messageHelper;
    }

    // Nếu rewrite thất bại hoặc không cần thiết, trả về nguyên văn userMessage.
    public String rewrite(
            AIConversation conversation,
            String userMessage
    ) {

        validateRewriteRequest(conversation, userMessage);

        String conversationMemory = conversationMemoryService.buildMemoryContext(conversation.getId());

        // Lớp gốc đọc lịch sử từ ChatMemory (danh sách message) của LangChain4j, không có thì bỏ qua
        // không viết lại. Memory 2 tầng đã là text sẵn nên gói thành 1 message, luôn có nội dung
        // (memory trống vẫn là "No previous conversation context.") nên lượt nào cũng gọi model như cũ.
        Query query = Query.from(userMessage, Metadata.from(
                UserMessage.from(userMessage),
                conversation.getId(),
                List.of(UserMessage.from(conversationMemory))
        ));

        try {
            String rewrittenQuestion = llmHelper.call(() -> transform(query)).iterator().next().text();

            return normalize(rewrittenQuestion, userMessage);

        } catch (LLMException e) {
            // Rewrite chỉ hỗ trợ retrieval, không phải bước bắt buộc: lỗi (kể cả model trả rỗng) thì dùng
            // nguyên văn câu hỏi, Document QA vẫn chạy như hành vi cũ thay vì làm cả lượt chat lỗi.
            return userMessage;
        }
    }

    // Đưa nguyên văn text memory 2 tầng vào {{chatMemory}}, không định dạng lại theo từng message.
    @Override
    protected String format(List<ChatMessage> chatMemory) {
        return ((UserMessage) chatMemory.get(0)).singleText();
    }


    // Helper

    // Model đôi khi bọc câu trả lời trong dấu ngoặc kép; chỉ chuẩn hoá whitespace/quote
    // bao ngoài, không biến đổi nội dung câu hỏi.
    private String normalize(String rewrittenQuestion, String userMessage) {

        if (rewrittenQuestion == null || rewrittenQuestion.isBlank()) {
            return userMessage;
        }

        String normalized = rewrittenQuestion.trim();

        if (normalized.length() > 1 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        return normalized.isBlank() ? userMessage : normalized;
    }

    private void validateRewriteRequest(
            AIConversation conversation,
            String userMessage
    ) {

        if (conversation == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        aiConversationHelper.validateConversationId(conversation.getId());
        messageHelper.validateContent(userMessage);
    }
}
