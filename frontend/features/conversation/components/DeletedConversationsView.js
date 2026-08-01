"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, RotateCcw, Trash2 } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import Toast from "@/components/ui/Toast";
import ConversationTypeFilter from "./ConversationTypeFilter";
import {
  getDeletedConversations,
  restoreConversation,
} from "@/services/conversationService";
import { conversationTypeLabel } from "@/constants/conversation";
import { formatDateTime } from "@/utils/format";

const PAGE_SIZE = 10;

// Rows aren't navigable (every detail endpoint resolves conversations by status = ACTIVE,
// so a soft-deleted one has no detail page to open) — the only row action is Restore.
export default function DeletedConversationsView() {
  const [items, setItems] = useState([]);
  const [selectedType, setSelectedType] = useState("");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [restoringId, setRestoringId] = useState(null);
  const [toast, setToast] = useState(null);

  const notify = useCallback((type, text) => setToast({ type, text }), []);

  const onRestore = async (c) => {
    setRestoringId(c.id);
    try {
      await restoreConversation(c.id);
      // Drop the row locally and nudge the active list (LeftSidebar) to refresh so
      // the conversation reappears there without a full page reload.
      setItems((prev) => prev.filter((x) => x.id !== c.id));
      window.dispatchEvent(new CustomEvent("conversation-restored", { detail: { id: c.id } }));
      notify("success", "Conversation restored");
    } catch (err) {
      notify("error", err.message || "Failed to restore conversation");
    } finally {
      setRestoringId(null);
    }
  };

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

        {/* .select-field is width:100% (globals.css, unlayered) so a width utility on
            the select itself is silently overridden — constrain via a fixed wrapper,
            matching the File Storage Document Type filter pattern. */}
        <div className="w-44 max-w-full">
          <label htmlFor="deleted-conversation-type" className="label-text">
            Conversation Type
          </label>
          <ConversationTypeFilter
            id="deleted-conversation-type"
            value={selectedType}
            onChange={setSelectedType}
            allowAll
            className="select-field"
          />
        </div>

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
            {items.map((c) => {
              const restoring = restoringId === c.id;
              return (
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
                  <button
                    type="button"
                    className="btn-secondary text-sm shrink-0"
                    disabled={restoringId != null}
                    onClick={() => onRestore(c)}
                  >
                    {restoring ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <RotateCcw className="h-4 w-4" />
                    )}
                    Restore
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        <LoadMore hasMore={hasMore} loading={loadingMore} onClick={() => load(page + 1, true)} />
      </div>

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
