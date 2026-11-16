"use client";

import { useState } from "react";
import { FileText, Loader2, Upload, X } from "lucide-react";
import AttachDocumentsModal from "@/features/conversation/components/AttachDocumentsModal";
import ConfirmDialog from "@/features/document/components/ConfirmDialog";
import Toast from "@/components/ui/Toast";
import { removeDocument } from "@/services/conversationService";
import { attachmentIconStyle } from "@/utils/format";

// One documents panel for both states, kept deliberately distinct:
//   mode="staged"        — documents picked for a conversation that does not exist yet
//                          (composer selection only; nothing persisted until submit)
//   mode="conversation"  — documents already attached to an open conversation (the
//                          attach/remove calls hit the backend immediately)
// The picker modal is controlled so the composer's paperclip button can open it too.
export default function DocumentsDock({
  mode,
  conversationId,
  documents = [],
  loading = false,
  error = "",
  title = "Documents",
  emptyText = "No documents selected yet.",
  pickerOpen,
  onPickerOpenChange,
  onPick,
  onAttached,
  onRemove,
}) {
  const [removeTarget, setRemoveTarget] = useState(null);
  const [removing, setRemoving] = useState(false);
  const [toast, setToast] = useState(null);

  const openPicker = () => onPickerOpenChange?.(true);
  const closePicker = () => onPickerOpenChange?.(false);

  const confirmRemove = async () => {
    if (!removeTarget) return;
    setRemoving(true);
    try {
      await removeDocument(conversationId, removeTarget.documentVersionId);
      setToast({ type: "success", text: "Document removed" });
      setRemoveTarget(null);
      onAttached?.();
    } catch (err) {
      setToast({ type: "error", text: err?.message || "Failed to remove document" });
    } finally {
      setRemoving(false);
    }
  };

  return (
    <aside className="hidden xl:flex w-80 shrink-0 flex-col border-l border-border-subtle bg-bg-primary">
      <div className="border-b border-border-subtle p-4">
        <p className="text-xs font-medium uppercase tracking-wider text-text-muted">{title}</p>
        <button type="button" className="btn-secondary mt-3 w-full text-sm" onClick={openPicker}>
          <Upload className="h-4 w-4" />
          Attach Document
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <p className="mb-3 text-xs font-medium uppercase tracking-wider text-text-muted">
          Attached ({documents.length})
        </p>

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-8 text-text-muted">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <p className="text-sm text-error">{error}</p>
        ) : documents.length === 0 ? (
          <p className="text-sm text-text-muted">{emptyText}</p>
        ) : (
          <ul className="space-y-2">
            {documents.map((doc, index) => (
              <li key={doc.documentVersionId} className="card flex items-center gap-3 p-3">
                <span
                  className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${attachmentIconStyle(index).bg}`}
                >
                  <FileText className={`h-4 w-4 ${attachmentIconStyle(index).color}`} />
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
                  onClick={() =>
                    mode === "conversation" ? setRemoveTarget(doc) : onRemove?.(doc)
                  }
                >
                  <X className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <AttachDocumentsModal
        open={Boolean(pickerOpen)}
        onClose={closePicker}
        conversationId={mode === "conversation" ? conversationId : undefined}
        alreadyAttachedDocumentIds={documents.map((doc) => doc.documentId)}
        onSelect={
          mode === "staged"
            ? (selected) => {
                closePicker();
                onPick?.(selected);
              }
            : undefined
        }
        onAttached={
          mode === "conversation"
            ? () => {
                closePicker();
                setToast({ type: "success", text: "Document(s) attached" });
                onAttached?.();
              }
            : undefined
        }
      />

      <ConfirmDialog
        open={Boolean(removeTarget)}
        onClose={() => setRemoveTarget(null)}
        onConfirm={confirmRemove}
        loading={removing}
        title="Remove document"
        confirmLabel="Remove"
        message={`Remove "${removeTarget?.documentTitle}" from this conversation? This cannot be undone.`}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </aside>
  );
}
