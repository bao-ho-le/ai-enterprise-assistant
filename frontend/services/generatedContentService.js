import { apiClient } from "@/lib/apiClient";

// All /generated-contents endpoints (GeneratedController) live here.
// Components/hooks call these, never fetch directly.

// GET /generated-contents  (generatedType filter + Pageable)
// -> Spring Slice<GeneratedContentResponse> — no totalElements/totalPages, only hasNext()
export function getGeneratedContents(params, signal) {
  return apiClient.get("/generated-contents", { params, signal });
}

// GET /generated-contents/{generatedContentId} -> GeneratedContentDetailResponse (with full content)
export function getGeneratedContentById(generatedContentId, signal) {
  return apiClient.get(`/generated-contents/${generatedContentId}`, { signal });
}

// PUT /generated-contents/{generatedContentId}  { title, content } -> GeneratedContentDetailResponse
export function updateGeneratedContent(generatedContentId, { title, content }) {
  return apiClient.putJson(`/generated-contents/${generatedContentId}`, { title, content });
}
