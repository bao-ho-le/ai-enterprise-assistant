"use client";

import { useCallback, useEffect, useState } from "react";
import { Upload, FileText, X, Loader2 } from "lucide-react";
import ConfirmDialog from "@/features/document/components/ConfirmDialog";
import Toast from "@/components/ui/Toast";
import AttachDocumentsModal from "@/features/conversation/components/AttachDocumentsModal";
import { getConversationDocuments, removeDocument } from "@/services/conversationService";
import { attachmentIconStyle } from "@/utils/format";

export default function RightDocumentPanel({ conversationId }) {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const [attachOpen, setAttachOpen] = useState(false);
  const [removeTarget, setRemoveTarget] = useState(null);
  const [removing, setRemoving] = useState(false);
  const [toast, setToast] = useState(null);
  const notify = useCallback((type, text) => setToast({ type, text }), []);

  // DocumentQaDetailView fetches its own copy of attachedDocuments independently —
  // nudge it to resync so the chat-lock state reacts without a page reload.
  const notifyDocumentsChanged = useCallback(() => {
    window.dispatchEvent(new CustomEvent("conversation-documents-changed", { detail: { conversationId } }));
  }, [conversationId]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getConversationDocuments(conversationId, controller.signal)
      .then((docs) => {
        if (!controller.signal.aborted) setDocuments(docs || []);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load attached documents");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [conversationId, reloadKey]);

  const confirmRemove = async () => {
    setRemoving(true);
    try {
      await removeDocument(conversationId, removeTarget.documentVersionId);
      notify("success", "Document removed");
      setRemoveTarget(null);
      reload();
      notifyDocumentsChanged();
    } catch (err) {
      notify("error", err.message || "Failed to remove document");
    } finally {
      setRemoving(false);
    }
  };

  return (
    <aside className="hidden xl:flex w-80 shrink-0 flex-col border-l border-border-subtle bg-bg-primary">
      <div className="flex h-14 items-center justify-between p-4">
        <button
          type="button"
          className="btn-secondary w-full text-sm transition-colors hover:bg-bg-elevated mt-4"
          onClick={() => setAttachOpen(true)}
        >
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
          <p className="text-sm text-text-muted">No documents attached yet.</p>
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
                  onClick={() => setRemoveTarget(doc)}
                >
                  <X className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <AttachDocumentsModal
        open={attachOpen}
        onClose={() => setAttachOpen(false)}
        conversationId={conversationId}
        alreadyAttachedDocumentIds={documents.map((d) => d.documentId)}
        onAttached={() => {
          setAttachOpen(false);
          notify("success", "Document(s) attached");
          reload();
          notifyDocumentsChanged();
        }}
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
