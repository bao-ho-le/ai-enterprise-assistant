"use client";

import Link from "next/link";
import { FileText, ListTree, MessagesSquare, Trash2, Loader2, Inbox, AlertCircle } from "lucide-react";
import DocumentRow from "./DocumentRow";
import Pagination from "./Pagination";

const HEADERS = [
  "File Name",
  "Upload Time",
  "Extension",
  "Size",
  "Semantic Similarity",
  "Processing",
  "Status",
  "Actions",
];

function StateRow({ children }) {
  return (
    <tr>
      <td colSpan={9} className="px-4 py-16">
        <div className="flex flex-col items-center justify-center gap-3 text-center text-text-muted">
          {children}
        </div>
      </td>
    </tr>
  );
}

export default function DocumentTable({
  documents,
  loading,
  error,
  selectedIds,
  onToggleRow,
  onToggleAll,
  number,
  totalPages,
  totalElements,
  onPrev,
  onNext,
  onDownload,
  onUploadVersion,
  onEdit,
  onDelete,
  onBulkDelete,
}) {
  const selectedCount = selectedIds.size;
  const allSelected = documents.length > 0 && documents.every((d) => selectedIds.has(d.id));

  return (
    <div className="card overflow-hidden">
      {/* Selection toolbar — always visible so the layout doesn't jump on select */}
      <div className="flex items-center justify-between gap-4 px-4 py-3 border-b border-border-subtle">
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-text-primary">
            {selectedCount > 0 ? `${selectedCount} file${selectedCount > 1 ? "s" : ""} selected` : "No files selected"}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href="/write-report"
            aria-disabled={selectedCount === 0}
            className={`btn-secondary py-2 px-3 text-sm ${selectedCount === 0 ? "pointer-events-none opacity-50" : ""}`}
          >
            <FileText className="h-4 w-4" />
            Write Report
          </Link>
          <Link
            href="/summary"
            aria-disabled={selectedCount === 0}
            className={`btn-secondary py-2 px-3 text-sm ${selectedCount === 0 ? "pointer-events-none opacity-50" : ""}`}
          >
            <ListTree className="h-4 w-4" />
            Summary
          </Link>
          <Link
            href="/document-qa"
            aria-disabled={selectedCount === 0}
            className={`btn-secondary py-2 px-3 text-sm ${selectedCount === 0 ? "pointer-events-none opacity-50" : ""}`}
          >
            <MessagesSquare className="h-4 w-4" />
            Document QA
          </Link>
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

      <div className="overflow-x-auto">
        <table className="w-full min-w-[1250px]">
          <thead>
            <tr className="border-b border-border-subtle bg-bg-secondary">
              <th className="px-4 py-3 text-left w-10">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
                  aria-label="Select all files"
                  checked={allSelected}
                  onChange={onToggleAll}
                />
              </th>
              {HEADERS.map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
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

            {!loading && !error && documents.length === 0 && (
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
                  selected={selectedIds.has(doc.id)}
                  onToggle={onToggleRow}
                  onDownload={onDownload}
                  onUploadVersion={onUploadVersion}
                  onEdit={onEdit}
                  onDelete={onDelete}
                />
              ))}
          </tbody>
        </table>
      </div>

      <Pagination
        number={number}
        totalPages={totalPages}
        totalElements={totalElements}
        shown={documents.length}
        onPrev={onPrev}
        onNext={onNext}
      />
    </div>
  );
}
