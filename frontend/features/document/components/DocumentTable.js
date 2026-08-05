"use client";

import { useEffect, useRef } from "react";
import { FileText, ListTree, MessagesSquare, Trash2, Loader2, Inbox, AlertCircle, FolderPlus, Upload } from "lucide-react";
import DocumentRow from "./DocumentRow";
import FolderRow, { ParentFolderRow } from "./FolderRow";

function StateRow({ children, colSpan }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-4 py-16">
        <div className="flex flex-col items-center justify-center gap-3 text-center text-text-muted">
          {children}
        </div>
      </td>
    </tr>
  );
}

export default function DocumentTable({
  documents,
  // Infinite scroll: hasMore/loadingMore/onLoadMore drive the sentinel below the
  // last row — no more Prev/Next, no page count.
  hasMore = false,
  loadingMore = false,
  onLoadMore,
  // Semantic Similarity column only makes sense while a semantic search is active.
  showSemanticColumn = false,
  // Folder browsing (omitted while searching/filtering, which spans all folders):
  // subfolders of the folder currently open, plus the ".." row when it has a parent.
  folders = [],
  showParentRow = false,
  onOpenParentFolder,
  onOpenFolder,
  onRenameFolder,
  onDeleteFolder,
  folderMode,
  onCreateFolder,
  onUploadClick,
  loading,
  error,
  selectedIds,
  onToggleRow,
  selectedFolderIds,
  onToggleFolderRow,
  onToggleAll,
  onDownload,
  onUploadVersion,
  onEdit,
  onDelete,
  onRestore,
  onBulkDelete,
  onViewEvidence,
  disabled,
  onNavigateToWriteReport,
  onNavigateToSummary,
  onNavigateToDocumentQA,
}) {
  // "Select all" and the header count cover both rows types — folders and
  // documents share one checkbox column, so one row of either type counts the
  // same toward "everything is selected".
  const selectedCount = selectedIds.size + selectedFolderIds.size;
  const hasFolderSelected = selectedFolderIds.size > 0;
  const allSelected =
    (documents.length > 0 || folders.length > 0) &&
    documents.every((d) => selectedIds.has(d.id)) &&
    folders.every((f) => selectedFolderIds.has(f.id));
  const colSpan = showSemanticColumn ? 9 : 8;

  const scrollRef = useRef(null);
  const sentinelRef = useRef(null);

  // Watches the sentinel row against the table's own scroll container (root),
  // not the window — the container scrolls internally, the page doesn't. Keyed
  // on documents.length so a sentinel still in view after a page is appended
  // (short list, tall viewport) re-triggers immediately instead of waiting for
  // a scroll event that may never come.
  useEffect(() => {
    if (!hasMore) return;
    const root = scrollRef.current;
    const sentinel = sentinelRef.current;
    if (!root || !sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) onLoadMore?.();
      },
      { root, rootMargin: "200px" }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, onLoadMore, documents.length]);

  return (
    // flex-1 min-h-0: fills whatever height FileStorageView's flex column leaves
    // after the search bar and breadcrumb row, instead of growing with row count —
    // the row area below scrolls internally (and loads more on scroll) so the
    // card's bottom edge always sits at the bottom of the screen.
    // No .card class: the table blends into the page's outer background instead of
    // sitting on a separate container background, so the container, toolbar, header
    // and rows all paint the same bg-bg-primary.
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary">
      {/* Selection toolbar — always visible so the layout doesn't jump on select */}
      <div className="flex shrink-0 items-center justify-between gap-4 px-4 py-3 border-b border-border-subtle bg-bg-primary">
        <div className="flex items-center gap-2">
          <button type="button" className="btn-primary py-2 px-3 text-sm" onClick={onUploadClick}>
            <Upload className="h-4 w-4" />
            Upload New Document
          </button>
          {folderMode && (
            <button type="button" className="btn-secondary py-2 px-3 text-sm" onClick={onCreateFolder}>
              <FolderPlus className="h-4 w-4" />
              New Folder
            </button>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onNavigateToWriteReport}
            disabled={disabled || selectedCount === 0 || hasFolderSelected}
            className={`btn-secondary py-2 px-3 text-sm ${disabled || selectedCount === 0 || hasFolderSelected ? "opacity-50" : ""}`}
          >
            <FileText className="h-4 w-4" />
            Write Report
          </button>
          <button
            type="button"
            onClick={onNavigateToSummary}
            disabled={disabled || selectedCount === 0 || hasFolderSelected}
            className={`btn-secondary py-2 px-3 text-sm ${disabled || selectedCount === 0 || hasFolderSelected ? "opacity-50" : ""}`}
          >
            <ListTree className="h-4 w-4" />
            Summary
          </button>
          <button
            type="button"
            onClick={onNavigateToDocumentQA}
            disabled={disabled || selectedCount === 0 || hasFolderSelected}
            className={`btn-secondary py-2 px-3 text-sm ${disabled || selectedCount === 0 || hasFolderSelected ? "opacity-50" : ""}`}
          >
            <MessagesSquare className="h-4 w-4" />
            Document QA
          </button>
          <button
            type="button"
            onClick={onBulkDelete}
            disabled={selectedCount === 0}
            className="btn-secondary py-2 px-3 text-sm text-error border-error/30 hover:bg-error/10 disabled:opacity-50 disabled:pointer-events-none"
          >
            <Trash2 className="h-4 w-4" />
            Delete
          </button>
        </div>
      </div>

      {/* min-h-0 overflow-auto: this is the only part of the card that scrolls
          (both axes — vertical for row count, horizontal same as before for the
          wide semantic search table). The header stays put via sticky, anchored
          to this container instead of the viewport.
          overscroll-y-contain: without it, once this list hits its scroll boundary
          (top or bottom), the browser chains the rest of the wheel/trackpad gesture
          to the next scrollable ancestor (the root layout's page wrapper) — which
          then drags this whole component, sticky header included, since sticky is
          only pinned relative to ITS OWN container, not the page. That's what reads
          as the header "jittering" or moving: the container is sliding, not the
          header un-sticking. `contain` stops the chain at this boundary. */}
      <div ref={scrollRef} className="min-h-0 flex-1 overflow-auto overscroll-y-contain">
        {/* border-separate (not border-collapse) is required so the sticky header
            can paint per-cell backgrounds without bleed-through. Its side effect:
            in the separated-borders model, <tr> borders are IGNORED — so both the
            header divider and every row divider are drawn by the cells themselves
            (border-b on each <th>/<td> in DocumentRow/FolderRow), never the <tr>. */}
        <table className="w-full border-separate border-spacing-0">
          {/* sticky + background on every <th> individually, not the <tr>/<thead> —
              a sticky thead with the background only on its row leaves a hairline
              gap at the top edge that scrolled rows bleed through as they pass
              underneath. Each cell painting its own background closes that gap. */}
          <thead>
            <tr>
              {/* w-full on File Name makes it absorb all remaining width, so the
                  other columns size themselves to their content and sit close
                  together instead of stretching across the table. */}
              <th className="sticky top-0 z-10 w-full border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                File Name
              </th>
              {/* Fixed-width selection column: reserves a small spot next to the
                  checkbox for the selection count so columns never shift when rows
                  are selected or deselected. */}
              <th className="sticky top-0 z-10 w-20 border-b border-border-default bg-bg-primary px-4 py-3 text-left">
                <div className="flex items-center gap-1.5">
                  <input
                    type="checkbox"
                    className="h-4 w-4 shrink-0 rounded border-border-default bg-bg-primary accent-accent"
                    aria-label="Select all"
                    checked={allSelected}
                    onChange={onToggleAll}
                  />
                  <span className="w-6 text-xs font-medium tabular-nums text-text-secondary">
                    {selectedCount > 0 ? selectedCount : ""}
                  </span>
                </div>
              </th>
              <th className="sticky top-0 z-10 border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                Upload Time
              </th>
              {/* whitespace-nowrap keeps the header on a single line; the
                  column auto-sizes to fit it. */}
              <th className="sticky top-0 z-10 whitespace-nowrap border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                Document Type
              </th>
              <th className="sticky top-0 z-10 border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                Extension
              </th>
              <th className="sticky top-0 z-10 border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                Size
              </th>
              {showSemanticColumn && (
                <th className="sticky top-0 z-10 whitespace-nowrap border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                  Match
                </th>
              )}
              <th className="sticky top-0 z-10 border-b border-border-default bg-bg-primary px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
                Processing
              </th>
              <th className="sticky top-0 z-10 border-b border-border-default bg-bg-primary px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-primary">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <StateRow colSpan={colSpan}>
                <Loader2 className="h-6 w-6 animate-spin" />
                <p className="text-sm">Loading documents…</p>
              </StateRow>
            )}

            {!loading && error && (
              <StateRow colSpan={colSpan}>
                <AlertCircle className="h-6 w-6 text-error" />
                <p className="text-sm text-error">{error}</p>
              </StateRow>
            )}

            {!loading && !error && showParentRow && (
              <ParentFolderRow onOpen={onOpenParentFolder} />
            )}

            {!loading &&
              !error &&
              folders.map((folder) => (
                <FolderRow
                  key={`folder-${folder.id}`}
                  folder={folder}
                  onOpen={onOpenFolder}
                  onRename={onRenameFolder}
                  onDelete={onDeleteFolder}
                  selected={selectedFolderIds.has(folder.id)}
                  onToggle={onToggleFolderRow}
                />
              ))}

            {!loading && !error && documents.length === 0 && folders.length === 0 && (
              <StateRow colSpan={colSpan}>
                <Inbox className="h-6 w-6" />
                <p className="text-sm">No documents found</p>
              </StateRow>
            )}

            {!loading &&
              !error &&
              documents.map((doc) => (
                <DocumentRow
                  key={doc.id}
                  doc={doc}
                  showSemanticColumn={showSemanticColumn}
                  selected={selectedIds.has(doc.id)}
                  onToggle={onToggleRow}
                  onDownload={onDownload}
                  onUploadVersion={onUploadVersion}
                  onEdit={onEdit}
                  onDelete={onDelete}
                  onRestore={onRestore}
                  onViewEvidence={onViewEvidence}
                />
              ))}

            {!loading && !error && loadingMore && (
              <StateRow colSpan={colSpan}>
                <Loader2 className="h-4 w-4 animate-spin" />
              </StateRow>
            )}

            {/* Scroll sentinel — invisible, just gives the IntersectionObserver
                something to watch right after the last row. */}
            {!loading && !error && (
              <tr aria-hidden="true">
                <td colSpan={colSpan} className="p-0">
                  <div ref={sentinelRef} className="h-px" />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
