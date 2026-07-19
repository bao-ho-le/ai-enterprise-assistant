"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import {
  FileText,
  FileSpreadsheet,
  File as FileIcon,
  ScanSearch,
  Download,
  MoreHorizontal,
  Upload,
  Pencil,
  Trash2,
} from "lucide-react";
import { documentTypeLabel, versionStatusBadge, documentStatusBadge } from "@/constants/document";
import { formatBytes, formatDateTime } from "@/utils/format";

// File Name icon + color, picked by extension (PDF red, Word blue, Excel green,
// plain text neutral, anything else falls back to a generic file icon).
const EXTENSION_ICON = {
  pdf: { Icon: FileText, bg: "bg-red-500/10", color: "text-red-400" },
  doc: { Icon: FileText, bg: "bg-blue-500/10", color: "text-blue-400" },
  docx: { Icon: FileText, bg: "bg-blue-500/10", color: "text-blue-400" },
  xls: { Icon: FileSpreadsheet, bg: "bg-green-500/10", color: "text-green-400" },
  xlsx: { Icon: FileSpreadsheet, bg: "bg-green-500/10", color: "text-green-400" },
  txt: { Icon: FileText, bg: "bg-bg-elevated", color: "text-text-secondary" },
};

function extensionIcon(extension) {
  return (
    EXTENSION_ICON[(extension || "").toLowerCase()] || {
      Icon: FileIcon,
      bg: "bg-bg-elevated",
      color: "text-text-secondary",
    }
  );
}

function RowActionsMenu({ doc, onUploadVersion, onEdit, onDelete }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return;
    const onDocClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, [open]);

  const run = (fn) => () => {
    setOpen(false);
    fn(doc);
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        className="btn-ghost p-1.5"
        aria-label="More actions"
        onClick={() => setOpen((v) => !v)}
      >
        <MoreHorizontal className="h-4 w-4" />
      </button>
      <div
        className={`action-dropdown ${
          open ? "visible" : "hidden"
        } absolute right-0 mt-1 w-48 rounded-lg border border-border-subtle bg-bg-card shadow-lg z-50`}
      >
        <div className="py-1">
          <Link
            href={`/file-storage/${doc.id}`}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
          >
            <FileText className="h-4 w-4 text-text-muted" />
            Document Details
          </Link>
          <button
            type="button"
            onClick={run(onUploadVersion)}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
          >
            <Upload className="h-4 w-4 text-text-muted" />
            Upload New Version
          </button>
          <button
            type="button"
            onClick={run(onEdit)}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
          >
            <Pencil className="h-4 w-4 text-text-muted" />
            Edit Metadata
          </button>
          <button
            type="button"
            onClick={run(onDelete)}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-error hover:bg-error/10 transition-colors"
          >
            <Trash2 className="h-4 w-4" />
            Delete Document
          </button>
        </div>
      </div>
    </div>
  );
}

export default function DocumentRow({
  doc,
  selected,
  onToggle,
  onDownload,
  onUploadVersion,
  onEdit,
  onDelete,
}) {
  const processing = versionStatusBadge(doc.versionStatus);
  const status = documentStatusBadge(doc.documentStatus);
  const { Icon: ExtIcon, bg: iconBg, color: iconColor } = extensionIcon(doc.extension);

  return (
    <tr className="border-b border-border-subtle transition-colors hover:bg-bg-elevated/50">
      <td className="px-4 py-4">
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
          checked={selected}
          onChange={() => onToggle(doc.id)}
          aria-label={`Select ${doc.title}`}
        />
      </td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-3">
          <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${iconBg}`}>
            <ExtIcon className={`h-4 w-4 ${iconColor}`} />
          </span>
          <div className="min-w-0">
            <Link
              href={`/file-storage/${doc.id}`}
              className="text-sm font-medium text-text-primary truncate hover:text-accent transition-colors"
            >
              {doc.title}
            </Link>
            <p className="text-xs text-text-muted truncate">
              {documentTypeLabel(doc.documentType)}
            </p>
          </div>
        </div>
      </td>

      <td className="px-4 py-4 text-sm text-text-secondary whitespace-nowrap">
        {formatDateTime(doc.uploadTime)}
      </td>

      <td className="px-4 py-4 text-sm text-text-secondary uppercase">
        {doc.extension || "—"}
      </td>

      <td className="px-4 py-4 text-sm text-text-secondary whitespace-nowrap">
        {formatBytes(doc.size)}
      </td>

      {/* Semantic Similarity — no backend data yet */}
      <td className="px-4 py-4 text-sm text-text-muted">—</td>

      <td className="px-4 py-4">
        <span className={`badge ${processing.badge}`}>{processing.label}</span>
      </td>

      <td className="px-4 py-4">
        <span className={`badge ${status.badge}`}>{status.label}</span>
      </td>

      <td className="px-4 py-4">
        <div className="flex items-center gap-1">
          <Link href={`/file-storage/${doc.id}`} className="btn-ghost p-1.5" title="View details">
            <ScanSearch className="h-4 w-4" />
            <span className="sr-only">View details</span>
          </Link>
          <button
            type="button"
            className="btn-ghost p-1.5"
            aria-label="Download"
            onClick={() => onDownload(doc)}
          >
            <Download className="h-4 w-4" />
          </button>
          <RowActionsMenu
            doc={doc}
            onUploadVersion={onUploadVersion}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>
      </td>
    </tr>
  );
}
