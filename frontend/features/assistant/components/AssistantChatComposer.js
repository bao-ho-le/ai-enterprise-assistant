"use client";

import { useState } from "react";
import { FileText, Loader2, Paperclip, Send, X } from "lucide-react";
import AttachDocumentsModal from "@/features/conversation/components/AttachDocumentsModal";

// Ask / Document QA keeps the original chatbox shape: one rounded input bar holding the
// question, with the attach and send actions inline on the right and the attached
// documents as removable chips just above it. Enter sends (Shift+Enter adds a line).
// Nothing here builds a payload — the hook still submits through the DOCUMENT_QA adapter.
export default function AssistantChatComposer({
  mode,
  prompt,
  onPromptChange,
  onSubmit,
  documents = [],
  onPick,
  onRemove,
  submitting = false,
  disabled = false,
  canSubmit = false,
  error = "",
  documentsError = "",
  hint = "",
}) {
  const [pickerOpen, setPickerOpen] = useState(false);

  const submit = (event) => {
    event?.preventDefault?.();
    if (!canSubmit) return;
    onSubmit?.();
  };

  return (
    <form onSubmit={submit} className="space-y-2">
      {documents.length > 0 && (
        <ul className="flex flex-wrap justify-center gap-1.5">
          {documents.map((doc) => (
            <li key={doc.documentVersionId}>
              <span className="inline-flex max-w-[260px] items-center gap-1.5 rounded-md border border-border-subtle bg-bg-elevated py-1 pl-2 pr-1 text-xs text-text-secondary">
                <FileText className="h-3.5 w-3.5 shrink-0 text-text-muted" />
                <span className="truncate" title={doc.documentTitle}>
                  {doc.documentTitle || "Untitled document"}
                </span>
                <button
                  type="button"
                  className="rounded p-0.5 text-text-muted hover:bg-bg-secondary hover:text-text-primary"
                  aria-label={`Remove ${doc.documentTitle || "document"}`}
                  onClick={() => onRemove?.(doc)}
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="flex min-h-[52px] items-center gap-2 rounded-xl border border-border-subtle bg-bg-primary px-3 py-2 transition-colors focus-within:border-accent/60">
        <textarea
          rows={1}
          aria-label={mode?.placeholder || "Ask a question"}
          placeholder={mode?.placeholder || "Ask a question about your documents…"}
          className="max-h-40 flex-1 resize-none bg-transparent py-0 text-sm leading-5 text-text-primary outline-none placeholder:text-text-muted disabled:opacity-50"
          value={prompt}
          onChange={(event) => onPromptChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              submit(event);
            }
          }}
          disabled={submitting || disabled}
        />

        <button
          type="button"
          className="flex h-8 shrink-0 items-center gap-1.5 rounded-md px-1.5 text-xs text-text-muted transition-colors hover:bg-bg-elevated hover:text-text-primary disabled:opacity-40"
          onClick={() => setPickerOpen(true)}
          disabled={submitting || disabled}
          title="Attach documents"
          aria-label="Attach documents"
        >
          <Paperclip className="h-4 w-4" />
          {documents.length > 0 && <span className="tabular-nums">{documents.length}</span>}
        </button>

        <button
          type="submit"
          className="flex h-8 shrink-0 items-center gap-1.5 rounded-md bg-accent px-3 text-xs font-medium text-white transition-colors hover:bg-accent-hover disabled:opacity-40"
          disabled={!canSubmit}
          title={mode?.submitLabel || "Send"}
        >
          {submitting ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Send className="h-3.5 w-3.5" />
          )}
          {submitting ? "Sending…" : "Send"}
        </button>
      </div>

      <div className="text-center">
        {error ? (
          <p className="text-xs text-error">{error}</p>
        ) : documentsError ? (
          <p className="text-xs text-warning">{documentsError}</p>
        ) : (
          hint && <p className="text-xs text-text-muted">{hint}</p>
        )}
      </div>

      <AttachDocumentsModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        alreadyAttachedDocumentIds={documents.map((doc) => doc.documentId)}
        onSelect={(selected) => {
          setPickerOpen(false);
          onPick?.(selected);
        }}
      />
    </form>
  );
}
