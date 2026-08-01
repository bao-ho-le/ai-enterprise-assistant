import { apiClient } from "@/lib/apiClient";

// All /ai-conversations/{conversationId}/messages endpoints (AIMessageController)
// live here. Components/hooks call these, never fetch directly.

// POST /ai-conversations/{conversationId}/messages  { content } -> MessageResponse { userMessage, assistantMessage }
export function sendMessage(conversationId, content) {
  return apiClient.postJson(`/ai-conversations/${conversationId}/messages`, { content });
}

// GET /ai-conversations/{conversationId}/messages  { beforeId?, size? }
// Keyset pagination: omit beforeId for the latest `size` messages, or pass the id of
// the oldest message currently loaded to get the `size` messages right before it.
// -> MessagePageResponse { content: AIMessageResponse[] (oldest-first), hasMore: boolean
// (are there even older messages beyond this page) }.
export function getMessages(conversationId, params, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/messages`, { params, signal });
}

// GET /ai-conversations/{conversationId}/messages/{messageId} -> MessageDetailResponse (with RAG sources)
export function getMessageDetail(conversationId, messageId, signal) {
  return apiClient.get(`/ai-conversations/${conversationId}/messages/${messageId}`, { signal });
}
