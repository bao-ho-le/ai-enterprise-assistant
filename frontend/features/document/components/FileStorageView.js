"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import SearchBar from "./SearchBar";
import FilterPopover from "./FilterPopover";
import DocumentTable from "./DocumentTable";
import FolderBreadcrumb from "./FolderBreadcrumb";
import FolderNameModal from "./FolderNameModal";
import UploadDocumentModal from "./UploadDocumentModal";
import UploadVersionModal from "./UploadVersionModal";
import EditMetadataModal from "./EditMetadataModal";
import ConfirmDialog from "./ConfirmDialog";
import EvidenceDialog from "./EvidenceDialog";
import Toast from "@/components/ui/Toast";
import { useDebounce } from "@/hooks/useDebounce";
import { useSemanticSearch } from "@/hooks/useSemanticSearch";
import { dateInputToIso } from "@/utils/format";
import { folderSegment, hrefForBreadcrumb, hrefForSegments } from "@/utils/folderPath";
import { useRouter, useSearchParams } from "next/navigation";
import {
  listDocuments,
  getDocument,
  deleteDocument,
  restoreDocument,
  downloadCurrentVersion,
} from "@/services/documentService";
import { deleteFolder, getFolderContents } from "@/services/folderService";

// Document page size for infinite scroll — folders are no longer paginated,
// they always come back in full (see FolderServiceImpl.getFolderContents).
const PAGE_SIZE = 20;
const SEMANTIC_TOP_K = 50;
// ponytail: search scans the first 200 documents (matching current filters)
// client-side rather than a server-side "search within these IDs" endpoint —
// raise this or push the intersection server-side if a tenant outgrows it.
const SEARCH_SCAN_SIZE = 200;

const INITIAL_FILTERS = {
  sort: "newest",
  documentType: "",
  extension: "",
  fromDate: "",
  toDate: "",
  status: "",
  documentStatus: "ACTIVE",
};

// Folder browsing shows one folder's contents; searching and filtering span every
// folder, so any active filter drops back to the flat document list (the folder
// contents endpoint supports neither filters nor deleted documents).
function hasActiveFilters(filters) {
  return (
    filters.sort !== INITIAL_FILTERS.sort ||
    filters.documentStatus !== INITIAL_FILTERS.documentStatus ||
    Boolean(
      filters.documentType ||
        filters.extension ||
        filters.fromDate ||
        filters.toDate ||
        filters.status
    )
  );
}

