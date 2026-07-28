"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { listDocuments, getDocument } from "@/services/documentService";
import { attachDocuments } from "@/services/conversationService";
import { documentTypeLabel } from "@/constants/document";
import { ApiError } from "@/lib/apiClient";

const PAGE_SIZE = 50;

// Document list rows carry no versionId (backend DocumentListResponse omits it) —
// resolve each selected document's current version at submit time, same pattern
// as documentService.downloadCurrentVersion.
export default function AttachDocumentsModal({ open, onClose, conversationId, alreadyAttachedDocumentIds, onAttached }) {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setSelectedIds(new Set());
    listDocuments({ documentStatus: "ACTIVE", page: 0, size: PAGE_SIZE }, controller.signal)
      .then((page) => {
        if (controller.signal.aborted) return;
        setDocuments(page?.content || []);
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message || "Failed to load documents");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [open]);

  const attachable = documents.filter(
    (d) => !alreadyAttachedDocumentIds?.includes(d.id) && d.versionStatus === "READY"
  );

  const toggle = (id) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const submit = async () => {
    if (selectedIds.size === 0 || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      const details = await Promise.all([...selectedIds].map((id) => getDocument(id)));
      const documentVersionIds = details
        .map((d) => d?.currentVersion?.versionId)
        .filter((id) => id != null);
      await attachDocuments(conversationId, documentVersionIds);
      onAttached();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to attach documents. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Attach documents" maxWidth="max-w-lg">
      {loading ? (
        <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : attachable.length === 0 ? (
        <p className="text-sm text-text-muted py-6 text-center">
          No ready documents available to attach.
        </p>
      ) : (
        <ul className="space-y-1.5 max-h-[50vh] overflow-y-auto">
          {attachable.map((doc) => (
            <li key={doc.id}>
              <label className="flex items-center gap-3 rounded-lg border border-border-subtle p-3 cursor-pointer hover:bg-bg-elevated transition-colors">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-accent"
                  checked={selectedIds.has(doc.id)}
                  onChange={() => toggle(doc.id)}
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-text-primary">{doc.title}</p>
                  <p className="text-xs text-text-muted">{documentTypeLabel(doc.documentType)}</p>
                </div>
              </label>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="text-sm text-error mt-3">{error}</p>}

      <div className="flex items-center justify-end gap-3 pt-4">
        <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
          Cancel
        </button>
        <button
          type="button"
          className="btn-primary text-sm"
          onClick={submit}
          disabled={selectedIds.size === 0 || submitting}
        >
          {submitting ? "Attaching…" : `Attach (${selectedIds.size})`}
        </button>
      </div>
    </Modal>
  );
}
