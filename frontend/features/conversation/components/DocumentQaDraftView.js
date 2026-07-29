"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Send, Loader2, FileText, AlertTriangle } from "lucide-react";
import SelectedDocumentsPanel from "./SelectedDocumentsPanel";
import { startDocumentQaConversation } from "@/services/conversationService";
import { ApiError } from "@/lib/apiClient";

// "New conversation" for Document QA no longer calls createConversation up front —
// nothing is persisted until the first message actually sends. Attached documents
// stay in local state (stagedDocuments) until then; the chat input stays locked
// until at least one is picked. Submitting calls the atomic /document-qa/start
// endpoint (create + attach + first message, one transaction) and only then
// navigates to the real conversation route.
//
// When navigated from File Storage with pre-selected documents (stored in
// sessionStorage by FileStorageView), this component reads them on mount and
// pre-populates the document panel — no auto-send, user still clicks Send.
export default function DocumentQaDraftView() {
  const router = useRouter();
  const [stagedDocuments, setStagedDocuments] = useState([]);

  // Restore documents passed from File Storage via sessionStorage.
  useEffect(() => {
    try {
      const raw = sessionStorage.getItem("file-storage-attach");
      if (raw) {
        const docs = JSON.parse(raw);
        sessionStorage.removeItem("file-storage-attach");
        if (Array.isArray(docs) && docs.length > 0) {
          setStagedDocuments(docs);
        }
      }
    } catch {
      // Ignore parse errors — user can pick documents manually.
    }
  }, []);
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const locked = stagedDocuments.length === 0;

  const submit = async (e) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed || submitting || locked) return;
    setSubmitting(true);
    setError("");
    try {
      const result = await startDocumentQaConversation(
        stagedDocuments.map((d) => d.documentVersionId),
        trimmed
      );
      router.push(`/document-qa/${result.conversation.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to start conversation. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <>
      <main className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
          <div className="max-w-3xl mx-auto space-y-6">            <div className="flex flex-col items-center gap-2 text-center py-10">
              <FileText className="h-6 w-6 text-text-muted" />
              <p className="text-sm text-text-muted">{locked ? "No document selected yet." : `${stagedDocuments.length} document(s) ready.`}</p>
              {locked && (
                <p className="flex items-center justify-center gap-1.5 text-xs text-warning">
                  <AlertTriangle className="h-3 w-3 shrink-0" />
                  Attach a document from the panel on the right to start asking questions.
                </p>
              )}
            </div>
          </div>
        </div>

        <div className="p-4">
          <form className="flex items-end gap-3 max-w-3xl mx-auto" onSubmit={submit}>
            <div className="flex-1">
              <div className="flex items-center gap-2 rounded-xl border border-border-subtle bg-bg-card px-3 py-2 min-h-[48px]">
                <textarea
                  rows={1}
                  placeholder={locked ? "Attach a document to start chatting" : "Ask a question about your documents..."}
                  className="flex-1 resize-none bg-transparent text-sm text-text-primary outline-none leading-normal disabled:opacity-50"
                  aria-label="Chat message input"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) submit(e);
                  }}
                  disabled={submitting || locked}
                />
                <button
                  type="submit"
                  className="flex h-8 w-8 items-center justify-center rounded-md bg-bg-elevated text-text-muted hover:bg-bg-card disabled:opacity-50"
                  aria-label="Send message"
                  disabled={!content.trim() || submitting || locked}
                >
                  {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                </button>
              </div>
            </div>
          </form>
          {error ? (
            <p className="text-center text-xs text-error mt-2">{error}</p>
          ) : locked ? (
            <p className="text-center text-xs text-text-muted mt-2">
              Ask questions only about the attached document(s). The AI will answer based on their content.
            </p>
          ) : (
            <p className="text-center text-xs text-text-muted mt-2">
              AI responses are grounded in your selected source documents
            </p>
          )}
        </div>
      </main>

      <SelectedDocumentsPanel documents={stagedDocuments} onChange={setStagedDocuments} />
    </>
  );
}
