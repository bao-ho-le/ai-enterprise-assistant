"use client";

import { CornerLeftUp, Folder, Pencil, Trash2 } from "lucide-react";
import RowActionsMenu from "./RowActionsMenu";
import { formatDateTime } from "@/utils/format";

// Same "no data" treatment the Semantic Similarity column already uses for documents.
// Every cell carries the row divider (border-b): the table is border-separate, which
// ignores <tr> borders — see DocumentRow's note.
function EmptyCell() {
  return (
    <td className="border-b border-border-default px-4 py-1 text-xs">
      <span className="text-text-muted">—</span>
    </td>
  );
}

// Parent-folder row: navigates to the parent folder on double-click, like a file
// manager. It deliberately has no checkbox and no actions — it isn't a real row
// of data — and every other column is left empty rather than showing a placeholder.
export function ParentFolderRow({ onOpen }) {
  return (
    <tr
      className="bg-bg-primary cursor-pointer select-none transition-colors hover:bg-bg-elevated/50"
      onDoubleClick={onOpen}
      title="Double-click to go to the parent folder"
    >
      <td className="border-b border-border-default px-4 py-1">
        <span className="flex h-6 w-6 items-center justify-center rounded-md bg-amber-500/10">
          <CornerLeftUp className="h-3.5 w-3.5 text-amber-400" />
        </span>
      </td>
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
      <td className="border-b border-border-default px-4 py-1" />
    </tr>
  );
}

// Single-click only selects visually; double-click opens the folder (documents open
// on single click instead, matching how the two behave in Google Drive).
export default function FolderRow({ folder, onOpen, onRename, onDelete, selected, onToggle }) {
  return (
    <tr
      className="bg-bg-primary cursor-pointer select-none transition-colors hover:bg-bg-elevated/50"
      onDoubleClick={() => onOpen(folder)}
      title="Double-click to open"
    >
      <td className="border-b border-border-default px-4 py-1">
        <div className="flex items-center gap-3">
          <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-amber-500/10">
            <Folder className="h-3.5 w-3.5 text-amber-400" />
          </span>
          {/* No max-width cap: the File Name column expands to fill the table,
              so the folder name only truncates when it genuinely exceeds the
              available width. */}
          <div className="min-w-0">
            <p className="truncate text-xs font-medium text-text-primary" title={folder.name}>
              {folder.name}
            </p>
          </div>
        </div>
      </td>
      <td className="border-b border-border-default px-4 py-1" onClick={(e) => e.stopPropagation()} onDoubleClick={(e) => e.stopPropagation()}>
        <input
          type="checkbox"
          className="h-4 w-4 rounded border-border-default bg-bg-primary accent-accent"
          checked={selected}
          onChange={() => onToggle(folder.id)}
          aria-label={`Select ${folder.name}`}
        />
      </td>

      <td className="border-b border-border-default px-4 py-1 text-xs text-text-secondary whitespace-nowrap">
        {formatDateTime(folder.createdAt)}
      </td>

      <td className="border-b border-border-default px-4 py-1 text-xs text-text-secondary uppercase">Folder</td>

      <EmptyCell />
      <EmptyCell />
      <EmptyCell />

      <td className="border-b border-border-default px-4 py-1" onDoubleClick={(e) => e.stopPropagation()}>
        {/* No Download button: there is no folder download endpoint. */}
        <div className="flex items-center justify-end gap-1">
          <RowActionsMenu itemCount={2}>
            {(close) => (
              <>
                <button
                  type="button"
                  onClick={() => {
                    close();
                    onRename(folder);
                  }}
                  className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
                >
                  <Pencil className="h-4 w-4 text-text-muted" />
                  Edit Name
                </button>
                <button
                  type="button"
                  onClick={() => {
                    close();
                    onDelete(folder);
                  }}
                  className="flex items-center gap-2 w-full px-3 py-2 text-sm text-error hover:bg-error/10 transition-colors"
                >
                  <Trash2 className="h-4 w-4" />
                  Delete Folder
                </button>
              </>
            )}
          </RowActionsMenu>
        </div>
      </td>
    </tr>
  );
}
