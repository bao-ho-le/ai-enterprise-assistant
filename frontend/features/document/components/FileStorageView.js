"use client";

import { useCallback, useEffect, useState } from "react";
import { Upload } from "lucide-react";
import SearchBar from "./SearchBar";
import FilterToolbar from "./FilterToolbar";
import DocumentTable from "./DocumentTable";
import UploadDocumentModal from "./UploadDocumentModal";
import UploadVersionModal from "./UploadVersionModal";
import EditMetadataModal from "./EditMetadataModal";
import ConfirmDialog from "./ConfirmDialog";
import Toast from "@/components/ui/Toast";
import { useDebounce } from "@/hooks/useDebounce";
import { dateInputToIso } from "@/utils/format";
import {
  listDocuments,
  deleteDocument,
  downloadCurrentVersion,
} from "@/services/documentService";

const PAGE_SIZE = 10;

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
  const [keyword, setKeyword] = useState("");
  const debouncedKeyword = useDebounce(keyword, 400);
  const [filters, setFilters] = useState(INITIAL_FILTERS);
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

  const notify = useCallback((type, text) => setToast({ type, text }), []);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    const params = {
      keyword: debouncedKeyword.trim() || undefined,
      sort: filters.sort || undefined,
      documentType: filters.documentType || undefined,
      extension: filters.extension || undefined,
      fromDate: dateInputToIso(filters.fromDate, false),
      toDate: dateInputToIso(filters.toDate, true),
      status: filters.status || undefined,
      documentStatus: filters.documentStatus || undefined,
      page,
      size: PAGE_SIZE,
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
  }, [debouncedKeyword, filters, page, reloadKey]);

  const onFilterChange = (patch) => {
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
      const allSelected = data.content.length > 0 && data.content.every((d) => prev.has(d.id));
      return allSelected ? new Set() : new Set(data.content.map((d) => d.id));
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
        documents={data.content}
        loading={loading}
        error={error}
        selectedIds={selectedIds}
        onToggleRow={toggleRow}
        onToggleAll={toggleAll}
        number={data.number}
        totalPages={data.totalPages}
        totalElements={data.totalElements}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
        onDownload={onDownload}
        onUploadVersion={(doc) => setVersionTarget(doc)}
        onEdit={(doc) => setEditTarget(doc)}
        onDelete={(doc) => setDeleteTarget(doc)}
        onBulkDelete={() => setDeleteTarget({ bulk: true })}
      />

      <UploadDocumentModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onUploaded={() => {
          notify("success", "Document uploaded");
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

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
