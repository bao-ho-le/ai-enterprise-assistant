"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Loader2, FileOutput, AlertCircle, Inbox } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import { getGeneratedContents } from "@/services/generatedContentService";
import { GENERATED_DOCUMENT_TYPES, generatedDocumentTypeLabel } from "@/constants/generatedContent";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 20;

export default function GeneratedContentListView() {
  const [generatedType, setGeneratedType] = useState("");
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const loadPage = useCallback(
    (pageToLoad, append) => {
      const setLoadingState = append ? setLoadingMore : setLoading;
      setLoadingState(true);
      setError("");
      return getGeneratedContents({
        generatedType: generatedType || undefined,
        page: pageToLoad,
        size: PAGE_SIZE,
      })
        .then((slice) => {
          setItems((prev) => (append ? [...prev, ...(slice?.content || [])] : slice?.content || []));
          setHasMore(Boolean(slice?.hasNext));
          setPage(pageToLoad);
        })
        .catch((err) => setError(err.message || "Failed to load generated content"))
        .finally(() => setLoadingState(false));
    },
    [generatedType]
  );

  useEffect(() => {
    loadPage(0, false);
  }, [loadPage]);

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-lg font-semibold text-text-primary">Generated Content</h1>
        <div className="flex items-center gap-2">
          <label htmlFor="generated-type-filter" className="text-xs text-text-muted whitespace-nowrap">
            Type
          </label>
          <select
            id="generated-type-filter"
            className="select-field"
            value={generatedType}
            onChange={(e) => setGeneratedType(e.target.value)}
          >
            <option value="">All</option>
            {GENERATED_DOCUMENT_TYPES.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-text-muted">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-sm">Loading…</span>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center gap-2 py-16 text-error">
          <AlertCircle className="h-6 w-6" />
          <p className="text-sm">{error}</p>
        </div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-2 py-16 text-text-muted">
          <Inbox className="h-6 w-6" />
          <p className="text-sm">No generated content yet.</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {items.map((item) => (
            <li key={item.id}>
              <Link
                href={`/generated-contents/${item.id}`}
                className="card flex items-center gap-3 p-4 hover:border-border-default transition-colors"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-bg-elevated">
                  <FileOutput className="h-4 w-4 text-text-secondary" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-text-primary">{item.title}</p>
                  <p className="text-xs text-text-muted">{generatedDocumentTypeLabel(item.generatedType)}</p>
                </div>
                <span className="text-xs text-text-muted shrink-0">{formatDateTime(item.updatedAt)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <LoadMore hasMore={hasMore} loading={loadingMore} onClick={() => loadPage(page + 1, true)} />
    </main>
  );
}
