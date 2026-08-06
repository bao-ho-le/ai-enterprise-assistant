"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FileText,
  FileSpreadsheet,
  File as FileIcon,
  ScanSearch,
  Download,
  Upload,
  Pencil,
  Trash2,
  RotateCcw,
  FolderOpen,
} from "lucide-react";
import RowActionsMenu from "./RowActionsMenu";
import { documentTypeLabel, versionStatusBadge } from "@/constants/document";
import { formatBytes, formatDateTime } from "@/utils/format";
import { hrefForFolderId } from "@/utils/folderPath";
import { gridTemplateColumns } from "./documentTableGrid";

// Match dot colour per score band. The two middle bands are a linear RGB gradient
// between the endpoints --success (#22c55e) and --warning (#eab308): t=1/3 ->
// #65bf41, t=2/3 -> #a7b925. Below 65% is unreachable on screen —
// qdrant.score-threshold=0.65 drops those hits server-side — so it falls back to
// the muted neutral instead of getting a band of its own.
function matchDotColor(percent) {
  if (percent >= 80) return "var(--success)";
  if (percent >= 75) return "#65bf41";
  if (percent >= 70) return "#a7b925";
  if (percent >= 65) return "var(--warning)";
  return "var(--text-muted)";
}

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

function DocumentActionsMenu({ doc, onUploadVersion, onEdit, onDelete, onRestore }) {
  const isDeleted = doc.documentStatus === "DELETED";

  return (
    <RowActionsMenu itemCount={isDeleted ? 2 : 4}>
      {(close) => {
        const run = (fn) => () => {
          close();
          fn(doc);
        };
        return (
          <>
            <Link
              href={`/file-storage/${doc.id}`}
              className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
            >
              <FileText className="h-4 w-4 text-text-muted" />
              Document Details
            </Link>
            {isDeleted ? (
              <button
                type="button"
                onClick={run(onRestore)}
                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
              >
                <RotateCcw className="h-4 w-4 text-text-muted" />
                Restore Document
              </button>
            ) : (
              <>
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
              </>
            )}
          </>
        );
      }}
    </RowActionsMenu>
  );
}

export default function DocumentRow({
  doc,
  showSemanticColumn,
  selected,
  onToggle,
  onDownload,
  onUploadVersion,
  onEdit,
  onDelete,
  onRestore,
  onViewEvidence,
}) {
  const processing = versionStatusBadge(doc.versionStatus);
  const hasMatch = doc.semanticScore !== null && doc.semanticScore !== undefined;
  const similarityPercent = hasMatch ? Math.round(doc.semanticScore * 100) : 0;
  const { Icon: ExtIcon, bg: iconBg, color: iconColor } = extensionIcon(doc.extension);
  const router = useRouter();

  return (
    // Clicking anywhere on the row opens the document detail page; the checkbox and
    // Actions cells stop the click so selecting/acting doesn't navigate away.
    //
    // grid, not <tr>: the header row lives outside the scroll container (see
    // DocumentTable.js), so there's no shared <table> left to align columns —
    // gridTemplateColumns is the same shared value the header uses instead.
    // Every cell carries its own border-b (that part is unchanged from the old
    // border-separate table) plus flex/items-center to replace the vertical
    // centering a <td> gave for free.
    <div
      role="row"
      className="grid bg-bg-primary cursor-pointer transition-colors hover:bg-bg-elevated/50"
      style={{ gridTemplateColumns: gridTemplateColumns(showSemanticColumn) }}
      onClick={() => router.push(`/file-storage/${doc.id}`)}
    >
      <div role="cell" className="min-w-0 flex items-center gap-3 border-b border-border-default px-4 py-1">
        <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${iconBg}`}>
          <ExtIcon className={`h-3.5 w-3.5 ${iconColor}`} />
        </span>
        {/* No max-width cap: the File Name column expands to fill the table,
            so the filename only truncates when it genuinely exceeds the
            available width. */}
        <div className="min-w-0">
          <Link
            href={`/file-storage/${doc.id}`}
            className="block truncate text-xs font-medium text-text-primary hover:text-accent transition-colors"
            title={doc.title}
          >
            {doc.title}
          </Link>
        </div>
      </div>
      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1"
        onClick={(e) => e.stopPropagation()}
      >
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
          checked={selected}
          onChange={() => onToggle(doc.id)}
          aria-label={`Select ${doc.title}`}
        />
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap"
      >
        {formatDateTime(doc.uploadTime)}
      </div>

      {/* uppercase is display-only, the same utility the Extension cell already
          uses — the stored DocumentType enum value is untouched. */}
      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap uppercase"
      >
        {documentTypeLabel(doc.documentType)}
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary uppercase"
      >
        {doc.extension || "—"}
      </div>

      <div
        role="cell"
        className="flex items-center border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap"
      >
        {formatBytes(doc.size)}
      </div>

      {showSemanticColumn && (
        <div role="cell" className="flex items-center border-b border-border-default px-4 py-1 text-xs">
          {hasMatch ? (
            <div className="flex items-center gap-1.5">
              {/* Fixed width + right-aligned + tabular-nums: "71%" and "99%" render at
                  slightly different widths otherwise (proportional digit spacing), which
                  shifts the dot that follows. Locking the text column's width keeps the
                  dot's position constant regardless of the digits. */}
              <span className="w-7 shrink-0 text-right text-xs tabular-nums text-text-secondary whitespace-nowrap">
                {similarityPercent}%
              </span>
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: matchDotColor(similarityPercent) }}
                aria-hidden="true"
              />
            </div>
          ) : (
            <span className="text-text-muted">—</span>
          )}
        </div>
      )}

      <div role="cell" className="flex items-center border-b border-border-default px-4 py-1">
        <span className={`badge ${processing.badge}`}>{processing.label}</span>
      </div>

      {/* Search results only carry the two actions that make sense on a match:
          inspect the evidence, or jump to where the document lives. */}
      <div
        role="cell"
        className="flex items-center justify-end gap-1 border-b border-border-default px-4 py-1"
        onClick={(e) => e.stopPropagation()}
      >
        {showSemanticColumn ? (
          <>
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="View matching chunks"
              title="View matching chunks"
              disabled={!hasMatch}
              onClick={() => onViewEvidence(doc)}
            >
              <ScanSearch className="h-4 w-4" />
            </button>
            <Link
              href={hrefForFolderId(doc.folderId)}
              className="btn-ghost p-1.5"
              aria-label="Go to containing folder"
              title="Go to containing folder"
            >
              <FolderOpen className="h-4 w-4" />
            </Link>
          </>
        ) : (
          <>
            <button
              type="button"
              className="btn-ghost p-1.5"
              aria-label="Download"
              onClick={() => onDownload(doc)}
            >
              <Download className="h-4 w-4" />
            </button>
            <DocumentActionsMenu
              doc={doc}
              onUploadVersion={onUploadVersion}
              onEdit={onEdit}
              onDelete={onDelete}
              onRestore={onRestore}
            />
          </>
        )}
      </div>
    </div>
  );
}
