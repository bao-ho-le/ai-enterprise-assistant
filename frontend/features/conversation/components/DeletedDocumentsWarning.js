"use client";

import { AlertTriangle, ChevronRight } from "lucide-react";
import { useRouter } from "next/navigation";

// Shown on Document QA / Report / Summary conversations when an attached document has
// been soft-deleted since being attached. Clicking jumps to File Storage pre-filtered
// to deleted documents so the user can restore it.
export default function DeletedDocumentsWarning() {
  const router = useRouter();

  return (
    <button
      type="button"
      onClick={() => router.push("/file-storage?status=DELETED")}
      className="inline-flex items-center gap-3 rounded-lg border border-warning/30 bg-warning/10 px-4 py-2.5 text-left transition-colors hover:bg-warning/15"
    >
      <span className="flex items-center gap-2 text-sm text-warning">
        <AlertTriangle className="h-4 w-4 shrink-0" />
        Tài liệu đính kèm đã bị xoá, cần khôi phục để tiếp tục sử dụng
      </span>
      <ChevronRight className="h-4 w-4 shrink-0 text-warning" />
    </button>
  );
}
