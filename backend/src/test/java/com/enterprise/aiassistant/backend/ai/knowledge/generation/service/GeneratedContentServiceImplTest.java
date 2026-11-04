package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GeneratedHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratedContentServiceImplTest {

    @Mock
    private GeneratedContentRepository generatedContentRepository;

    @Mock
    private GeneratedMapper generatedMapper;

    @Mock
    private GeneratedHelper generatedHelper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private GeneratedContentServiceImpl service;

    private final GeneratedContent content = GeneratedContent.builder()
            .id(5L)
            .title("Tiêu đề cũ")
            .content("Nội dung cũ")
            .build();

    // ===================== getGeneratedContents =====================

    @Test
    void getGeneratedContents_hasPermission_returnsOwnContentsMapped() {
        Pageable pageable = PageRequest.of(0, 10);
        GeneratedContentResponse response = GeneratedContentResponse.builder().id(5L).build();

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(generatedContentRepository.findOwnedByUser(1L, GeneratedDocumentType.EMAIL, pageable))
                .thenReturn(new SliceImpl<>(List.of(content)));
        when(generatedMapper.toGeneratedContentResponse(content)).thenReturn(response);

        Slice<GeneratedContentResponse> result = service.getGeneratedContents(GeneratedDocumentType.EMAIL, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getGeneratedContents_noPermission_throwsPermissionDenied() {
        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.AI_DOCUMENT_GENERATION);

        assertThatThrownBy(() -> service.getGeneratedContents(GeneratedDocumentType.EMAIL, PageRequest.of(0, 10)))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERMISSION_DENIED);

        verifyNoInteractions(generatedContentRepository);
    }

    // ===================== getGeneratedContentById =====================

    @Test
    void getGeneratedContentById_owner_returnsMappedResponse() {
        GeneratedContentDetailResponse response = GeneratedContentDetailResponse.builder().id(5L).build();

        stubOwnedContent(1L, true);
        when(generatedMapper.toGeneratedContentDetailResponse(content)).thenReturn(response);

        assertThat(service.getGeneratedContentById(5L)).isSameAs(response);
    }

    @Test
    void getGeneratedContentById_notOwner_throwsAccessDenied() {
        stubOwnedContent(2L, false);

        assertThatThrownBy(() -> service.getGeneratedContentById(5L))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verifyNoInteractions(generatedMapper);
    }

    // TODO (bạn tự viết): getGeneratedContentById_notFound_throwsNotFound
    // Repository không tìm thấy id 5: findById(5L) trả Optional.empty()
    // → phải ném GeneratedException với ErrorCode.GENERATED_CONTENT_NOT_FOUND,
    //   và mapper không được gọi (verifyNoInteractions(generatedMapper)).
    // Lưu ý: chỉ stub findById. Không stub getCurrentUserId/isOwnedByUser, vì code ném lỗi
    // trước khi tới đó; stub thừa sẽ làm Mockito báo UnnecessaryStubbingException.
    @Test
    void getGeneratedContentById_notFound_throwsNotFound() {
        when(generatedContentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->service.getGeneratedContentById(5L))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_NOT_FOUND);

        verifyNoInteractions(generatedMapper);
    }

    // ===================== updateGeneratedContent =====================

    @Test
    void updateGeneratedContent_owner_trimsAndUpdatesFields() {
        UpdateGeneratedContentRequest request = new UpdateGeneratedContentRequest();
        request.setTitle("  Tiêu đề mới  ");
        request.setContent("  Nội dung mới  ");

        stubOwnedContent(1L, true);

        service.updateGeneratedContent(5L, request);

        // Service không gọi save(): entity đang được JPA quản lý trong @Transactional,
        // nên chỉ cần set field là đủ. Vì vậy test kiểm tra chính object content.
        assertThat(content.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(content.getContent()).isEqualTo("Nội dung mới");
    }

    @Test
    void updateGeneratedContent_invalidRequest_doesNotLoadContent() {
        UpdateGeneratedContentRequest request = new UpdateGeneratedContentRequest();

        doThrow(new GeneratedException(ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED))
                .when(generatedHelper).validateUpdateRequest(request);

        assertThatThrownBy(() -> service.updateGeneratedContent(5L, request))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED);

        verifyNoInteractions(generatedContentRepository, currentUserService);
    }

    // ===================== test data =====================

    private void stubOwnedContent(Long currentUserId, boolean owned) {
        when(generatedContentRepository.findById(5L)).thenReturn(Optional.of(content));
        when(currentUserService.getCurrentUserId()).thenReturn(currentUserId);
        when(generatedContentRepository.isOwnedByUser(5L, currentUserId)).thenReturn(owned);
    }
}
