package com.enterprise.aiassistant.backend.ai.search.service;

import com.enterprise.aiassistant.backend.ai.search.dto.request.SemanticSearchRequest;
import com.enterprise.aiassistant.backend.ai.search.dto.response.SemanticSearchResult;

import java.util.List;

public interface SemanticSearchService {

    List<SemanticSearchResult> search(SemanticSearchRequest request);

}
