package com.enterprise.aiassistant.backend.ai.generation.service;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Full extracted text of every document attached to a conversation, concatenated for
// prompt context. Unlike SemanticSearchService this isn't similarity search — Report/
// Summary generation needs the whole source, not just the top-K matching chunks.
@Service
@RequiredArgsConstructor
public class DocumentContextService {

    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final DocumentTextRepository documentTextRepository;

    @Transactional(readOnly = true)
    public String buildContext(Long conversationId) {

        List<AIConversationDocument> attached =
                conversationDocumentRepository.findByAiConversationIdWithDocument(conversationId);

        if (attached.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        for (AIConversationDocument link : attached) {
            DocumentVersion version = link.getDocumentVersion();

            context.append("Document: ")
                    .append(version.getDocument().getTitle())
                    .append(" (v").append(version.getVersionNumber()).append(")\n");

            documentTextRepository.findByDocumentVersionId(version.getId())
                    .ifPresentOrElse(
                            text -> context.append(text.getContent()),
                            () -> context.append("[no extracted text available]")
                    );

            context.append("\n\n");
        }

        return context.toString();
    }
}
