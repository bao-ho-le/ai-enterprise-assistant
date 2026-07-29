"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, Trash2 } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import ConversationTypeFilter from "./ConversationTypeFilter";
import { getDeletedConversations } from "@/services/conversationService";
import { conversationTypeLabel } from "@/constants/conversation";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 10;

// Read-only on purpose: every detail endpoint resolves conversations by status = ACTIVE,
// so a soft-deleted one has no detail page to open. Rows are not clickable.
export default function DeletedConversationsView() {
  const [items, setItems] = useState([]);
  const [selectedType, setSelectedType] = useState("");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(
    (nextPage, append, signal) => {
      const loadingSetter = append ? setLoadingMore : setLoading;
      loadingSetter(true);
      if (!append) setError("");
      return getDeletedConversations(
        { conversationType: selectedType || undefined, page: nextPage, size: PAGE_SIZE },
        signal
      )
        .then((slice) => {
          if (signal?.aborted) return;
          setItems((prev) => (append ? [...prev, ...(slice?.content || [])] : slice?.content || []));
          setHasMore(Boolean(slice?.hasNext));
          setPage(nextPage);
        })
        .catch((err) => {
          if (signal?.aborted || err.name === "AbortError") return;
          setError(err.message || "Failed to load deleted conversations");
        })
        .finally(() => {
          if (!signal?.aborted) loadingSetter(false);
        });
    },
    [selectedType]
  );

  useEffect(() => {
    const controller = new AbortController();
    load(0, false, controller.signal);
    return () => controller.abort();
  }, [load]);

  return (
    <main className="flex-1 overflow-y-auto">
      <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 space-y-6">
        <div>
          <h1 className="text-lg font-semibold text-text-primary">Deleted conversations</h1>
          <p className="text-sm text-text-muted mt-1">
            Conversations you removed. They are kept on the server but can no longer be opened.
          </p>
        </div>

        <ConversationTypeFilter
          value={selectedType}
          onChange={setSelectedType}
          allowAll
          className="select-field w-56"
          ariaLabel="Filter deleted conversations by type"
        />

        {loading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-text-muted">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <p className="text-sm text-error">{error}</p>
        ) : items.length === 0 ? (
          <p className="text-sm text-text-muted">No deleted conversations.</p>
        ) : (
          <ul className="space-y-2">
            {items.map((c) => (
              <li key={c.id} className="card flex items-center gap-3 p-3">
                <Trash2 className="h-4 w-4 shrink-0 text-text-muted" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-text-primary" title={c.title}>
                    {c.title}
                  </p>
                  <p className="text-xs text-text-muted">
                    {conversationTypeLabel(c.conversationType)} · Deleted {formatDateTime(c.deletedAt)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}

        <LoadMore hasMore={hasMore} loading={loadingMore} onClick={() => load(page + 1, true)} />
      </div>
    </main>
  );
}
