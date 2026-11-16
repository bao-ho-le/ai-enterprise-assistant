"use client";

import { useState } from "react";
import { FileText, Plus, X } from "lucide-react";
import AttachDocumentsModal from "@/features/conversation/components/AttachDocumentsModal";
import { attachmentIconStyle } from "@/utils/format";

// Document selection rendered inline in the creation panel (not only in a side dock), so
// it stays visible and usable on tablet/mobile too. Staged documents only — nothing is
// persisted until the request is submitted (see useAssistantComposer.submit).
export default function SelectedDocumentsSection({
  documents = [],
  onPick,
  onRemove,
  title = "Source documents",
  description,
  required = false,
  error = "",
}) {
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="text-sm font-semibold text-text-primary">
            {title}
            {required && <span className="text-error"> *</span>}
          </h3>
          {description && <p className="mt-0.5 text-xs leading-relaxed text-text-muted">{description}</p>}
        </div>
        <button type="button" className="btn-secondary text-xs" onClick={() => setPickerOpen(true)}>
          <Plus className="h-3.5 w-3.5" />
          Add documents
        </button>
      </div>

      {documents.length === 0 ? (
        <button
          type="button"
          onClick={() => setPickerOpen(true)}
          className={`flex w-full flex-col items-center gap-1 rounded-lg border border-dashed bg-bg-primary px-4 py-6 text-center transition-colors hover:border-accent/50 ${
            error ? "border-warning/50" : "border-border-default"
          }`}
        >
          <FileText className="h-5 w-5 text-text-muted" />
          <span className="text-sm text-text-secondary">No documents selected</span>
          <span className="text-xs text-text-muted">
            {required ? "At least one source document is required for this mode." : "Optional for this mode."}
          </span>
        </button>
      ) : (
        <ul className="divide-y divide-border-subtle overflow-hidden rounded-lg border border-border-subtle">
          {documents.map((doc, index) => (
            <li key={doc.documentVersionId} className="flex items-center gap-3 bg-bg-primary px-3 py-2">
              <span
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${attachmentIconStyle(index).bg}`}
              >
                <FileText className={`h-4 w-4 ${attachmentIconStyle(index).color}`} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-text-primary" title={doc.documentTitle}>
                  {doc.documentTitle || "Untitled document"}
                </p>
                <p className="text-[11px] text-text-muted">v{doc.versionNumber}</p>
              </div>
              <button
                type="button"
                className="btn-ghost p-1"
                aria-label={`Remove ${doc.documentTitle || "document"}`}
                onClick={() => onRemove?.(doc)}
              >
                <X className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {documents.length > 0 && (
        <p className="text-[11px] text-text-muted">
          {documents.length} document{documents.length === 1 ? "" : "s"} selected
        </p>
      )}

      <AttachDocumentsModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        alreadyAttachedDocumentIds={documents.map((doc) => doc.documentId)}
        onSelect={(selected) => {
          setPickerOpen(false);
          onPick?.(selected);
        }}
      />
    </section>
  );
}
