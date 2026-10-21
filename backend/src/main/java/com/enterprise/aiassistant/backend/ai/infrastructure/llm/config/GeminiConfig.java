package com.enterprise.aiassistant.backend.ai.infrastructure.llm.config;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Configuration
public class GeminiConfig {


    @Value("${gemini.llm-model.api-key}")
    private String apiKey;


    @Value("${gemini.llm-model.model-name}")
    private String modelName;

    // Bật để in nguyên request/response gửi Gemini (API key tự được che); mặc định tắt.
    @Value("${gemini.llm-model.log-requests:false}")
    private boolean logRequests;

    @Bean
    public ChatModel geminiChatModel() {

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequestsAndResponses(logRequests)
                .build();
    }

    @Bean
    public ChatAssistant chatAssistant(ChatModel chatModel) {

        // AI Services đọc file prompt bằng charset mặc định của JVM chứ không cố định UTF-8;
        // prompt có tiếng Việt và "—", "•" nên chặn ngay lúc khởi động thay vì gửi prompt lỗi font.
        if (!StandardCharsets.UTF_8.equals(Charset.defaultCharset())) {
            throw new IllegalStateException(
                    "JVM must run with UTF-8 (-Dfile.encoding=UTF-8) to read prompt templates correctly");
        }

        return AiServices.create(ChatAssistant.class, chatModel);
    }
}
