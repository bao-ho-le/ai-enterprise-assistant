"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertTriangle, FileText, Loader2, RefreshCw } from "lucide-react";
import AssistantComposer from "./AssistantComposer";
import AssistantThread from "./AssistantThread";
import DocumentsDock from "./DocumentsDock";
import GenerationOptionsSheet from "./GenerationOptionsSheet";
import useAssistantThread from "@/features/assistant/hooks/useAssistantThread";
import { modeByConversationType } from "@/features/assistant/models/assistantRules";
import { initialGenerationValues } from "@/features/assistant/config/generationOptions";
import { conversationTypeLabel } from "@/constants/conversation";
import { formatDateTimeSlash } from "@/utils/format";
import DeletedDocumentsWarning from "@/features/conversation/components/DeletedDocumentsWarning";

// /chat/[conversationId] — one screen for all four conversation types.
// The adapter decides what is possible: DOCUMENT_QA gets the chat composer; the
// generation types get their result runs plus regenerate-with-options, never a chat
// input (the backend rejects messages for them with CONVERSATION_TYPE_NOT_CHAT).
export default function AssistantConversation({ conversationId }) {
  const thread = useAssistantThread(conversationId);

  const [prompt, setPrompt] = useState("");
  const [sending, setSending] = useState(false);
  const [actionError, setActionError] = useState("");

  const [contents, setContents] = useState({});
  const [pickerOpen, setPickerOpen] = useState(false);
  const [optionsOpen, setOptionsOpen] = useState(false);
  const [values, setValues] = useState({});
  const [regenerating, setRegenerating] = useState(false);

  const conversationType = thread.conversation?.conversationType;
  const mode = modeByConversationType(conversationType);
  const isChat = Boolean(thread.adapter?.supportsMessages);
  const showDock = Boolean(thread.adapter) && (thread.adapter.supportsMessages || thread.adapter.requiresDocuments);

  // Seed the regenerate form from the latest run's stored inputData (falls back to
  // schema defaults) — the same prefill the old GenerationDetailView did.
  useEffect(() => {
    if (!conversationType || isChat) return;
    setValues(initialGenerationValues(conversationType, thread.latestInputData || {}));
  }, [conversationType, isChat, thread.latestInputData]);

  const loadContent = useCallback(
    async (item) => {
      const generatedContentId = item?.generatedContentId;
      if (generatedContentId == null) return;
      setContents((previous) =>
        previous[generatedContentId] ? previous : { ...previous, [generatedContentId]: { loading: true } }
      );
      try {
        const data = await thread.loadGeneratedContent(generatedContentId);
        setContents((previous) => ({
          ...previous,
          [generatedContentId]: { loading: false, data, error: "" },
        }));
      } catch (err) {
        setContents((previous) => ({
          ...previous,
          [generatedContentId]: {
            loading: false,
            data: null,
            error: err?.message || "Failed to load generated content",
          },
        }));
      }
    },
    [thread.loadGeneratedContent]
  );

  const send = async () => {
    const trimmed = prompt.trim();
    if (!trimmed || sending) return;
    setSending(true);
    setActionError("");
    try {
      await thread.send(trimmed);
      setPrompt("");
    } catch (err) {
      setActionError(err?.message || "Failed to send message. Please try again.");
    } finally {
      setSending(false);
    }
  };

  const regenerate = async () => {
    if (regenerating) return;
    setRegenerating(true);
    setActionError("");
    try {
      await thread.regenerate(values);
    } catch (err) {
      setActionError(err?.message || "Failed to regenerate. Please try again.");
    } finally {
      setRegenerating(false);
    }
  };

  if (thread.loading) {
    return (
      <main className="flex flex-1 items-center justify-center overflow-hidden">
        <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
      </main>
    );
  }

  if (thread.error) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 overflow-hidden">
        <p className="text-sm text-error">{thread.error}</p>
        <button type="button" className="btn-secondary text-sm" onClick={thread.reload}>
          <RefreshCw className="h-4 w-4" />
          Try again
        </button>
      </main>
    );
  }

  if (thread.notFound) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 overflow-hidden px-4 text-center">
        <FileText className="h-6 w-6 text-text-muted" />
        <p className="text-sm text-text-secondary">
          This conversation is unavailable — it may have been deleted, or it has no generation run yet.
        </p>
        <button type="button" className="btn-secondary text-sm" onClick={thread.reload}>
          <RefreshCw className="h-4 w-4" />
          Reload
        </button>
      </main>
    );
  }

  const conversation = thread.conversation;
  const headerDate = conversation?.runCreatedAt || conversation?.createdAt;
  const lockedReason =
    isChat && thread.documents.length === 0
      ? "Attach a document from the panel on the right to start asking questions."
      : "";

  return (
    <>
      <main className="flex flex-1 flex-col overflow-hidden">
        <header className="border-b border-border-subtle px-4 py-3 sm:px-6">
          <div className="mx-auto flex max-w-3xl flex-wrap items-center gap-2">
            <h1 className="min-w-0 flex-1 truncate text-sm font-medium text-text-primary">
              {conversationTypeLabel(conversationType)} - {formatDateTimeSlash(headerDate)}
            </h1>
            {mode && <span className="badge badge-neutral shrink-0">{mode.badge}</span>}
            {!isChat && (
              <button type="button" className="btn-ghost text-xs" onClick={thread.reload} title="Reload runs">
                <RefreshCw className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </header>

        {thread.hasDeletedDocuments && (
          <div className="border-b border-border-subtle px-4 py-2 sm:px-6">
            <div className="mx-auto max-w-3xl">
              <DeletedDocumentsWarning
                documentIds={(thread.documents || []).map((doc) => doc.documentId)}
              />
            </div>
          </div>
        )}

        <AssistantThread
          conversationId={conversationId}
          conversationType={conversationType}
          items={thread.items}
          hasMore={thread.pagination.hasMore}
          loadingOlder={thread.loadingOlder}
          onLoadOlder={thread.loadOlder}
          contents={contents}
          onLoadContent={loadContent}
          onRegenerate={regenerate}
          onOpenOptions={() => setOptionsOpen(true)}
          regenerating={regenerating}
          canRegenerate={!isChat}
          emptyState={
            isChat ? (
              <div className="flex flex-col items-center gap-2 py-10 text-center">
                <FileText className="h-6 w-6 text-text-muted" />
                <p className="text-sm text-text-muted">
                  {thread.documents.length === 0 ? "No document selected yet." : "No messages yet."}
                </p>
                <p className="text-xs text-text-muted">
                  {thread.documents.length === 0
                    ? "Attach a document from the panel on the right to start asking questions."
                    : "Ask a question about your attached documents below."}
                </p>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-2 py-10 text-center">
                <FileText className="h-6 w-6 text-text-muted" />
                <p className="text-sm text-text-muted">No generation run yet for this conversation.</p>
              </div>
            )
          }
        />

        {isChat ? (
          <AssistantComposer
            modes={mode ? [mode] : []}
            mode={mode}
            showModeSelector={false}
            prompt={prompt}
            onPromptChange={setPrompt}
            onSubmit={send}
            submitting={sending}
            error={actionError}
            documentCount={thread.documents.length}
            onAttach={() => setPickerOpen(true)}
            lockReason={lockedReason}
            hint="AI responses are grounded in your selected source documents."
          />
        ) : (
          <div className="border-t border-border-subtle bg-bg-primary p-4">
            <div className="mx-auto max-w-3xl space-y-2">
              <p className="flex items-center justify-center gap-1.5 text-center text-xs text-text-muted">
                <AlertTriangle className="h-3 w-3 shrink-0" />
                Generation conversations are one-shot: edit the options and regenerate to create a new run.
              </p>
              {actionError && <p className="text-center text-xs text-error">{actionError}</p>}
            </div>
          </div>
        )}
      </main>

      {showDock && (
        <DocumentsDock
          mode="conversation"
          conversationId={conversationId}
          documents={thread.documents}
          pickerOpen={pickerOpen}
          onPickerOpenChange={setPickerOpen}
          title={conversationType === "DOCUMENT_QA" ? "Documents" : "Source documents"}
          emptyText="No documents attached yet."
          onAttached={() => {
            thread.refreshDocuments();
            thread.reload();
          }}
        />
      )}

      {!isChat && conversationType && (
        <GenerationOptionsSheet
          open={optionsOpen}
          onClose={() => setOptionsOpen(false)}
          conversationType={conversationType}
          values={values}
          onChange={(name, value) => setValues((previous) => ({ ...previous, [name]: value }))}
          title={`${conversationTypeLabel(conversationType)} options`}
          submitSlot={
            <button type="button" className="btn-primary text-sm" onClick={regenerate} disabled={regenerating}>
              {regenerating ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {regenerating ? "Regenerating…" : "Regenerate"}
            </button>
          }
        />
      )}
    </>
  );
}
