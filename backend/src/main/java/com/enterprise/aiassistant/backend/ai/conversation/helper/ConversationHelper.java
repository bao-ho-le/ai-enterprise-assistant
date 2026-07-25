package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationHelper {

    private final AIConversationRepository conversationRepository;

    private final DocumentVersionRepository documentVersionRepository;

    public AIConversation getConversationOrThrow(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    public List<DocumentVersion> getDocumentVersionsOrThrow(List<Long> documentVersionIds) {
        List<DocumentVersion> versions = documentVersionRepository.findAllById(documentVersionIds);

        if (versions.size() != documentVersionIds.size()) {
            throw new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }

        return versions;
    }

    public List<DocumentVersion> filterNewVersions(
            List<DocumentVersion> versions,
            List<Long> alreadyAttachedVersionIds
    ) {
        return versions.stream()
                .filter(version -> !alreadyAttachedVersionIds.contains(version.getId()))
                .toList();
    }
}
