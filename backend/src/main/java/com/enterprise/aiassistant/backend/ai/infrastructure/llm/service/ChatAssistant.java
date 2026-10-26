package com.enterprise.aiassistant.backend.ai.infrastructure.llm.service;

import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// AI Service của LangChain4j: chỉ khai báo interface, LangChain4j tự sinh phần thực thi
// (đọc file prompt trong resources, điền {{biến}} từ @V, gửi Gemini, trả kết quả).
// Bean được tạo ở GeminiConfig. Nơi gọi bọc qua LLMHelper.call() để lỗi thành LLMException.
// Document QA có AI Service riêng (DocumentQAAssistant) vì cần gắn bước tìm tài liệu.
public interface ChatAssistant {

    // Câu hỏi không cần tài liệu đính kèm, trả lời bằng kiến thức chung.
    @UserMessage(fromResource = "prompts/general-chat.txt")
    String generalChat(@V("message") String message);

    // User hỏi về chính cuộc hội thoại (intent CONVERSATION_SUMMARY): chỉ dựa trên memory.
    @UserMessage(fromResource = "prompts/conversation-summary.txt")
    String summarizeConversation(
            @V("message") String message,
            @V("conversationMemory") String conversationMemory
    );

    // Nén memory nội bộ, không phải câu trả lời cho user. Độ dài mục tiêu (6000 ký tự)
    // ghi thẳng trong file prompt, khớp với ngưỡng nén ở ConversationMemoryHelper.
    @UserMessage(fromResource = "prompts/conversation-memory-summary.txt")
    String compressMemory(
            @V("summarizedContext") String summarizedContext,
            @V("pendingContext") String pendingContext
    );

    // Chỉ phân loại, không trả lời. Trả thẳng enum: LangChain4j gửi kèm JSON schema (xem GeminiConfig)
    // để Gemini chỉ trả được 1 trong 4 giá trị, rồi tự đổi kết quả sang Intent.
    @UserMessage(fromResource = "prompts/intent-classification.txt")
    Intent classifyIntent(@V("message") String message);
}
