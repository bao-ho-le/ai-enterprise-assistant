"use client";

import { Loader2 } from "lucide-react";

// For Spring Slice<T> pagination: no totalElements/totalPages, only hasNext().
export default function LoadMore({ hasMore, loading, onClick, label = "Load more" }) {
  if (!hasMore) return null;

  return (
    <div className="flex justify-center py-4">
      <button type="button" className="btn-secondary text-sm" onClick={onClick} disabled={loading}>
        {loading ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" />
            Loading…
          </>
        ) : (
          label
        )}
      </button>
    </div>
  );
}
