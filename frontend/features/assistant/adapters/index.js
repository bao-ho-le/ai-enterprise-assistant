import { getDocumentQaConversationDetail } from "@/services/conversationService";
import { ApiError } from "@/lib/apiClient";
import { emailAdapter } from "./emailAdapter";
import { reportAdapter } from "./reportAdapter";
import { summaryAdapter } from "./summaryAdapter";
import { documentQaAdapter, RECENT_MESSAGES_LIMIT } from "./documentQaAdapter";

// Conversation-type -> adapter. DOCUMENT_QA is message-based; the other three are
// generation-based and share one implementation (see generationAdapter).
const ADAPTERS = {
  DOCUMENT_QA: documentQaAdapter,
  EMAIL_GENERATION: emailAdapter,
  REPORT_GENERATION: reportAdapter,
  SUMMARY_GENERATION: summaryAdapter,
};

export function getAdapter(conversationType) {
  return ADAPTERS[conversationType] || null;
}

// The route only carries the conversation id, and the conversation type decides which
// detail endpoint to use. The QA detail endpoint accepts any owned conversation and
// returns conversationType (generation conversations simply have no messages), so it
// doubles as the type probe — one extra read instead of guessing from error codes.
export async function loadConversationThread(conversationId, { signal } = {}) {
  const probe = await getDocumentQaConversationDetail(conversationId, RECENT_MESSAGES_LIMIT, signal);
  const adapter = getAdapter(probe?.conversationType);
  if (!adapter) {
    throw new ApiError(`Unsupported conversation type: ${probe?.conversationType || "unknown"}`, 400, null);
  }

  const loaded = adapter.supportsMessages
    ? await adapter.loadConversation(conversationId, { signal, detail: probe })
    : await adapter.loadConversation(conversationId, { signal });

  return { adapter, conversationType: probe?.conversationType, ...loaded };
}

export { documentQaAdapter, emailAdapter, reportAdapter, summaryAdapter };
