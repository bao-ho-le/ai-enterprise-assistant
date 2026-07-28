import { apiClient } from "@/lib/apiClient";

// All /ai-conversations endpoints (AIConversationController) live here.
// Components/hooks call these, never fetch directly.

// GET /ai-conversations  (ConversationFilterRequest: conversationType, status + Pageable)
// -> Spring Page<ConversationResponse>
export function getConversations(params, signal) {
  return apiClient.get("/ai-conversations", { params, signal });
}

// POST /ai-conversations  { title, conversationType } -> ConversationResponse
export function createConversation({ title, conversationType }) {
  return apiClient.postJson("/ai-conversations", { title, conversationType });
}

// PUT /ai-conversations/{conversationId}  { title } -> ConversationResponse
export function renameConversation(conversationId, title) {
  return apiClient.putJson(`/ai-conversations/${conversationId}`, { title });
}

// DELETE /ai-conversations/{conversationId} -> soft delete (status = DELETED)
export function softDeleteConversation(conversationId) {
  return apiClient.del(`/ai-conversations/${conversationId}`);
}

// DELETE /ai-conversations/{conversationId}/hard -> permanent, cascades messages/documents/generated content
export function hardDeleteConversation(conversationId) {
  return apiClient.del(`/ai-conversations/${conversationId}/hard`);
}

// GET /ai-conversations/{conversationId}?recentMessagesLimit= -> DocumentQaConversationDetailResponse
export function getDocumentQaConversationDetail(conversationId, recentMessagesLimit, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}`, {
    params: { recentMessagesLimit },
    signal,
  });
}

// GET /ai-conversations/{conversationId}/generation-detail -> GenerationConversationDetailResponse
// attachedDocuments is null when conversationType = EMAIL_GENERATION.
export function getGenerationConversationDetail(conversationId, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/generation-detail`, { signal });
}

// GET /ai-conversations/{conversationId}/documents -> ConversationDocumentResponse[]
export function getConversationDocuments(conversationId, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/documents`, { signal });
}

// POST /ai-conversations/{conversationId}/documents  { documentVersionIds: number[] }
// -> AttachDocumentsResponse { documents: AttachedDocumentItem[] }
export function attachDocuments(conversationId, documentVersionIds) {
  return apiClient.postJson(`/ai-conversations/${conversationId}/documents`, {
    documentVersionIds,
  });
}

// DELETE /ai-conversations/{conversationId}/documents/{documentVersionId} -> unattach, permanent
export function removeDocument(conversationId, documentVersionId) {
  return apiClient.del(`/ai-conversations/${conversationId}/documents/${documentVersionId}`);
}

// GET /ai-conversations/{conversationId}/generated-content  (Pageable)
// -> Spring Slice<GeneratedContentResponse> — no totalElements/totalPages, only hasNext()
export function getConversationGeneratedContents(conversationId, params, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/generated-content`, {
    params,
    signal,
  });
}
