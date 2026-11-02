package com.enterprise.aiassistant.backend.ai.analytics.usage.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// byFilter() trả Specification cho JPA Criteria, cần database thật nên để dành cho integration test.
class AIUsageHelperTest {

    private final AIUsageHelper helper = new AIUsageHelper();

    // ===================== fromDateFilter =====================

    @Test
    void fromDateFilter_setsOnlyFromDate() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);

        AIUsageLogFilterRequest filter = helper.fromDateFilter(from);

        assertThat(filter.getFromDate()).isEqualTo(from);
        assertThat(filter.getToDate()).isNull();
        assertThat(filter.getModel()).isNull();
    }

    // ===================== validateLogRequest =====================

    @Test
    void validateLogRequest_nullRequest_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateLogRequest(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_REQUEST_REQUIRED);
    }

    @Test
    void validateLogRequest_missingConversationType_throwsConversationTypeRequired() {
        AIUsageLogRequest request = validLogRequest().conversationType(null).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_CONVERSATION_TYPE_REQUIRED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateLogRequest_blankModel_throwsModelRequired(String model) {
        AIUsageLogRequest request = validLogRequest().model(model).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_MODEL_REQUIRED);
    }

    @Test
    void validateLogRequest_missingStatus_throwsStatusRequired() {
        AIUsageLogRequest request = validLogRequest().status(null).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_STATUS_REQUIRED);
    }

    @Test
    void validateLogRequest_negativeInputTokens_throwsInvalidTokenCount() {
        AIUsageLogRequest request = validLogRequest().inputTokens(-1).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_INVALID_TOKEN_COUNT);
    }

    @Test
    void validateLogRequest_negativeOutputTokens_throwsInvalidTokenCount() {
        AIUsageLogRequest request = validLogRequest().outputTokens(-1).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_INVALID_TOKEN_COUNT);
    }

    @Test
    void validateLogRequest_negativeCost_throwsInvalidEstimatedCost() {
        AIUsageLogRequest request = validLogRequest().estimatedCost(new BigDecimal("-0.01")).build();

        assertThatThrownBy(() -> helper.validateLogRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_INVALID_ESTIMATED_COST);
    }

    @Test
    void validateLogRequest_nullTokensAndCost_doesNotThrow() {
        AIUsageLogRequest request = validLogRequest().build();

        assertThatCode(() -> helper.validateLogRequest(request)).doesNotThrowAnyException();
    }

    @Test
    void validateLogRequest_zeroTokensAndCost_doesNotThrow() {
        AIUsageLogRequest request = validLogRequest()
                .inputTokens(0)
                .outputTokens(0)
                .estimatedCost(BigDecimal.ZERO)
                .build();

        assertThatCode(() -> helper.validateLogRequest(request)).doesNotThrowAnyException();
    }

    // ===================== normalizeToken / calculateTotalTokens =====================

    @Test
    void normalizeToken_null_returnsZero() {
        assertThat(AIUsageHelper.normalizeToken(null)).isZero();
    }

    @Test
    void normalizeToken_value_returnsSameValue() {
        assertThat(AIUsageHelper.normalizeToken(5)).isEqualTo(5);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "3,    4,    7",
            "NULL, 7,    7",
            "5,    NULL, 5",
            "NULL, NULL, 0"
    })
    void calculateTotalTokens_treatsNullAsZero(Integer inputTokens, Integer outputTokens, int expected) {
        assertThat(helper.calculateTotalTokens(inputTokens, outputTokens)).isEqualTo(expected);
    }

    // ===================== calculateSuccessRate =====================

    @Test
    void calculateSuccessRate_noRequests_returnsZero() {
        assertThat(helper.calculateSuccessRate(0, 0)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "1, 4, 25.0",
            "3, 3, 100.0",
            "0, 5, 0.0"
    })
    void calculateSuccessRate_validCounts_returnsPercentage(long successCount, long totalRequests, double expected) {
        assertThat(helper.calculateSuccessRate(successCount, totalRequests)).isEqualTo(expected);
    }

    @Test
    void calculateSuccessRate_emptyLogList_returnsZero() {
        assertThat(helper.calculateSuccessRate(List.of())).isZero();
    }

    // TODO (bạn tự viết): calculateSuccessRate_logList_returnsSuccessPercentage
    // Tạo 2 AIUsageLog bằng builder: một cái status SUCCESS, một cái status FAILED,
    // gọi helper.calculateSuccessRate(List.of(log1, log2)) → phải bằng 50.0.
    // Gợi ý: AIUsageLog.builder().status(AIUsageStatus.SUCCESS).build()
    @Test
    void calculateSuccessRate_logList_returnsSuccessPercentage(){
        AIUsageLog log1 = AIUsageLog.builder().status(AIUsageStatus.SUCCESS).build();
        AIUsageLog log2 = AIUsageLog.builder().status(AIUsageStatus.FAILED).build();

        assertThat(helper.calculateSuccessRate(List.of(log1, log2))).isEqualTo(50.0);
    }

    // ===================== validateFilter =====================

    @Test
    void validateFilter_nullFilter_throwsRequestRequired() {
        assertThatThrownBy(() -> helper.validateFilter(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_REQUEST_REQUIRED);
    }

    @Test
    void validateFilter_fromAfterTo_throwsInvalidDateRange() {
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        filter.setFromDate(LocalDateTime.of(2026, 10, 31, 0, 0));
        filter.setToDate(LocalDateTime.of(2026, 10, 1, 0, 0));

        assertThatThrownBy(() -> helper.validateFilter(filter))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void validateFilter_modelOver100Chars_throwsModelTooLong() {
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        filter.setModel("m".repeat(101));

        assertThatThrownBy(() -> helper.validateFilter(filter))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_MODEL_TOO_LONG);
    }

    @Test
    void validateFilter_fromEqualsTo_doesNotThrow() {
        LocalDateTime sameTime = LocalDateTime.of(2026, 10, 1, 0, 0);
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        filter.setFromDate(sameTime);
        filter.setToDate(sameTime);

        assertThatCode(() -> helper.validateFilter(filter)).doesNotThrowAnyException();
    }

    // ===================== validateDays =====================

    @ParameterizedTest
    @ValueSource(ints = {0, 91})
    void validateDays_outside1To90_throwsInvalidDays(int days) {
        assertThatThrownBy(() -> helper.validateDays(days))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_INVALID_DAYS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 90})
    void validateDays_atBoundary_doesNotThrow(int days) {
        assertThatCode(() -> helper.validateDays(days)).doesNotThrowAnyException();
    }

    // ===================== test data =====================

    // Request hợp lệ tối thiểu; từng test đổi đúng một field để kiểm tra một lỗi.
    private AIUsageLogRequest.AIUsageLogRequestBuilder validLogRequest() {
        return AIUsageLogRequest.builder()
                .conversationType(ConversationType.DOCUMENT_QA)
                .model("gemini-3.1-flash-lite")
                .status(AIUsageStatus.SUCCESS);
    }
}
