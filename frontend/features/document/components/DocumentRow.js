"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
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

const MENU_WIDTH = 192; // w-48
// ponytail: assumes the fixed 4-item menu height below; if items become
// dynamic, measure menuRef after mount instead of hardcoding this.
const MENU_HEIGHT = 160;

// Anchors below the button, flipping above it when there isn't enough
// room left at the bottom of the viewport (e.g. the table's last rows).
function computeMenuPosition(buttonRect) {
  const openUpward = window.innerHeight - buttonRect.bottom < MENU_HEIGHT + 8;
  return {
    top: openUpward ? buttonRect.top - MENU_HEIGHT - 4 : buttonRect.bottom + 4,
    left: buttonRect.right - MENU_WIDTH,
  };
}

function RowActionsMenu({ doc, onUploadVersion, onEdit, onDelete }) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef(null);
  const menuRef = useRef(null);

  // Dropdown is portaled to <body> with fixed positioning so it can't get
  // clipped by the table's overflow-x-auto scroll container. On scroll/resize
  // it re-anchors to the button instead of closing (closing would also fire
  // on the scroll-into-view a click itself can trigger).
  useEffect(() => {
    if (!open) return;

    const reposition = () => {
      if (!buttonRef.current) return;
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect()));
    };
    const onOutsideClick = (e) => {
      if (
        !buttonRef.current?.contains(e.target) &&
        !menuRef.current?.contains(e.target)
      ) {
        setOpen(false);
      }
    };

    document.addEventListener("click", onOutsideClick);
    window.addEventListener("scroll", reposition, true);
    window.addEventListener("resize", reposition);
    return () => {
      document.removeEventListener("click", onOutsideClick);
      window.removeEventListener("scroll", reposition, true);
      window.removeEventListener("resize", reposition);
    };
  }, [open]);

  const toggleOpen = () => {
    if (!open && buttonRef.current) {
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect()));
    }
    setOpen((v) => !v);
  };

  const run = (fn) => () => {
    setOpen(false);
    fn(doc);
  };

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        className="btn-ghost p-1.5"
        aria-label="More actions"
        onClick={toggleOpen}
      >
        <MoreHorizontal className="h-4 w-4" />
      </button>
      {open &&
        createPortal(
          <div
            ref={menuRef}
            style={{ position: "fixed", top: position.top, left: position.left, width: MENU_WIDTH }}
            className="rounded-lg border border-border-subtle bg-bg-card shadow-lg z-[200]"
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
          </div>,
          document.body
        )}
    </>
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
  onViewEvidence,
}) {
  const processing = versionStatusBadge(doc.versionStatus);
  const status = documentStatusBadge(doc.documentStatus);
  const hasMatch = doc.semanticScore !== null && doc.semanticScore !== undefined;
  const similarityPercent = hasMatch ? Math.round(doc.semanticScore * 100) : 0;
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
          <div className="min-w-0 max-w-[220px]">
            <Link
              href={`/file-storage/${doc.id}`}
              className="block truncate text-sm font-medium text-text-primary hover:text-accent transition-colors"
              title={doc.title}
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

      <td className="px-4 py-4 text-sm">
        {hasMatch ? (
          <div className="flex items-center gap-2">
            <div className="h-1.5 flex-1 rounded-full bg-bg-elevated overflow-hidden">
              <div
                className="h-full rounded-full bg-accent"
                style={{ width: `${similarityPercent}%` }}
              />
            </div>
            <span className="text-xs text-text-secondary whitespace-nowrap">{similarityPercent}%</span>
          </div>
        ) : (
          <span className="text-text-muted">—</span>
        )}
      </td>

      <td className="px-4 py-4">
        <span className={`badge ${processing.badge}`}>{processing.label}</span>
      </td>

      <td className="px-4 py-4">
        <span className={`badge ${status.badge}`}>{status.label}</span>
      </td>

      <td className="px-4 py-4">
        <div className="flex items-center gap-1">
          {hasMatch && (
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="View matching chunks"
              title="View matching chunks"
              onClick={() => onViewEvidence(doc)}
            >
              <ScanSearch className="h-4 w-4" />
            </button>
          )}
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
