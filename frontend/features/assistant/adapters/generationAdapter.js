import {
  getConversationDocuments,
  getConversationGenerations,
  getGenerationConversationDetail,
  startGenerationConversation,
  triggerGeneration,
} from "@/services/conversationService";
import { getGeneratedContentById } from "@/services/generatedContentService";
import { buildGenerationInput } from "../config/generationOptions";
import { parseInputData, toGenerationTimeline } from "../models/timelineModel";

export const HISTORY_PAGE_SIZE = 10;

// Generation adapter factory — Email / Report / Summary differ only in their
// conversationType + input schema, so they share one implementation.
//
// Backend contract (unchanged by this refactor):
//   GET  /ai-conversations/{id}/generation-detail  -> latest run, inputData, status
//   GET  /ai-conversations/{id}/generations        -> Slice<GenerationResponse> (metadata)
//   GET  /generated-contents/{generatedContentId}  -> content body
//   POST /ai-conversations/generation/start        -> create + first run
//   POST /ai-conversations/{id}/generate           -> re-run with new inputData
//
// Generation conversations are NOT chat conversations (AIMessageHelper restricts
// POST /messages to DOCUMENT_QA) — the adapter exposes regenerate(), never sendMessage().
export function createGenerationAdapter(conversationType) {
  return {
    conversationType,
    supportsMessages: false,
    requiresDocuments: conversationType !== "EMAIL_GENERATION",

    async loadConversation(conversationId, { signal } = {}) {
      const detail = await getGenerationConversationDetail(conversationId, signal);
      const history = await getConversationGenerations(
        conversationId,
        { page: 0, size: HISTORY_PAGE_SIZE },
        signal
      );
      const runs = history?.content || [];
      const inputData = parseInputData(detail?.inputData);
      return {
        conversation: detail,
        items: toGenerationTimeline(runs, {
          conversationType,
          latestGenerationId: detail?.generationId ?? null,
          inputData,
          status: detail?.status,
          generatedContentId: detail?.generatedContentId,
          latestRunCreatedAt: detail?.runCreatedAt,
        }),
        pagination: { hasMore: Boolean(history?.hasNext), page: 0 },
        attachedDocuments: detail?.attachedDocuments ?? null,
        hasDeletedAttachedDocuments: Boolean(detail?.hasDeletedAttachedDocuments),
        latestInputData: inputData,
        latestGeneratedContentId: detail?.generatedContentId ?? null,
        latestStatus: detail?.status ?? null,
      };
    },

    async loadOlder(conversationId, { page = 1, signal } = {}) {
      const slice = await getConversationGenerations(
        conversationId,
        { page, size: HISTORY_PAGE_SIZE },
        signal
      );
      return {
        items: toGenerationTimeline(slice?.content || [], { conversationType }),
        hasMore: Boolean(slice?.hasNext),
        page,
      };
    },

    // Tolerant loader: returns null for runs the backend never produced content for
    // (PENDING/RUNNING/FAILED), so the result card can keep showing the status instead.
    async loadGeneratedContent(generatedContentId, { signal } = {}) {
      if (generatedContentId == null) return null;
      return getGeneratedContentById(generatedContentId, signal);
    },

    async start({ prompt, documentVersionIds, values }) {
      const inputData = buildGenerationInput(conversationType, values, prompt);
      const result = await startGenerationConversation(conversationType, documentVersionIds, inputData);
      return {
        conversationId: result?.conversation?.id,
        conversation: result?.conversation ?? null,
        generation: result?.generation ?? null,
      };
    },

    async regenerate(conversationId, values) {
      return triggerGeneration(conversationId, values);
    },

    async loadDocuments(conversationId, { signal } = {}) {
      return getConversationDocuments(conversationId, signal);
    },
  };
}
