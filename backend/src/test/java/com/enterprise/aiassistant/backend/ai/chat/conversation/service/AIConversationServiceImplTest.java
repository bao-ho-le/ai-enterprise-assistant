package com.enterprise.aiassistant.backend.ai.chat.conversation.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.request.StartGenerationConversationRequest;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.DocumentQaConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.StartGenerationConversationResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.chat.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.service.AIMessageService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.service.GenerationService;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.document.service.DocumentAuthorizationService;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Service có 16 dependency nhưng mỗi test chỉ stub vài cái nó thật sự cần: các hàm validate*
// của helper là void, mock mặc định không làm gì, nên coi như "luôn hợp lệ" nếu không doThrow.
@ExtendWith(MockitoExtension.class)
class AIConversationServiceImplTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private DocumentAuthorizationService documentAuthorizationService;

    @Mock
    private AIConversationDocumentRepository conversationDocumentRepository;

    @Mock
    private AIMessageRepository messageRepository;

    @Mock
    private AIMessageSourceRepository messageSourceRepository;

    @Mock
    private ConversationMemoryRepository conversationMemoryRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private GeneratedContentRepository generatedContentRepository;

    @Mock
    private GenerationRepository generationRepository;

    @Mock
    private AIConversationMapper aiConversationMapper;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private GeneratedMapper generatedMapper;

    @Mock
    private AIMessageService aiMessageService;

    @Mock
    private GenerationService generationService;

    @InjectMocks
    private AIConversationServiceImpl service;

    private final AIConversation conversation = AIConversation.builder()
            .id(1L)
            .title("Hội thoại cũ")
            .conversationType(ConversationType.DOCUMENT_QA)
            .build();

    private final Document document1 = Document.builder().id(100L).build();

    private final Document document2 = Document.builder().id(200L).build();

    private final DocumentVersion version1 = DocumentVersion.builder().id(1L).document(document1).build();

    private final DocumentVersion version2 = DocumentVersion.builder().id(2L).document(document2).build();

    // ===================== createConversation =====================

    @Test
    void createConversation_documentQa_requiresCreateAndDocumentQaPermissions() {
        CreateConversationRequest request = createRequest(ConversationType.DOCUMENT_QA);
        User user = User.builder().id(1L).build();
        ConversationResponse response = ConversationResponse.builder().id(1L).build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(aiConversationMapper.toEntity(request, user)).thenReturn(conversation);
        when(aiConversationMapper.toResponse(conversation)).thenReturn(response);

        assertThat(service.createConversation(request)).isSameAs(response);

        verify(currentUserService).requirePermission(Permission.CONVERSATION_CREATE);
        verify(currentUserService).requirePermission(Permission.AI_DOCUMENT_QA);
        verify(conversationRepository).save(conversation);
    }

    // Không stub mapper: mock trả null nên service vẫn chạy hết. Test này chỉ quan tâm quyền nào bị kiểm tra.
    @ParameterizedTest
    @EnumSource(value = ConversationType.class, names = {
            "EMAIL_GENERATION", "REPORT_GENERATION", "MEETING_MINUTES_GENERATION", "SUMMARY_GENERATION", "FORM_GENERATION"
    })
    void createConversation_generationType_requiresGenerationPermission(ConversationType type) {
        service.createConversation(createRequest(type));

        verify(currentUserService).requirePermission(Permission.AI_DOCUMENT_GENERATION);
        verify(currentUserService, never()).requirePermission(Permission.AI_DOCUMENT_QA);
    }

    @Test
    void createConversation_noFeaturePermission_doesNotSave() {
        CreateConversationRequest request = createRequest(ConversationType.DOCUMENT_QA);

        // Service gọi requirePermission 2 lần với 2 quyền khác nhau. Nếu chỉ stub quyền AI_DOCUMENT_QA,
        // lần gọi đầu (CONVERSATION_CREATE) khác đối số với stub, Mockito báo PotentialStubbingProblem.
        // Nên stub rõ cả hai: có quyền tạo conversation, nhưng không có quyền Document QA.
        doNothing().when(currentUserService).requirePermission(Permission.CONVERSATION_CREATE);
        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.AI_DOCUMENT_QA);

        assertThatThrownBy(() -> service.createConversation(request))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERMISSION_DENIED);

        verifyNoInteractions(conversationRepository);
    }

    // ===================== startGenerationConversation =====================

    @Test
    void startGenerationConversation_noDocuments_generatesWithoutAttaching() {
        StartGenerationConversationRequest request = new StartGenerationConversationRequest();
        request.setConversationType(ConversationType.EMAIL_GENERATION);
        request.setInputData(Map.of("purpose", "Xin nghỉ phép"));

        ConversationResponse created = ConversationResponse.builder().id(1L).build();
        TriggerGenerationResponse generation = TriggerGenerationResponse.builder().build();
        StartGenerationConversationResponse response = StartGenerationConversationResponse.builder().build();

        // CreateConversationRequest do service tự tạo bên trong, test không có object đó nên dùng any()
        when(aiConversationMapper.toEntity(any(), any())).thenReturn(conversation);
        when(aiConversationMapper.toResponse(conversation)).thenReturn(created);
        // Đã dùng matcher (any) cho một đối số thì đối số còn lại cũng phải là matcher: eq(1L) thay vì 1L
        when(generationService.generate(eq(1L), any())).thenReturn(generation);
        when(aiConversationMapper.toStartGenerationConversationResponse(created, generation)).thenReturn(response);

        assertThat(service.startGenerationConversation(request)).isSameAs(response);

        // Không gửi documentVersionIds thì không được đi vào bước attach
        verifyNoInteractions(documentVersionRepository);

        ArgumentCaptor<TriggerGenerationRequest> captor = ArgumentCaptor.forClass(TriggerGenerationRequest.class);
        verify(generationService).generate(eq(1L), captor.capture());
        assertThat(captor.getValue().getInputData()).isEqualTo(Map.of("purpose", "Xin nghỉ phép"));
    }

    // ===================== renameConversation =====================

    @Test
    void renameConversation_owner_updatesTitleAndSaves() {
        ConversationResponse response = ConversationResponse.builder().id(1L).title("Tên mới").build();

        stubActiveConversation();
        when(aiConversationMapper.toResponse(conversation)).thenReturn(response);

        assertThat(service.renameConversation(1L, renameRequest("Tên mới"))).isSameAs(response);

        assertThat(conversation.getTitle()).isEqualTo("Tên mới");
        verify(conversationRepository).save(conversation);
    }

    @Test
    void renameConversation_notOwner_throwsAccessDenied() {
        stubActiveConversation();
        stubNotOwner();

        assertThatThrownBy(() -> service.renameConversation(1L, renameRequest("Tên mới")))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        assertThat(conversation.getTitle()).isEqualTo("Hội thoại cũ");
        verify(conversationRepository, never()).save(any());
    }

    // Conversation đã bị xóa mềm thì không tìm thấy với status ACTIVE
    @Test
    void renameConversation_notActive_throwsConversationNotFound() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renameConversation(1L, renameRequest("Tên mới")))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_NOT_FOUND);
    }

    // ===================== softDeleteConversation / restoreConversation =====================

    @Test
    void softDeleteConversation_owner_marksDeletedAndKeepsMessages() {
        stubActiveConversation();

        service.softDeleteConversation(1L);

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.DELETED);
        assertThat(conversation.getDeletedAt()).isNotNull();
        verify(conversationRepository).save(conversation);
        // Xóa mềm chỉ đổi trạng thái, không được xóa dữ liệu thật
        verifyNoInteractions(messageRepository);
    }

    @Test
    void restoreConversation_deletedConversation_reactivatesSameRow() {
        conversation.setStatus(ConversationStatus.DELETED);
        conversation.setDeletedAt(LocalDateTime.now());
        ConversationResponse response = ConversationResponse.builder().id(1L).build();

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(aiConversationMapper.toResponse(conversation)).thenReturn(response);

        assertThat(service.restoreConversation(1L)).isSameAs(response);

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getDeletedAt()).isNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    void restoreConversation_activeConversation_throwsNotDeleted() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.restoreConversation(1L))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_NOT_DELETED);

        verify(conversationRepository, never()).save(any());
    }

    // ===================== hardDeleteConversation =====================

    @Test
    void hardDeleteConversation_owner_deletesChildRowsBeforeConversation() {
        GeneratedContent content = GeneratedContent.builder().id(20L).build();
        Generation withContent = Generation.builder().id(10L).generatedContent(content).build();
        Generation withoutContent = Generation.builder().id(11L).build();

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(generationRepository.findByAiConversationId(1L)).thenReturn(List.of(withContent, withoutContent));

        service.hardDeleteConversation(1L);

        // Bảng con phải xóa trước bảng cha, nếu không DB báo lỗi khóa ngoại.
        // InOrder chỉ so thứ tự giữa các lời gọi được liệt kê, các lời gọi khác xen giữa không sao.
        InOrder inOrder = inOrder(
                messageSourceRepository, messageRepository, generationRepository,
                generatedContentRepository, conversationRepository, conversationMemoryRepository, conversationDocumentRepository
        );
        inOrder.verify(messageSourceRepository).deleteByAiMessage_ConversationId(1L);
        inOrder.verify(messageRepository).deleteByConversationId(1L);

        inOrder.verify(conversationMemoryRepository).deleteByConversationId(1L);
        inOrder.verify(conversationDocumentRepository).deleteByConversationId(1L);

        inOrder.verify(generationRepository).deleteByAiConversationId(1L);
        // Generation không có content (id 11) bị bỏ qua, chỉ xóa content id 20
        inOrder.verify(generatedContentRepository).deleteAllById(List.of(20L));

        inOrder.verify(conversationRepository).delete(conversation);
    }

    // TODO (bạn tự viết): hardDeleteConversation_noGeneratedContent_skipsDeleteAllById
    // Conversation chưa có generation nào → không được gọi generatedContentRepository.deleteAllById,
    // nhưng conversation vẫn phải bị xóa (verify(conversationRepository).delete(conversation)).
    // Gợi ý: chỉ cần stub findById(1L). Không cần stub generationRepository.findByAiConversationId:
    // hàm trả List nên mock mặc định đã trả list rỗng. Kiểm tra bằng verify(..., never()).deleteAllById(any()).
    @Test
    void hardDeleteConversation_noGeneratedContent_skipsDeleteAllById() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        service.hardDeleteConversation(1L);
        verify(conversationRepository).delete(conversation);
        verify(generatedContentRepository, never()).deleteAllById(any());
    }

    @Test
    void hardDeleteConversation_notOwner_deletesNothing() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        stubNotOwner();

        assertThatThrownBy(() -> service.hardDeleteConversation(1L))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verifyNoInteractions(
                messageSourceRepository, messageRepository, conversationMemoryRepository,
                conversationDocumentRepository, generationRepository, generatedContentRepository
        );
        verify(conversationRepository, never()).delete(any());
    }

    // ===================== attachDocuments =====================

    @Test
    void attachDocuments_someAlreadyAttached_savesOnlyNewLinks() {
        AIConversationDocument newLink = AIConversationDocument.builder().id(50L).build();

        stubActiveConversation();
        // Request gửi trùng id 1; stub chỉ khớp với [1, 2] nên chứng minh service đã bỏ id trùng trước khi query
        when(documentVersionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(version1, version2));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document1, document2)))
                .thenReturn(Set.of(100L, 200L));
        when(conversationDocumentRepository.findDocumentVersionIdsByConversationId(1L)).thenReturn(List.of(1L));
        when(aiConversationHelper.filterNewVersions(List.of(version1, version2), List.of(1L)))
                .thenReturn(List.of(version2));
        when(aiConversationMapper.toConversationDocument(conversation, version2)).thenReturn(newLink);

        service.attachDocuments(1L, attachRequest(1L, 1L, 2L));

        verify(conversationDocumentRepository).saveAll(List.of(newLink));
    }

    @Test
    void attachDocuments_missingVersion_throwsVersionNotFound() {
        stubActiveConversation();
        when(documentVersionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(version1));

        assertThatThrownBy(() -> service.attachDocuments(1L, attachRequest(1L, 2L)))
                .isInstanceOf(DocumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_VERSION_NOT_FOUND);

        verify(conversationDocumentRepository, never()).saveAll(any());
    }

    // TODO (bạn tự viết): attachDocuments_unreadableDocument_throwsAccessDenied
    // User gắn 2 version (1 và 2) nhưng chỉ được đọc tài liệu id 100, không được đọc tài liệu id 200
    // → ném AuthorizationException với ErrorCode.ACCESS_DENIED, và không được saveAll.
    // Đây là test bảo mật: AI không được đọc tài liệu mà user không có quyền đọc.
    // Gợi ý: stub như 3 dòng đầu của attachDocuments_someAlreadyAttached_savesOnlyNewLinks,
    // nhưng filterReadableDocumentIds trả Set.of(100L). Không stub các bước sau đó vì code đã dừng.
    @Test
    void attachDocuments_unreadableDocument_throwsAccessDenied(){
        stubActiveConversation();
        when(documentVersionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(version1, version2));
        when(documentAuthorizationService.filterReadableDocumentIds(List.of(document1, document2)))
                .thenReturn(Set.of(100L));

        assertThatThrownBy(() -> service.attachDocuments(1L, attachRequest(1L, 2L)))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verify(conversationDocumentRepository, never()).saveAll(any());
    }

    // ===================== removeDocument =====================

    @Test
    void removeDocument_attached_deletesLink() {
        AIConversationDocument link = AIConversationDocument.builder().id(50L).build();

        stubActiveConversation();
        when(conversationDocumentRepository.findByConversationIdAndDocumentVersionId(1L, 2L))
                .thenReturn(Optional.of(link));

        service.removeDocument(1L, 2L);

        verify(conversationDocumentRepository).delete(link);
    }

    @Test
    void removeDocument_notAttached_throwsNotAttached() {
        stubActiveConversation();
        when(conversationDocumentRepository.findByConversationIdAndDocumentVersionId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeDocument(1L, 2L))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_ATTACHED_TO_CONVERSATION);

        verify(conversationDocumentRepository, never()).delete(any());
    }

    // ===================== getDocumentQaConversationDetail =====================

    @Test
    void getDocumentQaConversationDetail_owner_returnsDocumentsAndLatestMessages() {
        AIConversationDocument link = AIConversationDocument.builder().id(50L).build();
        ConversationDocumentResponse documentResponse = ConversationDocumentResponse.builder().build();
        List<AIMessageResponse> messages = List.of(AIMessageResponse.builder().build());
        DocumentQaConversationDetailResponse response = DocumentQaConversationDetailResponse.builder().build();

        stubActiveConversation();
        when(conversationDocumentRepository.findByAiConversationIdWithDocument(1L)).thenReturn(List.of(link));
        when(aiConversationMapper.toConversationDocumentResponse(link)).thenReturn(documentResponse);
        when(conversationDocumentRepository.existsByConversationIdAndDocumentVersionDocumentStatus(1L, DocumentStatus.DELETED))
                .thenReturn(true);
        // beforeId = null nghĩa là lấy các tin nhắn mới nhất
        when(aiMessageService.getMessages(1L, null, 20))
                .thenReturn(MessagePageResponse.builder().content(messages).hasMore(true).build());
        when(aiConversationMapper.toDocumentQaDetailResponse(conversation, List.of(documentResponse), messages, true, true))
                .thenReturn(response);

        assertThat(service.getDocumentQaConversationDetail(1L, 20)).isSameAs(response);
    }

    // ===================== getConversations =====================

    @Test
    void getConversations_givenStatus_filtersByThatStatus() {
        ConversationFilterRequest filter = new ConversationFilterRequest();
        filter.setConversationType(ConversationType.DOCUMENT_QA);
        filter.setStatus(ConversationStatus.DELETED);
        Pageable pageable = PageRequest.of(0, 10);
        Slice<ConversationResponse> slice = new SliceImpl<>(List.of());

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(conversationRepository.filterConversations(ConversationType.DOCUMENT_QA, ConversationStatus.DELETED, 1L, pageable))
                .thenReturn(slice);

        assertThat(service.getConversations(filter, pageable)).isSameAs(slice);
    }

    // TODO (bạn tự viết): getConversations_noStatus_defaultsToActive
    // Filter rỗng (new ConversationFilterRequest(), không set gì) → service phải tự lọc theo ACTIVE.
    // Gợi ý: làm giống test ngay phía trên, nhưng stub
    //   conversationRepository.filterConversations(null, ConversationStatus.ACTIVE, 1L, pageable).
    // Stub chỉ khớp khi service truyền đúng ACTIVE; truyền sai thì mock trả null và assert isSameAs fail.
    @Test
    void getConversations_noStatus_defaultsToActive() {
        ConversationFilterRequest filter = new ConversationFilterRequest();
        filter.setConversationType(ConversationType.DOCUMENT_QA);

        Pageable pageable = PageRequest.of(0, 10);
        Slice<ConversationResponse> slice = new SliceImpl<>(List.of());

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(conversationRepository.filterConversations(ConversationType.DOCUMENT_QA, ConversationStatus.ACTIVE, 1L, pageable))
                .thenReturn(slice);

        assertThat(service.getConversations(filter, pageable)).isSameAs(slice);
    }


    // ===================== getGenerationConversationDetail =====================

    @Test
    void getGenerationConversationDetail_emailGeneration_skipsAttachedDocuments() {
        conversation.setConversationType(ConversationType.EMAIL_GENERATION);
        Generation generation = Generation.builder().id(10L).build();
        GenerationConversationDetailResponse response = GenerationConversationDetailResponse.builder().build();

        stubActiveConversation();
        when(generationRepository.findFirstByAiConversationIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(generation));
        when(aiConversationMapper.toGenerationDetailResponse(conversation, generation, null, false)).thenReturn(response);

        assertThat(service.getGenerationConversationDetail(1L)).isSameAs(response);

        // Email không có tài liệu đính kèm nên không được query bảng tài liệu
        verifyNoInteractions(conversationDocumentRepository);
    }

    @Test
    void getGenerationConversationDetail_noGeneration_throwsGenerationNotFound() {
        conversation.setConversationType(ConversationType.REPORT_GENERATION);

        stubActiveConversation();
        when(generationRepository.findFirstByAiConversationIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGenerationConversationDetail(1L))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_NOT_FOUND);
    }

    // ===================== test data =====================

    private void stubActiveConversation() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.of(conversation));
    }

    // User đang đăng nhập là id 2, không phải chủ conversation
    private void stubNotOwner() {
        when(currentUserService.getCurrentUserId()).thenReturn(2L);
        doThrow(new AuthorizationException(ErrorCode.ACCESS_DENIED))
                .when(aiConversationHelper).validateOwnership(conversation, 2L);
    }

    private CreateConversationRequest createRequest(ConversationType type) {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setConversationType(type);
        return request;
    }

    private RenameConversationRequest renameRequest(String title) {
        RenameConversationRequest request = new RenameConversationRequest();
        request.setTitle(title);
        return request;
    }

    private AttachDocumentsRequest attachRequest(Long... documentVersionIds) {
        AttachDocumentsRequest request = new AttachDocumentsRequest();
        request.setDocumentVersionIds(List.of(documentVersionIds));
        return request;
    }
}
