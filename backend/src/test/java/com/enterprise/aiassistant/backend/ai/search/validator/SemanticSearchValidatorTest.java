package com.enterprise.aiassistant.backend.ai.search.validator;

import com.enterprise.aiassistant.backend.ai.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.SearchException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticSearchValidatorTest {

    private final SemanticSearchValidator validator = new SemanticSearchValidator();

    private SemanticSearchRequest requestWith(String keyword, Integer topK, Long documentId) {
        SemanticSearchRequest request = new SemanticSearchRequest();
        request.setKeyword(keyword);
        request.setTopK(topK);
        request.setDocumentId(documentId);
        return request;
    }

    @Test
    void validate_blankKeyword_throwsKeywordRequired() {
        SearchException ex = assertThrows(SearchException.class,
                () -> validator.validate(requestWith("  ", null, null)));
        assertEquals(ErrorCode.SEARCH_KEYWORD_REQUIRED, ex.getErrorCode());
    }

    @Test
    void validate_topKAboveMax_throwsInvalidTopK() {
        SearchException ex = assertThrows(SearchException.class,
                () -> validator.validate(requestWith("machine learning", 51, null)));
        assertEquals(ErrorCode.INVALID_TOP_K, ex.getErrorCode());
    }

    @Test
    void validate_topKZero_throwsInvalidTopK() {
        SearchException ex = assertThrows(SearchException.class,
                () -> validator.validate(requestWith("machine learning", 0, null)));
        assertEquals(ErrorCode.INVALID_TOP_K, ex.getErrorCode());
    }

    @Test
    void validate_nonPositiveDocumentId_throwsInvalidDocumentId() {
        SearchException ex = assertThrows(SearchException.class,
                () -> validator.validate(requestWith("machine learning", 5, 0L)));
        assertEquals(ErrorCode.INVALID_DOCUMENT_ID, ex.getErrorCode());
    }

    @Test
    void validate_validRequest_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validate(requestWith("machine learning", 10, 3L)));
    }

    @Test
    void resolveTopK_null_returnsDefault() {
        assertEquals(10, validator.resolveTopK(null));
    }

    @Test
    void resolveTopK_present_returnsAsIs() {
        assertEquals(25, validator.resolveTopK(25));
    }

}
