package com.enterprise.aiassistant.backend.ai.knowledge.search.helper;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.ai.knowledge.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SearchHelper}: request validation bounds and usage logging.
 */
class SearchHelperTest {

    private final AIUsageLogService aiUsageLogService = mock(AIUsageLogService.class);

    private SearchHelper helper;

    @BeforeEach
    void setUp() {
        helper = new SearchHelper(aiUsageLogService);
    }

    @Test
    void shouldRejectNullRequest() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(null)),
                ErrorCode.SEARCH_KEYWORD_REQUIRED
        );
    }

    @Test
    void shouldRejectNullAndBlankKeyword() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request(null, null, null))),
                ErrorCode.SEARCH_KEYWORD_REQUIRED
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request("   ", null, null))),
                ErrorCode.SEARCH_KEYWORD_REQUIRED
        );
    }

    @Test
    void shouldRejectKeywordLongerThan255Characters() {

        String tooLong = "a".repeat(256);

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request(tooLong, null, null))),
                ErrorCode.KEYWORD_TOO_LONG
        );
    }

    @Test
    void shouldAcceptKeywordOfExactly255Characters() {

        String maxLength = "a".repeat(255);

        assertThat(catchThrowable(() -> helper.validateSearchRequest(request(maxLength, null, null))))
                .isNull();
    }

    @Test
    void shouldRejectTopKOutOfRange() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request("q", 0, null))),
                ErrorCode.INVALID_TOP_K
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request("q", 51, null))),
                ErrorCode.INVALID_TOP_K
        );
    }

    @Test
    void shouldAcceptTopKBoundaries() {

        assertThat(catchThrowable(() -> helper.validateSearchRequest(request("q", 1, null)))).isNull();
        assertThat(catchThrowable(() -> helper.validateSearchRequest(request("q", 50, null)))).isNull();
    }

    @Test
    void shouldRejectNonPositiveDocumentId() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request("q", null, 0L))),
                ErrorCode.INVALID_DOCUMENT_ID
        );

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> helper.validateSearchRequest(request("q", null, -5L))),
                ErrorCode.INVALID_DOCUMENT_ID
        );
    }

    @Test
    void shouldAcceptValidRequest() {

        assertThat(catchThrowable(() -> helper.validateSearchRequest(request("q", 10, 7L)))).isNull();
    }

    @Test
    void shouldResolveDefaultAndExplicitTopK() {

        assertThat(helper.resolveTopK(null)).isEqualTo(10);
        assertThat(helper.resolveTopK(25)).isEqualTo(25);
    }

    @Test
    void shouldLogSemanticSearchUsage() {

        helper.logUsage("gemini-embedding-001", 12, AIUsageStatus.SUCCESS, null);

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());

        AIUsageLogRequest request = captor.getValue();

        assertThat(request.getConversationType()).isEqualTo(ConversationType.SEMANTIC_SEARCH);
        assertThat(request.getModel()).isEqualTo("gemini-embedding-001");
        assertThat(request.getInputTokens()).isEqualTo(12);
        assertThat(request.getOutputTokens()).isZero();
        assertThat(request.getStatus()).isEqualTo(AIUsageStatus.SUCCESS);
        assertThat(request.getErrorMessage()).isNull();
    }

    @Test
    void shouldLogFailureReason() {

        helper.logUsage("model", null, AIUsageStatus.FAILED, "qdrant down");

        ArgumentCaptor<AIUsageLogRequest> captor = ArgumentCaptor.forClass(AIUsageLogRequest.class);
        verify(aiUsageLogService).logAiUsage(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(AIUsageStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("qdrant down");
        assertThat(captor.getValue().getInputTokens()).isNull();
    }

    private static SemanticSearchRequest request(String keyword, Integer topK, Long documentId) {

        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(keyword);
        request.setTopK(topK);
        request.setDocumentId(documentId);

        return request;
    }
}
