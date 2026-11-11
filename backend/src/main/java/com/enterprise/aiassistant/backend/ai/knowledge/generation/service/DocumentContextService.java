package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

// Full extracted text of every document attached to a conversation, concatenated for
// prompt context. Unlike SemanticSearchService this isn't similarity search — Report/
// Summary generation needs the whole source, not just the top-K matching chunks.
@Service
@RequiredArgsConstructor
public class DocumentContextService {

    private final AIConversationDocumentRepository conversationDocumentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final AIConversationHelper aiConversationHelper;
    private final DocumentAuthorizationService documentAuthorizationService;

    @Transactional(readOnly = true)
    public String buildContext(Long conversationId) {

        // 1. Lấy tất cả tài liệu đã được đính kèm vào conversation
        List<AIConversationDocument> attachedDocuments =
                conversationDocumentRepository.findByAiConversationIdWithDocument(conversationId);

        if (attachedDocuments.isEmpty()) {
            return "";
        }

        // Không build context / không tốn token LLM nếu có tài liệu đã bị soft-delete
        aiConversationHelper.validateAttachedDocumentsNotDeleted(attachedDocuments);

        Set<Long> readableDocumentIds = documentAuthorizationService.filterReadableDocumentIds(
                attachedDocuments.stream()
                        .map(link -> link.getDocumentVersion().getDocument())
                        .filter(Objects::nonNull)
                        .toList()
        );

        // Dùng StringBuilder để ghép nội dung của nhiều tài liệu thành một context duy nhất
        StringBuilder context = new StringBuilder();

        // Duyệt qua từng tài liệu được đính kèm
        for (AIConversationDocument link : attachedDocuments) {
            DocumentVersion version = link.getDocumentVersion();

            if(version.getDocument() == null || !readableDocumentIds.contains(version.getDocument().getId()))
                continue;

            // Thêm metadata của tài liệu để AI biết nội dung thuộc tài liệu và phiên bản nào
            context.append("Document: ")
                    .append(version.getDocument().getTitle())
                    .append(" (v").append(version.getVersionNumber()).append(")\n");

            // Thêm toàn bộ nội dung đã được trích xuất từ tài liệu,
            // hoặc ghi chú nếu chưa có extracted text
            documentTextRepository.findByDocumentVersionId(version.getId())
                    .ifPresentOrElse(
                            text -> context.append(text.getContent()),
                            () -> context.append("[no extracted text available]")
                    );

            // Ngăn cách giữa các tài liệu để prompt rõ ràng hơn
            context.append("\n\n");
        }

        return context.toString();
    }
}
