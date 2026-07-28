"use client";

import { useEffect, useState } from "react";
import { Loader2, FileText } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { getMessageDetail } from "@/services/messageService";

// MessageSourceResponse carries no chunk text (only documentTitle/pageNumber/score) —
// unlike EvidenceDialog's semantic-search matches, there's no excerpt to render here.
export default function MessageSourcesDialog({ open, onClose, conversationId, messageId }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !messageId) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getMessageDetail(conversationId, messageId, controller.signal)
      .then((d) => {
        if (!controller.signal.aborted) setDetail(d);
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message || "Failed to load sources");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [open, conversationId, messageId]);

  return (
    <Modal open={open} onClose={onClose} title="Sources" maxWidth="max-w-lg">
      {loading ? (
        <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : error ? (
        <p className="text-sm text-error">{error}</p>
      ) : !detail?.sources || detail.sources.length === 0 ? (
        <p className="text-sm text-text-muted">No sources cited for this message.</p>
      ) : (
        <ul className="space-y-2 max-h-[60vh] overflow-y-auto">
          {detail.sources.map((s) => (
            <li key={s.chunkId} className="flex items-center gap-3 rounded-lg border border-border-subtle p-3">
              <FileText className="h-4 w-4 text-text-muted shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-text-primary">{s.documentTitle}</p>
                <p className="text-xs text-text-muted">{s.pageNumber ? `Page ${s.pageNumber}` : "Page —"}</p>
              </div>
              {s.score != null && (
                <span className="badge badge-success shrink-0">{Math.round(s.score * 100)}% match</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
