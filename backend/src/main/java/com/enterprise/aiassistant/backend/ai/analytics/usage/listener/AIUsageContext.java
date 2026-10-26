package com.enterprise.aiassistant.backend.ai.analytics.usage.listener;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

// AIUsageListener chỉ thấy request/response gửi Gemini, không biết lượt gọi thuộc luồng nào. Điểm bắt
// đầu của mỗi luồng (gửi tin nhắn chat, generation, semantic search, xử lý tài liệu) mở context này
// quanh các lời gọi model, listener đọc ra khi ghi usage log. Lời gọi model chạy cùng thread nên
// dùng ThreadLocal; dùng với try-with-resources để luôn được dọn khi ra khỏi khối:
//   try (AIUsageContext ignored = AIUsageContext.open(type, conversationId, null)) { ... }
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AIUsageContext implements AutoCloseable {

    private static final ThreadLocal<AIUsageContext> CURRENT = new ThreadLocal<>();

    private final ConversationType conversationType;
    private final Long conversationId;
    private final Long generationId;

    @Getter(AccessLevel.NONE)
    private final AIUsageContext previous;

    public static AIUsageContext open(ConversationType conversationType, Long conversationId, Long generationId) {
        AIUsageContext context = new AIUsageContext(conversationType, conversationId, generationId, CURRENT.get());
        CURRENT.set(context);
        return context;
    }

    public static Optional<AIUsageContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
