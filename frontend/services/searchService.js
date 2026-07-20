import { apiClient } from "@/lib/apiClient";

// GET /search/semantic -> SemanticSearchResult[]
export function semanticSearch({ keyword, topK, documentId }, signal) {
  return apiClient.get("/search/semantic", {
    params: { keyword, topK, documentId },
    signal,
  });
}
