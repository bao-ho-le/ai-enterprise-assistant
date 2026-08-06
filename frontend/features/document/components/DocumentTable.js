"use client";

import { useEffect, useRef, useState } from "react";
import { FileText, ListTree, MessagesSquare, Trash2, Loader2, Inbox, AlertCircle, FolderPlus, Upload } from "lucide-react";
import DocumentRow from "./DocumentRow";
import FolderRow, { ParentFolderRow } from "./FolderRow";
import { gridTemplateColumns } from "./documentTableGrid";

// Loading/error/empty states span the full row width — they don't need to
// line up with any column, so no grid/gridTemplateColumns involved here.
function StateRow({ children }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-4 py-16 text-center text-text-muted">
      {children}
    </div>
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
  const columns = gridTemplateColumns(showSemanticColumn);

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

  // The header row now lives outside the scrolling body (see the two blocks
  // below) instead of being a sticky part of it, so nothing physically inside
  // the scroll area can drag it — that's what actually guarantees 0px of
  // header movement while scrolling (sticky's compositor-timed repaint could
  // still show a 1-2px flicker on fast/inertial scroll even when its computed
  // position was exactly correct).
  //
  // The tradeoff: the body's vertical scrollbar (when the list is tall enough
  // to need one) eats into ITS width but not the header's, since they're two
  // separate boxes now — without correction the header's columns would sit a
  // few pixels wider than the body's. This measures the real scrollbar width
  // (0 on systems with overlay scrollbars, e.g. macOS default) and reserves
  // the same gap on the header, kept in sync via ResizeObserver so it also
  // reacts when infinite scroll crosses the "now tall enough to scroll"
  // threshold, not just on window resize.
  const [scrollbarWidth, setScrollbarWidth] = useState(0);
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const measure = () => setScrollbarWidth(el.offsetWidth - el.clientWidth);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    // flex-1 min-h-0: fills whatever height FileStorageView's flex column leaves
    // after the search bar and breadcrumb row, instead of growing with row
    // count — the body below (flex-1 min-h-0) fills whatever's left below the
    // toolbar and header and scrolls its own rows (and loads more) internally.
    // No .card class: the table blends into the page's outer background instead of
    // sitting on a separate container background, so the container, toolbar, header
    // and rows all paint the same bg-bg-primary.
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border-subtle bg-bg-primary" role="table">
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

      {/* Header — a plain static row, not part of the scroll area at all, so it
          cannot move during scroll by construction. One border/background for
          the whole row is enough now (no more per-cell background — that was
          only ever needed so a *sticky* header wouldn't show scrolled rows
          bleeding through the gaps between cells, which can't happen once the
          header isn't overlapping scrolled content in the first place). */}
      <div
        role="row"
        className="grid shrink-0 border-b border-border-default bg-bg-primary"
        style={{ gridTemplateColumns: columns, paddingRight: scrollbarWidth }}
      >
        <div role="columnheader" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
          File Name
        </div>
        {/* Fixed-width selection column: reserves a small spot next to the
            checkbox for the selection count so columns never shift when rows
            are selected or deselected. */}
        <div role="columnheader" className="flex items-center gap-1.5 px-4 py-3 text-left">
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
        <div role="columnheader" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
          Upload Time
        </div>
        {/* whitespace-nowrap keeps the header on a single line; the
            column width is fixed (see documentTableGrid.js) rather than
            auto-sizing to it. */}
        <div className="whitespace-nowrap px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary" role="columnheader">
          Document Type
        </div>
        <div role="columnheader" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
          Extension
        </div>
        <div role="columnheader" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
          Size
        </div>
        {showSemanticColumn && (
          <div className="whitespace-nowrap px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary" role="columnheader">
            Match
          </div>
        )}
        <div role="columnheader" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-primary">
          Processing
        </div>
        <div role="columnheader" className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-primary">
          Actions
        </div>
      </div>

      {/* min-h-0 overflow-auto: this is the only part of the card that scrolls
          (both axes — vertical for row count, horizontal same as before for the
          wide semantic search table).
          overscroll-y-contain: without it, once this list hits its scroll boundary
          (top or bottom), the browser chains the rest of the wheel/trackpad gesture
          to the next scrollable ancestor (the root layout's page wrapper) — which
          then drags this whole component. `contain` stops the chain at this
          boundary. */}
      <div ref={scrollRef} role="rowgroup" className="min-h-0 flex-1 overflow-auto overscroll-y-contain">
        {loading && (
          <StateRow>
            <Loader2 className="h-6 w-6 animate-spin" />
            <p className="text-sm">Loading documents…</p>
          </StateRow>
        )}

        {!loading && error && (
          <StateRow>
            <AlertCircle className="h-6 w-6 text-error" />
            <p className="text-sm text-error">{error}</p>
          </StateRow>
        )}

        {!loading && !error && showParentRow && <ParentFolderRow onOpen={onOpenParentFolder} />}

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
          <StateRow>
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
          <StateRow>
            <Loader2 className="h-4 w-4 animate-spin" />
          </StateRow>
        )}

        {/* Scroll sentinel — invisible, just gives the IntersectionObserver
            something to watch right after the last row. */}
        {!loading && !error && <div aria-hidden="true" ref={sentinelRef} className="h-px" />}
      </div>
    </div>
  );
}
