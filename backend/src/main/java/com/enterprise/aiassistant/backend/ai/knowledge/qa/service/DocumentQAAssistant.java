package com.enterprise.aiassistant.backend.ai.knowledge.qa.service;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// AI Service riêng cho Document QA, tách khỏi ChatAssistant vì có gắn RetrievalAugmentor (DocumentQAConfig):
// trước khi gửi Gemini, LangChain4j tự tìm đoạn liên quan trong Qdrant và nối vào cuối prompt
// (dòng cuối của document-qa.txt là "Document excerpts:").
public interface DocumentQAAssistant {

    // parameters mang câu hỏi và các version tài liệu đính kèm cho bước tìm kiếm (khoá ở DocumentQAConfig).
    // Trả Result để lấy token usage ghi AI usage log và các đoạn đã dùng (sources) làm evidence.
    @UserMessage(fromResource = "prompts/document-qa.txt")
    Result<String> answer(
            @V("question") String question,
            @V("conversationMemory") String conversationMemory,
            InvocationParameters parameters
    );
}
