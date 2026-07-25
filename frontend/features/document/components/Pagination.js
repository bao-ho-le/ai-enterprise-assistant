import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ number, totalPages, totalElements, shown, onPrev, onNext, itemLabel = "documents" }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-border-subtle bg-bg-secondary">
      <p className="text-sm text-text-muted">
        Showing {shown} of {totalElements} {itemLabel}
      </p>
      <div className="flex items-center gap-2">
        <button
          type="button"
          className="btn-ghost p-2"
          aria-label="Previous page"
          disabled={number <= 0}
          onClick={onPrev}
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <span className="text-sm text-text-secondary px-2">
          Page {totalPages === 0 ? 0 : number + 1} of {totalPages}
        </span>
        <button
          type="button"
          className="btn-ghost p-2"
          aria-label="Next page"
          disabled={number >= totalPages - 1}
          onClick={onNext}
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
