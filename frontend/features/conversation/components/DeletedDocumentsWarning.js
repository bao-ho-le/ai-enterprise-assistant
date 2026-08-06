"use client";

import { AlertTriangle, ChevronRight } from "lucide-react";
import { useRouter } from "next/navigation";

// Shown on Document QA / Report / Summary conversations when an attached document has
// been soft-deleted since being attached. Clicking jumps to the Trash page with that
// document highlighted so the user can find and restore it without hunting for it.
//
// documentIds only needs to be "this conversation's attached documents" — Trash only
// ever lists documents that are actually deleted, so passing an active document's id
// here is harmless, it simply won't match anything there.
export default function DeletedDocumentsWarning({ documentIds = [] }) {
  const router = useRouter();
  const highlight = documentIds.filter((id) => id != null).join(",");
  const href = highlight ? `/file-storage/trash?highlight=${highlight}` : "/file-storage/trash";

  return (
    <button
      type="button"
      onClick={() => router.push(href)}
      className="inline-flex items-center gap-3 rounded-lg border border-warning/30 bg-warning/10 px-4 py-2.5 text-left transition-colors hover:bg-warning/15"
    >
      <span className="flex items-center gap-2 text-sm text-warning">
        <AlertTriangle className="h-4 w-4 shrink-0" />
        An attached document has been deleted — restore it to continue using it
      </span>
      <ChevronRight className="h-4 w-4 shrink-0 text-warning" />
    </button>
  );
}
