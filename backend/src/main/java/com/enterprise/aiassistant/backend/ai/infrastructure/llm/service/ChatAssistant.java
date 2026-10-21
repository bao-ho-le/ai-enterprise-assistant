package com.enterprise.aiassistant.backend.ai.infrastructure.llm.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// AI Service của LangChain4j: chỉ khai báo interface, LangChain4j tự sinh phần thực thi
// (đọc file prompt trong resources, điền {{biến}} từ @V, gửi Gemini, trả kết quả).
// Bean được tạo ở GeminiConfig. Nơi gọi bọc qua LLMHelper.call() để lỗi thành LLMException.
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

    // Trả Result thay vì String để lấy token usage ghi AI usage log.
    @UserMessage(fromResource = "prompts/document-qa.txt")
    Result<String> answerFromDocuments(
            @V("question") String question,
            @V("conversationMemory") String conversationMemory,
            @V("documentExcerpts") String documentExcerpts
    );

    // Chạy trước retrieval của Document QA: viết lại thành câu hỏi độc lập, không trả lời.
    @UserMessage(fromResource = "prompts/question-rewrite.txt")
    String rewriteQuestion(
            @V("userMessage") String userMessage,
            @V("conversationMemory") String conversationMemory
    );

    // Nén memory nội bộ, không phải câu trả lời cho user. Độ dài mục tiêu (6000 ký tự)
    // ghi thẳng trong file prompt, khớp với ngưỡng nén ở ConversationMemoryHelper.
    @UserMessage(fromResource = "prompts/conversation-memory-summary.txt")
    String compressMemory(
            @V("summarizedContext") String summarizedContext,
            @V("pendingContext") String pendingContext
    );

    // Chỉ phân loại, không trả lời; output được parse thẳng vào enum Intent.
    @UserMessage(fromResource = "prompts/intent-classification.txt")
    String classifyIntent(@V("message") String message);
}
