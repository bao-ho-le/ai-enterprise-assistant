package com.enterprise.aiassistant.backend.ai.infrastructure.prompt;

import com.enterprise.aiassistant.backend.ai.infrastructure.llm.service.ChatAssistant;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.FormGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.ReportGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static dev.langchain4j.service.AiServices.create;
import static org.junit.jupiter.api.Assertions.*;

// Mỗi file trong resources/prompts phải điền được đủ biến. Sai tên biến thì LangChain4j ném lỗi,
// LLMHelper đổi thành LLMException và intent/rewrite/nén memory lặng lẽ rơi vào nhánh fallback,
// nên phải bắt ở đây. Không gọi Gemini: ChatModel giả chỉ giữ lại prompt nhận được.
class PromptTemplatesTest {

    private String sentPrompt;

    private final ChatModel capturingModel = new ChatModel() {
        @Override
        public ChatResponse doChat(ChatRequest request) {
            sentPrompt = ((UserMessage) request.messages().get(0)).singleText();
            return ChatResponse.builder().aiMessage(AiMessage.from("ok")).build();
        }
    };

    private final ChatAssistant chatAssistant = create(ChatAssistant.class, capturingModel);
    private final PromptBuilderService promptBuilderService = new PromptBuilderService();

    @Test
    void chatAssistantPromptsFillEveryVariable() {

        assertSent(() -> chatAssistant.generalChat("Xin chào"), "Xin chào");
        assertSent(() -> chatAssistant.classifyIntent("Tóm tắt tài liệu"), "Tóm tắt tài liệu");
        assertSent(() -> chatAssistant.summarizeConversation("Nãy giờ nói gì?", "MEMORY-1"), "Nãy giờ nói gì?", "MEMORY-1");
        assertSent(() -> chatAssistant.rewriteQuestion("Cái đó là gì?", "MEMORY-2"), "Cái đó là gì?", "MEMORY-2");
        assertSent(() -> chatAssistant.compressMemory("OLD-SUMMARY", "NEW-TURNS"), "OLD-SUMMARY", "NEW-TURNS");
        assertSent(() -> chatAssistant.answerFromDocuments("Doanh thu Q3?", "MEMORY-3",
                promptBuilderService.buildDocumentExcerpts(List.of("CHUNK-A", "CHUNK-B"))),
                "Doanh thu Q3?", "MEMORY-3", "CHUNK-A\n---\nCHUNK-B");
    }

    @Test
    void generationPromptsFillEveryVariable() {

        EmailGenerationInput email = new EmailGenerationInput();
        email.setPurpose("Mời họp");
        email.setTone("Formal");
        email.setLanguage("Vietnamese");
        assertFilled(promptBuilderService.buildEmailPrompt(email), "Mời họp", "Tone: Formal. Use formal");

        ReportGenerationInput report = new ReportGenerationInput();
        report.setTitle("Báo cáo Q3");
        report.setFromDate("2026-07-01");
        assertFilled(promptBuilderService.buildReportPrompt(report, "DOC-CONTEXT"),
                "titled \"Báo cáo Q3\"", "2026-07-01 to …", "DOC-CONTEXT");

        SummaryGenerationInput summary = new SummaryGenerationInput();
        summary.setStyle("BULLET_POINTS");
        summary.setIncludeActionItems(true);
        assertFilled(promptBuilderService.buildSummaryPrompt(summary, "DOC-CONTEXT"),
                "action items section: Yes", "Presentation style: BULLET_POINTS. Present the summary as bullet points", "DOC-CONTEXT");

        FormGenerationInput form = new FormGenerationInput();
        form.setPurpose("Đăng ký nghỉ phép");
        assertFilled(promptBuilderService.buildFormPrompt(form), "Đăng ký nghỉ phép", "Infer reasonable fields");
    }

    // Dữ liệu chứa đúng tên biến của prompt phải được giữ nguyên (SinglePassPromptTemplateFactory),
    // không bị thay bằng giá trị của biến khác như bộ điền mặc định của LangChain4j.
    @Test
    void valuesContainingVariableNamesStayLiteral() {

        chatAssistant.summarizeConversation("Hỏi {{conversationMemory}}", "USER:\n{{message}} {{current_date}}");
        assertTrue(sentPrompt.contains("Hỏi {{conversationMemory}}"), () -> "Message was changed:\n" + sentPrompt);
        assertTrue(sentPrompt.contains("USER:\n{{message}} {{current_date}}"), () -> "Memory was changed:\n" + sentPrompt);

        ReportGenerationInput report = new ReportGenerationInput();
        report.setTitle("Báo cáo Q3");
        String prompt = promptBuilderService.buildReportPrompt(report, "Hợp đồng {{title}} ngày {{current_date}}");
        assertTrue(prompt.contains("Hợp đồng {{title}} ngày {{current_date}}"), () -> "Document text was changed:\n" + prompt);
    }

    private void assertSent(Supplier<Object> call, String... expectedParts) {
        sentPrompt = null;
        call.get();
        assertFilled(sentPrompt, expectedParts);
    }

    private void assertFilled(String prompt, String... expectedParts) {
        assertNotNull(prompt);
        assertFalse(prompt.contains("{{"), () -> "Unfilled variable in prompt:\n" + prompt);
        for (String part : expectedParts) {
            assertTrue(prompt.contains(part), () -> "Missing \"" + part + "\" in prompt:\n" + prompt);
        }
    }
}
