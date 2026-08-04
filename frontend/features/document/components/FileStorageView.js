"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Upload } from "lucide-react";
import SearchBar from "./SearchBar";
import FilterToolbar from "./FilterToolbar";
import DocumentTable from "./DocumentTable";
import UploadDocumentModal from "./UploadDocumentModal";
import UploadVersionModal from "./UploadVersionModal";
import EditMetadataModal from "./EditMetadataModal";
import ConfirmDialog from "./ConfirmDialog";
import EvidenceDialog from "./EvidenceDialog";
import Toast from "@/components/ui/Toast";
import { useDebounce } from "@/hooks/useDebounce";
import { useSemanticSearch } from "@/hooks/useSemanticSearch";
import { dateInputToIso } from "@/utils/format";
import { matchesSimilarityBucket } from "@/constants/document";
import { useRouter, useSearchParams } from "next/navigation";
import {
  listDocuments,
  getDocument,
  deleteDocument,
  restoreDocument,
  downloadCurrentVersion,
} from "@/services/documentService";

const PAGE_SIZE = 10;
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
  similarity: "",
};

export default function FileStorageView() {
  // Deep link from the conversation "deleted attachment" warning: /file-storage?status=DELETED
  // pre-applies the Status filter so the table opens already showing deleted documents.
  const searchParams = useSearchParams();
  const initialDocumentStatus = searchParams.get("status") === "DELETED" ? "DELETED" : "ACTIVE";

  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const trimmedKeyword = debouncedKeyword.trim();
  const isSearching = trimmedKeyword.length > 0;

  const [filters, setFilters] = useState({ ...INITIAL_FILTERS, documentStatus: initialDocumentStatus });
  const [page, setPage] = useState(0);

  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0, number: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  const [selectedIds, setSelectedIds] = useState(new Set());
  const [toast, setToast] = useState(null);

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

  useEffect(() => {
    const controller = new AbortController();
    const params = {
      sort: filters.sort || undefined,
      documentType: filters.documentType || undefined,
      extension: filters.extension || undefined,
      fromDate: dateInputToIso(filters.fromDate, false),
      toDate: dateInputToIso(filters.toDate, true),
      status: filters.status || undefined,
      documentStatus: filters.documentStatus || undefined,
      page: isSearching ? 0 : page,
      size: isSearching ? SEARCH_SCAN_SIZE : PAGE_SIZE,
    };

    setLoading(true);
    setError("");
    listDocuments(params, controller.signal)
      .then((pageData) => {
        if (controller.signal.aborted) return;
        setData(pageData || { content: [], totalElements: 0, totalPages: 0, number: 0 });
        setSelectedIds(new Set());
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load documents");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [filters, page, reloadKey, isSearching]);

  // While searching, the plain paginated list is replaced by semantic matches,
  // ranked by best score and re-paginated client-side.
  const matchedDocuments = useMemo(() => {
    if (!isSearching) return null;
    return data.content
      .filter((doc) => semanticByDocument.has(doc.id))
      .map((doc) => ({ ...doc, semanticScore: semanticByDocument.get(doc.id).bestScore }))
      .filter((doc) => matchesSimilarityBucket(doc.semanticScore, filters.similarity))
      .sort((a, b) => b.semanticScore - a.semanticScore);
  }, [isSearching, data.content, semanticByDocument, filters.similarity]);

  const tableDocuments = isSearching
    ? matchedDocuments.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)
    : data.content;
  const tableTotalElements = isSearching ? matchedDocuments.length : data.totalElements;
  const tableTotalPages = isSearching
    ? Math.max(1, Math.ceil(matchedDocuments.length / PAGE_SIZE))
    : data.totalPages;
  const tableNumber = isSearching ? page : data.number;

  const onFilterChange = (patch) => {
    // EMAIL_TEMPLATE is hidden from the Document Type filter (display-only);
    // ignore any stale value so the select never holds a hidden option.
    if (patch.documentType === "EMAIL_TEMPLATE") patch.documentType = "";
    setFilters((f) => ({ ...f, ...patch }));
    setPage(0);
  };
  const onKeywordChange = (value) => {
    setKeyword(value);
    setPage(0);
  };

  const toggleRow = (id) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const toggleAll = () =>
    setSelectedIds((prev) => {
      const allSelected = tableDocuments.length > 0 && tableDocuments.every((d) => prev.has(d.id));
      return allSelected ? new Set() : new Set(tableDocuments.map((d) => d.id));
    });

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
        await Promise.all([...selectedIds].map((id) => deleteDocument(id)));
        notify("success", `Deleted ${selectedIds.size} document(s)`);
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
            const listItem = data.content.find((doc) => doc.id === id);
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
    [selectedIds, data.content, notify, router]
  );

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-10">
        <SearchBar value={keyword} onChange={onKeywordChange} />
      </div>

      <div className="flex flex-col sm:flex-row sm:items-stretch gap-4 mb-8">
        <div className="flex-1">
          <FilterToolbar filters={filters} onChange={onFilterChange} />
        </div>
        <div className="flex items-center">
          <button
            type="button"
            className="btn-upload-file-file-storage shrink-0"
            onClick={() => setUploadOpen(true)}
          >
            <Upload className="h-4 w-4" />
            Upload New Document
          </button>
        </div>
      </div>

      <DocumentTable
        documents={tableDocuments}
        loading={loading || (isSearching && searchLoading)}
        error={error || (isSearching ? searchError : "")}
        selectedIds={selectedIds}
        onToggleRow={toggleRow}
        onToggleAll={toggleAll}
        number={tableNumber}
        totalPages={tableTotalPages}
        totalElements={tableTotalElements}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
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

      <UploadDocumentModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
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
        title="Delete document"
        confirmLabel="Delete"
        message={
          deleteTarget?.bulk
            ? `Delete ${selectedIds.size} selected document(s)? This cannot be undone.`
            : `Delete "${deleteTarget?.title}"? This cannot be undone.`
        }
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