// folderPath / folderId come from the URL (/file-storage = root, /file-storage/folders/…
// = a specific folder), so browser Back/Forward moves between folders instead of
// leaving the page, and a pasted folder URL loads that folder directly.
export default function FileStorageView({ folderPath = [], folderId = null }) {
  // Deep link from the conversation "deleted attachment" warning: /file-storage?status=DELETED
  // pre-applies the Status filter so the table opens already showing deleted documents.
  const searchParams = useSearchParams();
  const initialDocumentStatus = searchParams.get("status") === "DELETED" ? "DELETED" : "ACTIVE";

  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const trimmedKeyword = debouncedKeyword.trim();
  const isSearching = trimmedKeyword.length > 0;

  const [filters, setFilters] = useState({ ...INITIAL_FILTERS, documentStatus: initialDocumentStatus });

  const [folderView, setFolderView] = useState(null);
  const folderMode = !isSearching && !hasActiveFilters(filters);

  // Accumulated document list — infinite scroll appends pages here instead of
  // replacing the current page. totalElements comes from the backend Page and
  // stays fixed for the life of this view (folder/filter/sort/search combo).
  const [documents, setDocuments] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  // abortRef holds the controller for the current view's requests (initial +
  // load-more), so switching folder/filter/sort/search cancels stale ones.
  // pageRef/loadingMoreRef are refs (not state) because loadMoreDocuments needs
  // synchronous re-entrancy guarding and the next-page index right away.
  const abortRef = useRef(null);
  const pageRef = useRef(0);
  const loadingMoreRef = useRef(false);

  const [selectedIds, setSelectedIds] = useState(new Set());
  const [selectedFolderIds, setSelectedFolderIds] = useState(new Set());
  const [toast, setToast] = useState(null);

  const [folderNameTarget, setFolderNameTarget] = useState(null); // folder | { create: true }
  const [deleteFolderTarget, setDeleteFolderTarget] = useState(null);
  const [deletingFolder, setDeletingFolder] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [versionTarget, setVersionTarget] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null); // doc | { bulk: true }
  const [deleting, setDeleting] = useState(false);
  const [restoreTarget, setRestoreTarget] = useState(null);
  const [restoring, setRestoring] = useState(false);
  const [evidenceTarget, setEvidenceTarget] = useState(null);
  const [navigating, setNavigating] = useState(false);

  const router = useRouter();

  const notify = useCallback((type, text) => setToast({ type, text }), []);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  // Semantic search — replaces the old LIKE-based `keyword` filter entirely.
  const {
    results: semanticHits,
    loading: searchLoading,
    error: searchError,
  } = useSemanticSearch(isSearching ? trimmedKeyword : "", SEMANTIC_TOP_K);

  // documentId -> { bestScore, matches: SemanticSearchResult[] } (matches stay
  // score-sorted since semanticHits is already score-sorted by the backend).
  const semanticByDocument = useMemo(() => {
    const map = new Map();
    for (const hit of semanticHits) {
      const entry = map.get(hit.documentId);
      if (!entry) {
        map.set(hit.documentId, { bestScore: hit.score, matches: [hit] });
      } else {
        entry.matches.push(hit);
        if (hit.score > entry.bestScore) entry.bestScore = hit.score;
      }
    }
    return map;
  }, [semanticHits]);

  // Builds the query params for /documents, shared between the initial fetch
  // and each subsequent infinite-scroll page so they stay in lockstep.
  const buildListParams = useCallback(
    (pageIndex, size) => ({
      sort: filters.sort || undefined,
      documentType: filters.documentType || undefined,
      extension: filters.extension || undefined,
      fromDate: dateInputToIso(filters.fromDate, false),
      toDate: dateInputToIso(filters.toDate, true),
      status: filters.status || undefined,
      documentStatus: filters.documentStatus || undefined,
      page: pageIndex,
      size,
    }),
    [filters]
  );

  // Resets the view: fetches the folder (full, once) and/or the first document
  // page. Changing folder/filters/sort/search re-runs this and its cleanup
  // aborts whatever the previous view still had in flight (including a
  // load-more request), so a late response can't append into the new view.
  useEffect(() => {
    const controller = new AbortController();
    abortRef.current = controller;
    pageRef.current = 0;

    setDocuments([]);
    setTotalElements(0);
    setLoading(true);
    setError("");

    const request = folderMode
      ? getFolderContents(folderId, { page: 0, size: PAGE_SIZE }, controller.signal).then((contents) => {
          if (controller.signal.aborted) return;
          setFolderView(contents || null);
          setDocuments(contents?.documents?.content || []);
          setTotalElements(contents?.documents?.totalElements || 0);
          pageRef.current = 1;
        })
      : listDocuments(
          buildListParams(0, isSearching ? SEARCH_SCAN_SIZE : PAGE_SIZE),
          controller.signal
        ).then((pageData) => {
          if (controller.signal.aborted) return;
          setFolderView(null);
          setDocuments(pageData?.content || []);
          setTotalElements(pageData?.totalElements || 0);
          pageRef.current = 1;
        });

    request
      .then(() => {
        if (!controller.signal.aborted) {
          setSelectedIds(new Set());
          setSelectedFolderIds(new Set());
        }
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load documents");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [filters, reloadKey, isSearching, folderMode, folderId, buildListParams]);

  // Fetches the next document page and appends it — called by DocumentTable's
  // scroll sentinel. Guarded by a ref (not state) so a fast double-trigger from
  // the IntersectionObserver can't fire two requests before state catches up.
  const loadMoreDocuments = useCallback(async () => {
    if (loadingMoreRef.current) return;
    const controller = abortRef.current;
    if (!controller) return;
    const pageToFetch = pageRef.current;

    loadingMoreRef.current = true;
    setLoadingMore(true);
    try {
      if (folderMode) {
        const contents = await getFolderContents(
          folderId,
          { page: pageToFetch, size: PAGE_SIZE },
          controller.signal
        );
        if (controller.signal.aborted) return;
        setDocuments((prev) => [...prev, ...(contents?.documents?.content || [])]);
      } else {
        const pageData = await listDocuments(buildListParams(pageToFetch, PAGE_SIZE), controller.signal);
        if (controller.signal.aborted) return;
        setDocuments((prev) => [...prev, ...(pageData?.content || [])]);
      }
      pageRef.current = pageToFetch + 1;
    } catch (err) {
      if (!controller.signal.aborted && err.name !== "AbortError") {
        notify("error", err.message || "Failed to load more documents");
      }
    } finally {
      loadingMoreRef.current = false;
      if (!controller.signal.aborted) setLoadingMore(false);
    }
  }, [folderMode, folderId, buildListParams, notify]);

  // A folder can be reached by id alone (the "go to folder" action on a search
  // result); once the breadcrumb arrives, rewrite the URL to its full path so the
  // address bar and the breadcrumb always agree. replace, not push — this is the
  // same location, not a navigation.
  useEffect(() => {
    if (!folderView?.breadcrumb?.length) return;
    const canonical = hrefForBreadcrumb(folderView.breadcrumb);
    if (canonical !== window.location.pathname) router.replace(canonical, { scroll: false });
  }, [folderView, router]);

  // While searching, the accumulated list is replaced by semantic matches,
  // ranked by best score. Search already fetches its full bounded scan
  // (SEARCH_SCAN_SIZE) in one shot, so there's nothing left to load more of.
  const matchedDocuments = useMemo(() => {
    if (!isSearching) return null;
    return documents
      .filter((doc) => semanticByDocument.has(doc.id))
      .map((doc) => ({ ...doc, semanticScore: semanticByDocument.get(doc.id).bestScore }))
      .sort((a, b) => b.semanticScore - a.semanticScore);
  }, [isSearching, documents, semanticByDocument]);

  const tableDocuments = isSearching ? matchedDocuments : documents;
  const tableTotalElements = isSearching ? matchedDocuments.length : totalElements;
  const hasMoreDocuments = tableDocuments.length < tableTotalElements;

  // Called by the filter popover only when the user commits (Apply Filters /
  // Clear All) — the draft is edited inside the popover, filtering logic and
  // request building below are untouched.
  const handleApplyFilters = (nextFilters) => {
    setFilters(nextFilters);
  };
  const onKeywordChange = (value) => {
    setKeyword(value);
  };

  // router.push, not local state: each folder gets a real history entry, so Back
  // returns to the parent folder instead of leaving File Storage entirely.
  const openFolder = (folder) => router.push(hrefForSegments([...folderPath, folderSegment(folder)]));
  const openParentFolder = () => router.push(hrefForSegments(folderPath.slice(0, -1)));

  const confirmDeleteFolder = async () => {
    setDeletingFolder(true);
    try {
      await deleteFolder(deleteFolderTarget.id);
      notify("success", "Folder deleted");
      setDeleteFolderTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Delete failed");
    } finally {
      setDeletingFolder(false);
    }
  };

  const toggleRow = (id) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const toggleFolderRow = (id) =>
    setSelectedFolderIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  // Folders and documents share one "select all" checkbox — everything currently
  // in the table (both types) toggles together.
  const toggleAll = () => {
    const subfolders = folderView?.subfolders || [];
    const allSelected =
      (tableDocuments.length > 0 || subfolders.length > 0) &&
      tableDocuments.every((d) => selectedIds.has(d.id)) &&
      subfolders.every((f) => selectedFolderIds.has(f.id));
    if (allSelected) {
      setSelectedIds(new Set());
      setSelectedFolderIds(new Set());
    } else {
      setSelectedIds(new Set(tableDocuments.map((d) => d.id)));
      setSelectedFolderIds(new Set(subfolders.map((f) => f.id)));
    }
  };

  const onDownload = async (doc) => {
    try {
      await downloadCurrentVersion(doc.id);
    } catch (err) {
      notify("error", err.message || "Download failed");
    }
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      if (deleteTarget?.bulk) {
        await Promise.all([
          ...[...selectedIds].map((id) => deleteDocument(id)),
          ...[...selectedFolderIds].map((id) => deleteFolder(id)),
        ]);
        notify("success", `Deleted ${selectedIds.size + selectedFolderIds.size} item(s)`);
      } else {
        await deleteDocument(deleteTarget.id);
        notify("success", "Document deleted");
      }
      setDeleteTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  const confirmRestore = async () => {
    setRestoring(true);
    try {
      await restoreDocument(restoreTarget.id);
      notify("success", "Document restored");
      setRestoreTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Restore failed");
    } finally {
      setRestoring(false);
    }
  };

  const navigateToFeature = useCallback(
    async (href) => {
      const ids = [...selectedIds];
      if (ids.length === 0) return;
      setNavigating(true);
      try {
        // Resolve current version for each selected document (same pattern as AttachDocumentsModal).
        const details = await Promise.all(ids.map((id) => getDocument(id)));
        const attachDocs = ids
          .map((id, i) => {
            const versionId = details[i]?.currentVersion?.versionId;
            if (versionId == null) return null;
            const listItem = documents.find((doc) => doc.id === id);
            return {
              documentId: id,
              documentVersionId: versionId,
              documentTitle: listItem?.title,
              versionNumber: details[i].currentVersion.versionNumber,
            };
          })
          .filter(Boolean);

        if (attachDocs.length === 0) {
          notify("error", "Could not resolve document versions. Please try again.");
          return;
        }

        sessionStorage.setItem("file-storage-attach", JSON.stringify(attachDocs));
        router.push(href);
      } catch (err) {
        notify("error", err.message || "Failed to prepare documents. Please try again.");
      } finally {
        setNavigating(false);
      }
    },
    [selectedIds, documents, notify, router]
  );

  return (
    // flex flex-col + min-h-0 + overflow-hidden: this box is pinned to exactly the
    // height the root layout leaves below the nav bar, instead of growing with row
    // count — DocumentTable (flex-1 min-h-0) then fills whatever's left below the
    // search bar and breadcrumb row and scrolls its own rows (and loads more) internally.
    // overscroll-contain: defense in depth alongside DocumentTable's own
    // overscroll-y-contain — if this box ever ends up a sub-pixel taller than the
    // root layout's scroll wrapper (font metrics, scrollbar reflow, zoom), this stops
    // that wrapper from chaining a scroll into itself and dragging this whole page
    // section (sticky table header included) instead of clipping the excess.
    <main className="mx-auto flex min-h-0 w-full max-w-[1440px] flex-1 flex-col overflow-hidden overscroll-contain px-4 pt-2 pb-8 sm:px-6 lg:px-8">
      <div className="mb-5 shrink-0">
        <SearchBar
          value={keyword}
          onChange={onKeywordChange}
          leftSlot={
            <FilterPopover
              filters={filters}
              defaults={INITIAL_FILTERS}
              active={hasActiveFilters(filters)}
              onApply={handleApplyFilters}
            />
          }
        />
      </div>

      {folderView && (
        <div className="mb-4 min-w-0 shrink-0">
          <FolderBreadcrumb breadcrumb={folderView.breadcrumb} />
        </div>
      )}

      <DocumentTable
        documents={tableDocuments}
        hasMore={hasMoreDocuments}
        loadingMore={loadingMore}
        onLoadMore={loadMoreDocuments}
        showSemanticColumn={isSearching}
        folderMode={folderMode}
        onCreateFolder={() => setFolderNameTarget({ create: true })}
        onUploadClick={() => setUploadOpen(true)}
        folders={folderView?.subfolders || []}
        showParentRow={folderMode && folderPath.length > 0}
        onOpenParentFolder={openParentFolder}
        onOpenFolder={openFolder}
        onRenameFolder={(folder) => setFolderNameTarget(folder)}
        onDeleteFolder={(folder) => setDeleteFolderTarget(folder)}
        loading={loading || (isSearching && searchLoading)}
        error={error || (isSearching ? searchError : "")}
        selectedIds={selectedIds}
        onToggleRow={toggleRow}
        selectedFolderIds={selectedFolderIds}
        onToggleFolderRow={toggleFolderRow}
        onToggleAll={toggleAll}
        onDownload={onDownload}
        onUploadVersion={(doc) => setVersionTarget(doc)}
        onEdit={(doc) => setEditTarget(doc)}
        onDelete={(doc) => setDeleteTarget(doc)}
        onRestore={(doc) => setRestoreTarget(doc)}
        onBulkDelete={() => setDeleteTarget({ bulk: true })}
        onViewEvidence={(doc) => setEvidenceTarget(doc)}
        disabled={navigating}
        onNavigateToWriteReport={() => navigateToFeature("/write-report")}
        onNavigateToSummary={() => navigateToFeature("/summary")}
        onNavigateToDocumentQA={() => navigateToFeature("/document-qa")}
      />

      <FolderNameModal
        open={Boolean(folderNameTarget)}
        onClose={() => setFolderNameTarget(null)}
        folder={folderNameTarget?.create ? null : folderNameTarget}
        parentFolderId={folderView?.currentFolder?.id}
        onSaved={(isRename) => {
          notify("success", isRename ? "Folder renamed" : "Folder created");
          reload();
        }}
      />

      <UploadDocumentModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        folderId={folderView?.currentFolder?.id}
        onUploaded={(count) => {
          notify("success", count > 1 ? `Uploaded ${count} document(s)` : "Document uploaded");
          reload();
        }}
      />

      <UploadVersionModal
        open={Boolean(versionTarget)}
        onClose={() => setVersionTarget(null)}
        documentId={versionTarget?.id}
        title={versionTarget?.title}
        onUploaded={() => {
          notify("success", "New version uploaded");
          reload();
        }}
      />

      <EditMetadataModal
        open={Boolean(editTarget)}
        onClose={() => setEditTarget(null)}
        documentId={editTarget?.id}
        onSaved={() => {
          notify("success", "Metadata updated");
          reload();
        }}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title={deleteTarget?.bulk ? "Delete selected items" : "Delete document"}
        confirmLabel="Delete"
        message={
          deleteTarget?.bulk
            ? `Delete ${selectedIds.size + selectedFolderIds.size} selected item(s)? Any selected folders are deleted along with everything inside them. This cannot be undone.`
            : `Delete "${deleteTarget?.title}"? This cannot be undone.`
        }
      />

      <ConfirmDialog
        open={Boolean(deleteFolderTarget)}
        onClose={() => setDeleteFolderTarget(null)}
        onConfirm={confirmDeleteFolder}
        loading={deletingFolder}
        title="Delete folder"
        confirmLabel="Delete"
        message={`Delete "${deleteFolderTarget?.name}"? Everything inside it is moved to the trash too.`}
      />

      <ConfirmDialog
        open={Boolean(restoreTarget)}
        onClose={() => setRestoreTarget(null)}
        onConfirm={confirmRestore}
        loading={restoring}
        tone="default"
        title="Restore document"
        confirmLabel="Restore"
        loadingLabel="Restoring…"
        message={`Restore "${restoreTarget?.title}"? It will become active again.`}
      />

      <EvidenceDialog
        open={Boolean(evidenceTarget)}
        onClose={() => setEvidenceTarget(null)}
        doc={evidenceTarget}
        matches={evidenceTarget ? semanticByDocument.get(evidenceTarget.id)?.matches : null}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
