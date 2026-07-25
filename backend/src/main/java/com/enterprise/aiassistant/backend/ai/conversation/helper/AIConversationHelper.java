package com.enterprise.aiassistant.backend.ai.conversation.helper;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;

@Component
public class AIConversationHelper {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MIN_RECENT_MESSAGES_LIMIT = 1;
    private static final int MAX_RECENT_MESSAGES_LIMIT = 100;

    private final Set<String> ALLOWED_SORTS = Set.of("newest", "oldest");

    public void validateFilter(ConversationFilterRequest filter) {

        if (filter == null) {
            return;
        }

        if (filter.getSort() != null
                && !ALLOWED_SORTS.contains(filter.getSort().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_SORT_OPTION);
        }

        if (filter.getFromDate() != null
                && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (filter.getKeyword() != null && filter.getKeyword().length() > 255) {
            throw new BusinessException(ErrorCode.KEYWORD_TOO_LONG);
        }
    }

    public void validateConversationId(Long conversationId) {

        if (conversationId == null) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_REQUIRED);
        }

        if (conversationId <= 0) {
            throw new AIConversationException(ErrorCode.CONVERSATION_ID_INVALID);
        }
    }

    public void validateRecentMessagesLimit(int recentMessagesLimit) {

        if (recentMessagesLimit < MIN_RECENT_MESSAGES_LIMIT
                || recentMessagesLimit > MAX_RECENT_MESSAGES_LIMIT) {
            throw new AIConversationException(ErrorCode.RECENT_MESSAGES_LIMIT_INVALID);
        }
    }

    public void validatePageable(Pageable pageable) {

        if (pageable == null) {
            throw new AIConversationException(ErrorCode.PAGEABLE_REQUIRED);
        }

        if (pageable.getPageNumber() < 0) {
            throw new AIConversationException(ErrorCode.PAGE_NUMBER_INVALID);
        }

        if (pageable.getPageSize() <= 0) {
            throw new AIConversationException(ErrorCode.PAGE_SIZE_INVALID);
        }

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new AIConversationException(ErrorCode.PAGEABLE_PAGE_SIZE_TOO_LARGE);
        }
    }
}
