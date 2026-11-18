"use client";

import { useCallback, useState } from "react";
import { FileText, Plus, X } from "lucide-react";
import FolderBrowserPicker from "@/features/document/components/FolderBrowserPicker";
import { getDocument } from "@/services/documentService";

// Source documents for the generation modes (Report / Summary).
//
// Interaction: the browser stays on screen but short (~4 rows, expandable). Ticking rows
// only stages them — nothing is attached until "Add files" is pressed, which resolves the
// current version of each document and hands it to the composer. After adding, the ticks
// are cleared, so removing a document (chip ✕) and adding it again works the same way
// every time. Staged documents are only persisted when the request is submitted.
export default function SelectedDocumentsSection({
  documents = [],
  onPick,
  onRemove,
  onClear,
  title = "Source documents",
  description,
  required = false,
}) {
  const [expanded, setExpanded] = useState(false);
  const [pendingIds, setPendingIds] = useState([]);
  const [adding, setAdding] = useState(false);
  const [pickError, setPickError] = useState("");
  // Bumped after every successful add so the browser starts with an empty selection.
  const [selectionResetKey, setSelectionResetKey] = useState(0);

  const selectedDocumentIds = documents.map((doc) => doc.documentId);

  const handleSelectionChange = useCallback((ids) => {
    setPendingIds(ids);
    setPickError("");
  }, []);

  const addSelected = useCallback(async () => {
    if (adding || pendingIds.length === 0) return;
    const fresh = pendingIds.filter((id) => !selectedDocumentIds.includes(id));
    if (fresh.length === 0) {
      setSelectionResetKey((key) => key + 1);
      setPendingIds([]);
      return;
    }

    setAdding(true);
    setPickError("");
    try {
      const details = await Promise.all(fresh.map((id) => getDocument(id)));
      const picked = fresh
        .map((id, index) => {
          const detail = details[index];
          const versionId = detail?.currentVersion?.versionId;
          if (versionId == null) return null;
          return {
            documentId: id,
            documentVersionId: versionId,
            documentTitle: detail?.documentInfo?.title,
            versionNumber: detail.currentVersion.versionNumber,
          };
        })
        .filter(Boolean);

      if (picked.length === 0) {
        setPickError("Those documents have no ready version yet.");
      } else {
        onPick?.(picked);
      }
      setSelectionResetKey((key) => key + 1);
      setPendingIds([]);
    } catch (err) {
      setPickError(err?.message || "Failed to load the selected documents.");
    } finally {
      setAdding(false);
    }
  }, [adding, pendingIds, selectedDocumentIds, onPick]);

  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="flex flex-wrap items-baseline gap-2 text-sm font-semibold text-text-primary">
            <span>
              {title}
              {required && <span className="text-error"> *</span>}
            </span>
            <span className="text-xs font-normal text-text-muted" aria-live="polite">
              {documents.length > 0 ? `${documents.length} selected` : "None selected"}
            </span>
          </h3>
          {description && <p className="mt-0.5 text-xs leading-relaxed text-text-muted">{description}</p>}
        </div>

        {documents.length > 0 && (
          <button type="button" className="btn-ghost text-xs" onClick={() => onClear?.()}>
            Clear all
          </button>
        )}
      </div>

      {documents.length > 0 ? (
        <ul className="flex flex-wrap gap-1.5">
          {documents.map((doc) => (
            <li key={doc.documentVersionId}>
              <span className="inline-flex max-w-[280px] items-center gap-1.5 rounded-md border border-border-subtle bg-bg-elevated py-1 pl-2 pr-1 text-xs text-text-secondary">
                <FileText className="h-3.5 w-3.5 shrink-0 text-text-muted" />
                <span className="truncate" title={doc.documentTitle}>
                  {doc.documentTitle || "Untitled document"}
                </span>
                <span className="shrink-0 text-[10px] text-text-muted">v{doc.versionNumber}</span>
                <button
                  type="button"
                  className="rounded p-0.5 text-text-muted transition-colors hover:bg-bg-secondary hover:text-text-primary"
                  aria-label={`Remove ${doc.documentTitle || "document"}`}
                  onClick={() => onRemove?.(doc)}
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-xs text-text-muted">No documents selected yet — pick one from the list below.</p>
      )}

      <FolderBrowserPicker
        active
        mode="attach"
        hideRootBreadcrumb
        selectionResetKey={selectionResetKey}
        listHeightClass={expanded ? "h-72" : "h-[150px]"}
        documentFilter={(doc) => !selectedDocumentIds.includes(doc.id) && doc.versionStatus === "READY"}
        onSelectionChange={handleSelectionChange}
      />

      <div className="flex flex-wrap items-center justify-between gap-2">
        <button
          type="button"
          className="btn-ghost text-xs"
          onClick={() => setExpanded((previous) => !previous)}
          aria-expanded={expanded}
        >
          {expanded ? "Show fewer" : "Show more"}
        </button>

        <div className="flex items-center gap-2">
          {adding && <span className="text-[11px] text-text-muted">Adding…</span>}
          <button
            type="button"
            className="btn-secondary text-xs"
            onClick={addSelected}
            disabled={pendingIds.length === 0 || adding}
          >
            <Plus className="h-3.5 w-3.5" />
            Add files{pendingIds.length > 0 ? ` (${pendingIds.length})` : ""}
          </button>
        </div>
      </div>

      {pickError && <p className="text-xs text-error">{pickError}</p>}
    </section>
  );
}
