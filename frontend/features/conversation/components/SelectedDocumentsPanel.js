"use client";

import { useState } from "react";
import { Upload, FileText, X } from "lucide-react";
import AttachDocumentsModal from "./AttachDocumentsModal";
import { attachmentIconStyle } from "@/utils/format";

// Right panel for generation create-forms (Report/Summary): no conversation exists
// yet, so picks stay in local state and only get attached (real API call) once the
// form is submitted and a conversation actually exists — see GenerationForm.submit.
export default function SelectedDocumentsPanel({ documents, onChange }) {
  const [pickerOpen, setPickerOpen] = useState(false);

  const remove = (documentVersionId) => {
    onChange(documents.filter((d) => d.documentVersionId !== documentVersionId));
  };

  return (
    <aside className="hidden xl:flex w-80 shrink-0 flex-col border-l border-border-subtle bg-bg-primary">
      <div className="flex h-14 items-center justify-between p-4">
        <button
          type="button"
          className="btn-secondary w-full text-sm transition-colors hover:bg-bg-elevated mt-4"
          onClick={() => setPickerOpen(true)}
        >
          <Upload className="h-4 w-4" />
          Attach Document
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <p className="mb-3 text-xs font-medium uppercase tracking-wider text-text-muted">
          Attached ({documents.length})
        </p>

        {documents.length === 0 ? (
          <p className="text-sm text-text-muted">No documents selected yet.</p>
        ) : (
          <ul className="space-y-2">
            {documents.map((doc, i) => (
              <li key={doc.documentVersionId} className="card flex items-center gap-3 p-3">
                <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${attachmentIconStyle(i).bg}`}>
                  <FileText className={`h-4 w-4 ${attachmentIconStyle(i).color}`} />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-text-primary" title={doc.documentTitle}>
                    {doc.documentTitle}
                  </p>
                  <p className="text-xs text-text-muted">v{doc.versionNumber}</p>
                </div>
                <button
                  type="button"
                  className="btn-ghost p-1"
                  aria-label="Remove document"
                  onClick={() => remove(doc.documentVersionId)}
                >
                  <X className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <AttachDocumentsModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        alreadyAttachedDocumentIds={documents.map((d) => d.documentId)}
        onSelect={(newlySelected) => {
          setPickerOpen(false);
          onChange([...documents, ...newlySelected]);
        }}
      />
    </aside>
  );
}
