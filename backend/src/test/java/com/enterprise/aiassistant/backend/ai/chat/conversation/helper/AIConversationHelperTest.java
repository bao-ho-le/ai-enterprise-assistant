package com.enterprise.aiassistant.backend.ai.chat.conversation.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.StartGenerationConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIConversationHelperTest {

    private final AIConversationHelper helper = new AIConversationHelper();

    // ===================== filterNewVersions =====================

    @Test
    void filterNewVersions_skipsAlreadyAttachedVersions() {
        DocumentVersion v1 = version(1L, DocumentStatus.ACTIVE);
        DocumentVersion v2 = version(2L, DocumentStatus.ACTIVE);
        DocumentVersion v3 = version(3L, DocumentStatus.ACTIVE);

        List<DocumentVersion> result = helper.filterNewVersions(List.of(v1, v2, v3), List.of(2L));

        assertThat(result).containsExactly(v1, v3);
    }

    // ===================== validateCreateConversationRequest =====================

    @Test
    void validateCreateConversationRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateCreateConversationRequest(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateCreateConversationRequest_missingConversationType_throwsRequestRequired() {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setTitle("Hỏi đáp hợp đồng");

        assertThatThrownBy(() -> helper.validateCreateConversationRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateCreateConversationRequest_blankTitle_setsDefaultTitle(String title) {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setConversationType(ConversationType.EMAIL_GENERATION);
        request.setTitle(title);

        helper.validateCreateConversationRequest(request);

        // Dạng "Email Generation - dd/MM/yyyy HH:mm"
        assertThat(request.getTitle()).matches("Email Generation - \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}");
    }

    @Test
    void validateCreateConversationRequest_hasTitle_keepsTitle() {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setConversationType(ConversationType.DOCUMENT_QA);
        request.setTitle("Hỏi đáp hợp đồng");

        helper.validateCreateConversationRequest(request);

        assertThat(request.getTitle()).isEqualTo("Hỏi đáp hợp đồng");
    }

    // ===================== validateRenameRequest =====================

    @Test
    void validateRenameRequest_invalidConversationId_throwsConversationIdInvalid() {
        RenameConversationRequest request = new RenameConversationRequest();
        request.setTitle("Tên mới");

        assertThatThrownBy(() -> helper.validateRenameRequest(0L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_INVALID);
    }

    @Test
    void validateRenameRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateRenameRequest(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateRenameRequest_blankTitle_throwsRequestRequired(String title) {
        RenameConversationRequest request = new RenameConversationRequest();
        request.setTitle(title);

        assertThatThrownBy(() -> helper.validateRenameRequest(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateRenameRequest_valid_doesNotThrow() {
        RenameConversationRequest request = new RenameConversationRequest();
        request.setTitle("Tên mới");

        assertThatCode(() -> helper.validateRenameRequest(1L, request)).doesNotThrowAnyException();
    }

    // ===================== validateAttachRequest =====================

    @Test
    void validateAttachRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateAttachRequest(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void validateAttachRequest_emptyVersionIds_throwsRequestRequired(List<Long> documentVersionIds) {
        AttachDocumentsRequest request = new AttachDocumentsRequest();
        request.setDocumentVersionIds(documentVersionIds);

        assertThatThrownBy(() -> helper.validateAttachRequest(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateAttachRequest_valid_doesNotThrow() {
        AttachDocumentsRequest request = new AttachDocumentsRequest();
        request.setDocumentVersionIds(List.of(10L, 11L));

        assertThatCode(() -> helper.validateAttachRequest(1L, request)).doesNotThrowAnyException();
    }

    // ===================== validateVersionsNotDeleted =====================

    @Test
    void validateVersionsNotDeleted_hasDeletedDocument_throwsDocumentDeleted() {
        List<DocumentVersion> versions = List.of(
                version(1L, DocumentStatus.ACTIVE),
                version(2L, DocumentStatus.DELETED)
        );

        assertThatThrownBy(() -> helper.validateVersionsNotDeleted(versions))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_DELETED);
    }

    @Test
    void validateVersionsNotDeleted_allActive_doesNotThrow() {
        List<DocumentVersion> versions = List.of(version(1L, DocumentStatus.ACTIVE));

        assertThatCode(() -> helper.validateVersionsNotDeleted(versions)).doesNotThrowAnyException();
    }

    // ===================== validateAttachedDocumentsNotDeleted =====================

    @Test
    void validateAttachedDocumentsNotDeleted_hasDeletedDocument_throwsAttachedDocumentDeleted() {
        List<AIConversationDocument> attached = List.of(
                attached(version(1L, DocumentStatus.ACTIVE)),
                attached(version(2L, DocumentStatus.DELETED))
        );

        assertThatThrownBy(() -> helper.validateAttachedDocumentsNotDeleted(attached))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ATTACHED_DOCUMENT_DELETED);
    }

    @Test
    void validateAttachedDocumentsNotDeleted_allActive_doesNotThrow() {
        List<AIConversationDocument> attached = List.of(attached(version(1L, DocumentStatus.ACTIVE)));

        assertThatCode(() -> helper.validateAttachedDocumentsNotDeleted(attached)).doesNotThrowAnyException();
    }

    // ===================== validateConversationId =====================

    @Test
    void validateConversationId_null_throwsConversationIdRequired() {
        assertThatThrownBy(() -> helper.validateConversationId(null))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void validateConversationId_notPositive_throwsConversationIdInvalid(Long conversationId) {
        assertThatThrownBy(() -> helper.validateConversationId(conversationId))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_ID_INVALID);
    }

    @Test
    void validateConversationId_positive_doesNotThrow() {
        assertThatCode(() -> helper.validateConversationId(1L)).doesNotThrowAnyException();
    }

    // ===================== validateRecentMessagesLimit =====================

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void validateRecentMessagesLimit_outside1To100_throwsLimitInvalid(int limit) {
        assertThatThrownBy(() -> helper.validateRecentMessagesLimit(limit))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECENT_MESSAGES_LIMIT_INVALID);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void validateRecentMessagesLimit_atBoundary_doesNotThrow(int limit) {
        assertThatCode(() -> helper.validateRecentMessagesLimit(limit)).doesNotThrowAnyException();
    }

    // ===================== validateStartDocumentQaRequest =====================

    @Test
    void validateStartDocumentQaRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateStartDocumentQaRequest(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    // ===================== validateStartGenerationRequest =====================

    @Test
    void validateStartGenerationRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateStartGenerationRequest(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateStartGenerationRequest_missingConversationType_throwsRequestRequired() {
        StartGenerationConversationRequest request = new StartGenerationConversationRequest();
        request.setInputData(Map.of("recipient", "Phòng nhân sự"));

        assertThatThrownBy(() -> helper.validateStartGenerationRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateStartGenerationRequest_missingInputData_throwsRequestRequired() {
        StartGenerationConversationRequest request = new StartGenerationConversationRequest();
        request.setConversationType(ConversationType.EMAIL_GENERATION);

        assertThatThrownBy(() -> helper.validateStartGenerationRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_REQUIRED);
    }

    @Test
    void validateStartGenerationRequest_valid_doesNotThrow() {
        StartGenerationConversationRequest request = new StartGenerationConversationRequest();
        request.setConversationType(ConversationType.EMAIL_GENERATION);
        request.setInputData(Map.of("recipient", "Phòng nhân sự"));

        assertThatCode(() -> helper.validateStartGenerationRequest(request)).doesNotThrowAnyException();
    }

    // ===================== validateGenerationConversationType =====================

    @ParameterizedTest
    @EnumSource(value = ConversationType.class, names = {
            "EMAIL_GENERATION", "REPORT_GENERATION", "SUMMARY_GENERATION",
            "MEETING_MINUTES_GENERATION", "FORM_GENERATION"
    })
    void validateGenerationConversationType_generationType_doesNotThrow(ConversationType type) {
        assertThatCode(() -> helper.validateGenerationConversationType(type)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = ConversationType.class, names = {
            "EMAIL_GENERATION", "REPORT_GENERATION", "SUMMARY_GENERATION",
            "MEETING_MINUTES_GENERATION", "FORM_GENERATION"
    }, mode = EnumSource.Mode.EXCLUDE)
    void validateGenerationConversationType_otherType_throwsNotGeneration(ConversationType type) {
        assertThatThrownBy(() -> helper.validateGenerationConversationType(type))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_TYPE_NOT_GENERATION);
    }

    // ===================== validateOwnership =====================

    @Test
    void validateOwnership_owner_doesNotThrow() {
        AIConversation conversation = AIConversation.builder()
                .user(User.builder().id(1L).build())
                .build();

        assertThatCode(() -> helper.validateOwnership(conversation, 1L)).doesNotThrowAnyException();
    }

    @Test
    void validateOwnership_conversationWithoutUser_throwsAccessDenied() {
        AIConversation conversation = AIConversation.builder().build();

        assertThatThrownBy(() -> helper.validateOwnership(conversation, 1L))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // TODO (bạn tự viết): validateOwnership_differentUser_throwsAccessDenied
    // Conversation thuộc user id 1, gọi validateOwnership với currentUserId = 2
    // → phải ném AuthorizationException với ErrorCode.ACCESS_DENIED.
    @Test
    void validateOwnership_differentUser_throwsAccessDenied(){
        AIConversation conversation = AIConversation.builder()
                .user(User.builder().id(1L).build())
                .build();

        assertThatThrownBy(() -> helper.validateOwnership(conversation, 2L))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    // ===================== test data =====================

    private DocumentVersion version(Long id, DocumentStatus documentStatus) {
        Document document = Document.builder().status(documentStatus).build();
        return DocumentVersion.builder().id(id).document(document).build();
    }

    private AIConversationDocument attached(DocumentVersion version) {
        return AIConversationDocument.builder().documentVersion(version).build();
    }
}
