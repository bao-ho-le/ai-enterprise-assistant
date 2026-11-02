package com.enterprise.aiassistant.backend.ai.knowledge.generation.helper;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedHelperTest {

    private final GeneratedHelper helper = new GeneratedHelper();

    // ===================== validateGeneratedContentId =====================

    @Test
    void validateGeneratedContentId_null_throwsIdRequired() {
        assertThatThrownBy(() -> helper.validateGeneratedContentId(null))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_ID_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void validateGeneratedContentId_notPositive_throwsIdInvalid(Long generatedContentId) {
        assertThatThrownBy(() -> helper.validateGeneratedContentId(generatedContentId))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_ID_INVALID);
    }

    @Test
    void validateGeneratedContentId_positive_doesNotThrow() {
        assertThatCode(() -> helper.validateGeneratedContentId(1L)).doesNotThrowAnyException();
    }

    // ===================== validateUpdateRequest =====================

    @Test
    void validateUpdateRequest_nullRequest_throwsUpdateRequestRequired() {
        assertThatThrownBy(() -> helper.validateUpdateRequest(null))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED);
    }

    @Test
    void validateUpdateRequest_blankTitle_throwsTitleRequired() {
        UpdateGeneratedContentRequest request = new UpdateGeneratedContentRequest();
        request.setTitle(" ");
        request.setContent("Nội dung email");

        assertThatThrownBy(() -> helper.validateUpdateRequest(request))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED);
    }

    // TODO (bạn tự viết): validateUpdateRequest_blankContent_throwsBodyRequired
    // Request có title hợp lệ (vd "Email xin nghỉ") nhưng content rỗng ("" hoặc null)
    // → phải ném GeneratedException với ErrorCode.GENERATED_CONTENT_BODY_REQUIRED.
    // Lưu ý: title phải hợp lệ, nếu không thì lỗi title sẽ ném ra trước.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateUpdateRequest_blankContent_throwsBodyRequired(String content){
        UpdateGeneratedContentRequest request = new UpdateGeneratedContentRequest();

        request.setTitle("Email xin nghỉ");
        request.setContent(content);

        assertThatThrownBy(() -> helper.validateUpdateRequest(request))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_BODY_REQUIRED);
    }

    @Test
    void validateUpdateRequest_valid_doesNotThrow() {
        UpdateGeneratedContentRequest request = new UpdateGeneratedContentRequest();
        request.setTitle("Email xin nghỉ");
        request.setContent("Nội dung email");

        assertThatCode(() -> helper.validateUpdateRequest(request)).doesNotThrowAnyException();
    }

    // ===================== validateTitle =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateTitle_blank_throwsTitleRequired(String title) {
        assertThatThrownBy(() -> helper.validateTitle(title))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED);
    }

    @Test
    void validateTitle_over500Chars_throwsTitleTooLong() {
        String title = "a".repeat(501);

        assertThatThrownBy(() -> helper.validateTitle(title))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_TITLE_TOO_LONG);
    }

    @Test
    void validateTitle_500CharsWithSurroundingSpaces_doesNotThrow() {
        // Độ dài được tính sau khi trim, nên khoảng trắng hai đầu không bị tính
        String title = "  " + "a".repeat(500) + "  ";

        assertThatCode(() -> helper.validateTitle(title)).doesNotThrowAnyException();
    }

    // ===================== validateContent =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateContent_blank_throwsBodyRequired(String content) {
        assertThatThrownBy(() -> helper.validateContent(content))
                .isInstanceOf(GeneratedException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATED_CONTENT_BODY_REQUIRED);
    }

    @Test
    void validateContent_hasContent_doesNotThrow() {
        assertThatCode(() -> helper.validateContent("Nội dung email")).doesNotThrowAnyException();
    }
}
