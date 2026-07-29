import { apiClient } from "@/lib/apiClient";

// All /ai-conversations endpoints (AIConversationController) live here.
// Components/hooks call these, never fetch directly.

// GET /ai-conversations  (ConversationFilterRequest: conversationType, status + Pageable)
// -> Spring Slice<ConversationResponse>
export function getConversations(params, signal) {
  return apiClient.get("/ai-conversations", { params, signal });
}

// GET /ai-conversations/deleted  (conversationType + Pageable) -> Spring Slice<ConversationResponse>
// Soft-deleted only; the backend pins status = DELETED, this can't return active ones.
export function getDeletedConversations(params, signal) {
  return apiClient.get("/ai-conversations/deleted", { params, signal });
}

// POST /ai-conversations/generation/start  { conversationType, documentVersionIds, inputData }
// -> StartGenerationConversationResponse { conversation, generation: TriggerGenerationResponse }
// Atomic on the backend (one transaction), mirrors startDocumentQaConversation below.
export function startGenerationConversation(conversationType, documentVersionIds, inputData) {
  return apiClient.postJson("/ai-conversations/generation/start", {
    conversationType,
    documentVersionIds,
    inputData,
  });
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

// GET /ai-conversations/{conversationId}/generations  (Pageable)
// -> Spring Slice<GenerationResponse> { generationId, status, createdAt, generatedContentId }
// — no totalElements/totalPages, only hasNext(); metadata only, no title/generatedType.
export function getConversationGenerations(conversationId, params, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/generations`, {
    params,
    signal,
  });
}

// POST /ai-conversations/{conversationId}/generate  { inputData } -> TriggerGenerationResponse
// { generationRunId, status, generatedContentId, errorMessage }
export function triggerGeneration(conversationId, inputData) {
  return apiClient.postJson(`/ai-conversations/${conversationId}/generate`, { inputData });
}

// POST /ai-conversations/document-qa/start  { documentVersionIds, content }
// -> StartDocumentQaConversationResponse { conversation, message }
// Atomic on the backend (one transaction) — creates the conversation, attaches the
// documents, and sends the first message in one call, so no empty conversation can
// ever be left behind if something fails partway through.
export function startDocumentQaConversation(documentVersionIds, content) {
  return apiClient.postJson("/ai-conversations/document-qa/start", { documentVersionIds, content });
}
