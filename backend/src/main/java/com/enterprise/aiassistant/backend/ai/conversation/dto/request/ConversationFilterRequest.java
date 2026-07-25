package com.enterprise.aiassistant.backend.ai.conversation.dto.request;

import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
public class ConversationFilterRequest {

    private String keyword;

    /**
     * newest
     * oldest
     */
    private String sort;

    private ConversationType conversationType;

    private ConversationStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime toDate;
}
