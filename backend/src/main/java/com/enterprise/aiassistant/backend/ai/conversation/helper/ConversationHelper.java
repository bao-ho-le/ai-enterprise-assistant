package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationHelper {

    private static final int MAX_PAGE_SIZE = 50;

    public List<DocumentVersion> filterNewVersions(
            List<DocumentVersion> versions,
            List<Long> alreadyAttachedVersionIds
    ) {
        return versions.stream()
                .filter(version -> !alreadyAttachedVersionIds.contains(version.getId()))
                .toList();
    }

    public void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new ConversationException(ErrorCode.PAGEABLE_REQUIRED);
        }

        if (pageable.getPageNumber() < 0) {
            throw new ConversationException(ErrorCode.PAGE_NUMBER_INVALID);
        }

        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new ConversationException(ErrorCode.PAGE_SIZE_INVALID);
        }
    }
}
