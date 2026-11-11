package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageListener;
import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.manager.QdrantCollectionManager;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

// Lớp nền cho mọi integration test: khởi động toàn bộ app Spring như khi chạy thật, nhưng
//   - database: Postgres thật chạy trong Docker (Testcontainers), Flyway tạo schema như production
//   - Qdrant: thay bằng mock, vì không có Qdrant trong môi trường test
//   - Gemini: thay bằng ChatModel giả luôn trả "FAKE ANSWER" (không tốn tiền, kết quả cố định)
//   - đăng nhập: không cần JWT, dùng loginAs(user, permission...) gắn vào từng request
//
// Mọi class con dùng chung một cấu hình nên Spring chỉ khởi động app (và Postgres) MỘT lần cho cả
// lượt chạy test, các class sau dùng lại context đã có (context caching).
// Cần Docker đang chạy trên máy.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({IntegrationTestBase.TestBeans.class, TestDataFactory.class})
public abstract class IntegrationTestBase {

    public static final String FAKE_ANSWER = "FAKE ANSWER";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestDataFactory data;

    // Mặc định trả FAKE_ANSWER; test cần giả lập LLM lỗi thì gọi fakeChatModel.failWith(...)
    @Autowired
    protected FakeChatModel fakeChatModel;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Lúc app khởi động, QdrantCollectionManager gọi Qdrant để tạo collection.
    // Không có Qdrant thì app không khởi động được, nên thay bằng mock không làm gì.
    @MockitoBean
    private QdrantCollectionManager qdrantCollectionManager;

    // Các test dùng chung một database. Dọn dữ liệu TRƯỚC mỗi test để test không phụ thuộc nhau,
    // kể cả khi test trước bị fail giữa chừng. Giữ lại dữ liệu seed của Flyway (admin, user, role).
    @BeforeEach
    void cleanDatabase() {
        fakeChatModel.reset();
        jdbcTemplate.execute("""
                TRUNCATE ai_usage_logs, ai_message_sources, ai_messages, conversation_memories,
                         ai_conversation_documents, generations, generated_content, ai_conversations,
                         document_chunks, document_texts, document_accesses, document_versions, documents, files
                RESTART IDENTITY CASCADE
                """);
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE 'it\\_%'");
        jdbcTemplate.update("DELETE FROM departments WHERE name LIKE 'IT %'");
    }

    // Giả lập user đã đăng nhập với đúng các permission truyền vào, gắn vào request:
    //   mockMvc.perform(get("/api/v1/ai-usage").with(loginAs(user, Permission.AI_USAGE_READ_SELF)))
    // Không dùng @WithMockUser vì CurrentUserService cần principal là UserPrincipal.
    protected static RequestPostProcessor loginAs(User user, Permission... permissions) {
        return user(UserPrincipal.from(user, Set.of(permissions)));
    }

    // Đếm số dòng thật trong DB, dùng để kiểm tra dữ liệu đã được lưu, xóa hoặc rollback
    protected long countRows(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        // Cơ chế auto-configure DataSource của Spring Boot hoạt động như sau:
        // Có bean JdbcConnectionDetails?
        //   ├─ CÓ  → dùng nó (URL/user/password của container)
        //   └─ KHÔNG → đọc spring.datasource.url/username/password trong properties
        // Mà @ServiceConnection sẽ dùng PostgreSQLContainer để đăng kí bean JdbcConnectionDetails, để DataSource dùng
        // Do đó cấu hình PostgreSQL bên dưới sẽ được dùng cho test thay vì cấu hình bên trong .properties
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }

        // @Primary: ưu tiên bean này hơn Gemini thật (GeminiConfig) ở mọi chỗ cần ChatModel.
        // Chỉ cần override doChat: mọi cách gọi ChatModel (chat(String), AI Service...) đều đi qua đây.
        @Bean
        @Primary
        FakeChatModel fakeChatModel(AIUsageListener aiUsageListener) {
            return new FakeChatModel(aiUsageListener);
        }
    }

    // Gắn AIUsageListener giống Gemini thật (GeminiConfig) nên luồng ghi usage log vẫn chạy thật
    public static class FakeChatModel implements ChatModel {

        private final List<ChatModelListener> listeners;
        private volatile RuntimeException failure;
        private volatile String lastPrompt;

        FakeChatModel(ChatModelListener listener) {
            this.listeners = List.of(listener);
        }

        public void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        public void reset() {
            this.failure = null;
            this.lastPrompt = null;
        }

        // Nội dung của lần gọi AI gần nhất, để kiểm tra prompt có chứa đúng dữ liệu không
        public String lastPrompt() {
            return lastPrompt;
        }

        @Override
        public List<ChatModelListener> listeners() {
            return listeners;
        }

        // Giống Gemini thật: request luôn có tên model, nếu không AIUsageListener không ghi được log lỗi
        @Override
        public ChatRequestParameters defaultRequestParameters() {
            return ChatRequestParameters.builder().modelName("fake-model").build();
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            lastPrompt = request.messages().toString();
            if (failure != null) {
                throw failure;
            }
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(FAKE_ANSWER))
                    .modelName("fake-model")
                    .tokenUsage(new TokenUsage(10, 5))
                    .build();
        }
    }
}
