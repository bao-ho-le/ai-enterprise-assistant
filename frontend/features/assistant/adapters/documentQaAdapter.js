import {
  getConversationDocuments,
  getDocumentQaConversationDetail,
  startDocumentQaConversation,
} from "@/services/conversationService";
import { getMessages, sendMessage } from "@/services/messageService";
import { mergeTimeline, toQaTimeline } from "../models/timelineModel";

// Document QA adapter — the only message-based conversation type.
// Mirrors the behaviour of the old DocumentQaDetailView/DocumentQaDraftView:
//   * latest N messages come from GET /ai-conversations/{id}
//   * older pages use keyset pagination (beforeId) via GET /{id}/messages
//   * a new conversation is created atomically together with its first message
//     (POST /ai-conversations/document-qa/start) so an empty conversation can't exist.
export const RECENT_MESSAGES_LIMIT = 20;

export const documentQaAdapter = {
  conversationType: "DOCUMENT_QA",
  supportsMessages: true,
  requiresDocuments: true,

  // `detail` may be passed in when the caller already probed the conversation type
  // (see adapters/index.loadConversationThread) so the detail isn't fetched twice.
  async loadConversation(conversationId, { signal, detail: prefetched } = {}) {
    const detail =
      prefetched || (await getDocumentQaConversationDetail(conversationId, RECENT_MESSAGES_LIMIT, signal));
    const messages = detail?.recentMessages || [];
    return {
      conversation: detail,
      items: toQaTimeline(messages),
      pagination: {
        hasMore: Boolean(detail?.hasMoreMessages),
        cursor: messages[0]?.id ?? null,
      },
      attachedDocuments: detail?.attachedDocuments || [],
      hasDeletedAttachedDocuments: Boolean(detail?.hasDeletedAttachedDocuments),
    };
  },

  async loadOlder(conversationId, { cursor, signal } = {}) {
    const slice = await getMessages(conversationId, { beforeId: cursor, size: RECENT_MESSAGES_LIMIT }, signal);
    const older = slice?.content || [];
    return {
      items: toQaTimeline(older),
      hasMore: Boolean(slice?.hasMore),
      cursor: older[0]?.id ?? cursor ?? null,
    };
  },

  async sendMessage(conversationId, content) {
    const result = await sendMessage(conversationId, content);
    return mergeTimeline([], toQaTimeline([result?.userMessage, result?.assistantMessage].filter(Boolean)));
  },

  async start({ prompt, documentVersionIds }) {
    const result = await startDocumentQaConversation(documentVersionIds, prompt);
    return {
      conversationId: result?.conversation?.id,
      conversation: result?.conversation ?? null,
    };
  },

  async loadDocuments(conversationId, { signal } = {}) {
    return getConversationDocuments(conversationId, signal);
  },
};
