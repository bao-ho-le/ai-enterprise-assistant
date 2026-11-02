package com.enterprise.aiassistant.backend.ai.knowledge.generation.helper;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationHelperTest {

    private final GenerationHelper helper = new GenerationHelper();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== validateTriggerRequest =====================

    @Test
    void validateTriggerRequest_nullRequest_throwsInputDataRequired() {
        assertThatThrownBy(() -> helper.validateTriggerRequest(null))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_INPUT_DATA_REQUIRED);
    }

    @Test
    void validateTriggerRequest_missingInputData_throwsInputDataRequired() {
        TriggerGenerationRequest request = new TriggerGenerationRequest();

        assertThatThrownBy(() -> helper.validateTriggerRequest(request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_INPUT_DATA_REQUIRED);
    }

    @Test
    void validateTriggerRequest_valid_doesNotThrow() {
        TriggerGenerationRequest request = new TriggerGenerationRequest();
        request.setInputData(Map.of("recipient", "Phòng nhân sự"));

        assertThatCode(() -> helper.validateTriggerRequest(request)).doesNotThrowAnyException();
    }

    // ===================== validateGenerationId =====================

    @Test
    void validateGenerationId_null_throwsGenerationIdRequired() {
        assertThatThrownBy(() -> helper.validateGenerationId(null))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_ID_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -5L})
    void validateGenerationId_notPositive_throwsGenerationIdInvalid(Long generationId) {
        assertThatThrownBy(() -> helper.validateGenerationId(generationId))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_ID_INVALID);
    }

    @Test
    void validateGenerationId_positive_doesNotThrow() {
        assertThatCode(() -> helper.validateGenerationId(1L)).doesNotThrowAnyException();
    }

    // ===================== parseInput =====================

    @Test
    void parseInput_validJson_mapsToObject() throws Exception {
        JsonNode inputData = objectMapper.readTree("""
                {"recipient": "Phòng nhân sự", "purpose": "Xin nghỉ phép", "tone": "FORMAL"}
                """);

        EmailGenerationInput input = helper.parseInput(inputData, EmailGenerationInput.class);

        assertThat(input.getRecipient()).isEqualTo("Phòng nhân sự");
        assertThat(input.getPurpose()).isEqualTo("Xin nghỉ phép");
        assertThat(input.getTone()).isEqualTo("FORMAL");
    }

    @Test
    void parseInput_unknownField_ignoresIt() throws Exception {
        JsonNode inputData = objectMapper.readTree("""
                {"recipient": "Phòng nhân sự", "fieldKhongTonTai": 123}
                """);

        EmailGenerationInput input = helper.parseInput(inputData, EmailGenerationInput.class);

        assertThat(input.getRecipient()).isEqualTo("Phòng nhân sự");
    }

    @Test
    void parseInput_wrongFieldType_throwsInputDataInvalid() throws Exception {
        // recipient là String nhưng JSON gửi object → Jackson không đổi được
        JsonNode inputData = objectMapper.readTree("""
                {"recipient": {"name": "Phòng nhân sự"}}
                """);

        assertThatThrownBy(() -> helper.parseInput(inputData, EmailGenerationInput.class))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_INPUT_DATA_INVALID);
    }

    // ===================== validateSourceDocumentsRequired =====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateSourceDocumentsRequired_blank_throwsSourceDocumentsRequired(String documentContext) {
        assertThatThrownBy(() -> helper.validateSourceDocumentsRequired(documentContext))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);
    }

    @Test
    void validateSourceDocumentsRequired_hasContent_doesNotThrow() {
        assertThatCode(() -> helper.validateSourceDocumentsRequired("Nội dung tài liệu"))
                .doesNotThrowAnyException();
    }

    // ===================== truncateTitle =====================

    @Test
    void truncateTitle_shorterThanMax_onlyTrims() {
        assertThat(helper.truncateTitle("  Báo cáo quý 3  ", 50)).isEqualTo("Báo cáo quý 3");
    }

    // TODO (bạn tự viết): truncateTitle_longerThanMax_trimsThenCuts
    // Ví dụ truncateTitle("  Báo cáo doanh thu  ", 7) phải trả "Báo cáo":
    // khoảng trắng đầu/cuối bị bỏ trước, sau đó mới cắt còn đúng 7 ký tự.
    // Đây là test kiểm tra giá trị trả về (assertThat(...).isEqualTo(...)), không phải exception.
    @Test
    void truncateTitle_longerThanMax_trimsThenCuts(){
        String title = "   " +  "a".repeat(55) + "   ";

        assertThat(helper.truncateTitle(title, 50)).isEqualTo("a".repeat(50));
    }
}
