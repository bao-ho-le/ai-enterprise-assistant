"use client";

import { Loader2, FileOutput } from "lucide-react";
import Modal from "@/components/ui/Modal";
import LoadMore from "@/components/ui/LoadMore";
import { formatDateTime } from "@/utils/format";

// Completed runs only — a PENDING/RUNNING/FAILED run has no content to open.
export default function GenerationHistoryModal({
  open,
  onClose,
  items,
  loading,
  hasMore,
  loadingMore,
  onLoadMore,
  onSelect,
}) {
  const completed = (items || []).filter(
    (i) => i.status === "COMPLETED" && i.generatedContentId != null
  );

  return (
    <Modal open={open} onClose={onClose} title="Generation history">
      {loading ? (
        <div className="flex items-center justify-center gap-2 py-8 text-text-muted">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : completed.length === 0 ? (
        <p className="text-sm text-text-muted">No completed generation yet.</p>
      ) : (
        <ul className="space-y-2 max-h-[60vh] overflow-y-auto">
          {completed.map((item) => (
            <li key={item.generationId}>
              <button
                type="button"
                onClick={() => onSelect(item.generatedContentId)}
                className="card flex w-full items-center gap-3 p-3 text-left transition-colors hover:border-border-default"
              >
                <FileOutput className="h-4 w-4 shrink-0 text-text-secondary" />
                <span className="min-w-0 flex-1 truncate text-sm text-text-primary">
                  {formatDateTime(item.createdAt)}
                </span>
                <span className="badge badge-success shrink-0">COMPLETED</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <LoadMore hasMore={hasMore} loading={loadingMore} onClick={onLoadMore} />
    </Modal>
  );
}
