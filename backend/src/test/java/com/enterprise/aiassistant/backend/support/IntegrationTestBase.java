package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.infrastructure.vectorstore.manager.QdrantCollectionManager;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
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
        ChatModel fakeChatModel() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest request) {
                    return ChatResponse.builder().aiMessage(AiMessage.from(FAKE_ANSWER)).build();
                }
            };
        }
    }
}
