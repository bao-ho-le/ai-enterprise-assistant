package com.enterprise.aiassistant.backend.ai.search.helper;

import com.enterprise.aiassistant.backend.ai.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.SearchException;
import org.springframework.stereotype.Component;

@Component
public class SearchHelper {

    private static final int DEFAULT_TOP_K = 10;
    private static final int MAX_TOP_K = 50;
    private static final int MAX_KEYWORD_LENGTH = 255;

    public void validateSearchRequest(SemanticSearchRequest request) {

        if (request == null
                || request.getKeyword() == null
                || request.getKeyword().isBlank()) {

            throw new SearchException(ErrorCode.SEARCH_KEYWORD_REQUIRED);
        }

        if (request.getKeyword().length() > MAX_KEYWORD_LENGTH) {
            throw new SearchException(ErrorCode.KEYWORD_TOO_LONG);
        }

        if (request.getTopK() != null
                && (request.getTopK() < 1 || request.getTopK() > MAX_TOP_K)) {

            throw new SearchException(ErrorCode.INVALID_TOP_K);
        }

        if (request.getDocumentId() != null && request.getDocumentId() <= 0) {
            throw new SearchException(ErrorCode.INVALID_DOCUMENT_ID);
        }
    }

    public int resolveTopK(Integer topK) {
        return topK != null ? topK : DEFAULT_TOP_K;
    }



}
