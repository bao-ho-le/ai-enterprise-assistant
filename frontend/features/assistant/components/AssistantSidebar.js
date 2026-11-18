"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ChevronDown, Loader2, Plus, Trash2 } from "lucide-react";
import ConfirmDialog from "@/features/document/components/ConfirmDialog";
import Toast from "@/components/ui/Toast";
import RenameConversationModal from "@/features/conversation/components/RenameConversationModal";
import ConversationRowMenu from "@/features/conversation/components/ConversationRowMenu";
import { getConversations, softDeleteConversation } from "@/services/conversationService";
import { ASSISTANT_MODES } from "@/features/assistant/models/assistantRules";
import { formatConversationTitle } from "@/utils/format";

const PAGE_SIZE = 20;

// Sidebar-only display formatting: backend conversation titles are generated as
// "<Type Name> - 29/07/2026 12:29" (see AIConversationHelper.defaultTitle). The sidebar
// shows the type abbreviation and a "-" between date and time so more rows fit; the
// stored title/API payload are untouched and renamed titles are shown verbatim.
// Longer labels must come before their short forms, so the map order matters.
const SIDEBAR_TITLE_ABBREVIATIONS = {
  "Email Generation": "EG",
  "Report Generation": "RG",
  "Write Report": "RG",
  "Summary Generation": "SG",
  "Summary": "SG",
  "Document QA": "QA",
};

function formatSidebarTitle(title) {
  if (!title) return title;
  let value = title;
  for (const [label, abbr] of Object.entries(SIDEBAR_TITLE_ABBREVIATIONS)) {
    if (value.toLowerCase().startsWith(label.toLowerCase())) {
      value = abbr + value.slice(label.length);
      break;
    }
  }
  return formatConversationTitle(value);
}

const FILTERS = [{ value: "", label: "All" }].concat(
  ASSISTANT_MODES.map((mode) => ({ value: mode.conversationType, label: mode.filterLabel }))
);

// One list for all four conversation types. Filtering stays server-side
// (GET /ai-conversations?conversationType=…, default ACTIVE + pageable), so switching
// filters doesn't page through an unbounded history in the browser.
export default function AssistantSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const activeConversationId = pathname.startsWith("/chat/") ? pathname.slice("/chat/".length).split("/")[0] : null;

  const [filter, setFilter] = useState("");
  const [conversations, setConversations] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  const [renameTarget, setRenameTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState(null);

  const requestRef = useRef(0);
  const notify = useCallback((type, text) => setToast({ type, text }), []);
  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  const loadPage = useCallback(
    async (pageToLoad, append, signal) => {
      const sequence = ++requestRef.current;
      if (append) setLoadingMore(true);
      else setLoading(true);
      setError("");
      try {
        const slice = await getConversations(
          { conversationType: filter || undefined, page: pageToLoad, size: PAGE_SIZE },
          signal
        );
        if (sequence !== requestRef.current || signal?.aborted) return;
        const content = slice?.content || [];
        setConversations((previous) => {
          if (!append) return content;
          const seen = new Set(previous.map((item) => item.id));
          return [...previous, ...content.filter((item) => !seen.has(item.id))];
        });
        setHasMore(Boolean(slice?.hasNext));
        setPage(pageToLoad);
      } catch (err) {
        if (signal?.aborted || err?.name === "AbortError") return;
        if (sequence !== requestRef.current) return;
        setError(err?.message || "Failed to load conversations");
      } finally {
        if (sequence === requestRef.current && !signal?.aborted) {
          setLoading(false);
          setLoadingMore(false);
        }
      }
    },
    [filter]
  );

  // Reload page 0 on filter change, on explicit reloads, and whenever the route changes
  // (a freshly created conversation shows up without a full page reload).
  useEffect(() => {
    const controller = new AbortController();
    loadPage(0, false, controller.signal);
    return () => controller.abort();
  }, [loadPage, reloadKey, pathname]);

  // Conversations restored from the Deleted Conversations screen must reappear here.
  useEffect(() => {
    const onRestored = (event) => {
      if (event.detail?.id != null) reload();
    };
    window.addEventListener("conversation-restored", onRestored);
    return () => window.removeEventListener("conversation-restored", onRestored);
  }, [reload]);

  const onRenamed = (updated) => {
    setRenameTarget(null);
    notify("success", "Conversation renamed");
    reload();
    // The open thread fetched its own copy of the title — nudge it to stay in sync.
    window.dispatchEvent(new CustomEvent("conversation-renamed", { detail: updated }));
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await softDeleteConversation(deleteTarget.id);
      notify("success", "Conversation deleted");
      if (String(deleteTarget.id) === activeConversationId) router.push("/chat");
      setDeleteTarget(null);
      reload();
    } catch (err) {
      notify("error", err?.message || "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <aside className="hidden md:flex w-68 shrink-0 flex-col border-r border-border-subtle bg-bg-primary">
      <div className="flex flex-col gap-0.5 p-2">
        <button
          type="button"
          onClick={() => router.push("/chat")}
          className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
        >
          <Plus className="h-[18px] w-[18px] shrink-0 text-text-secondary" strokeWidth={1.5} />
          <span className="text-sm font-medium text-text-primary">New chat</span>
        </button>

        <Link
          href="/conversations/deleted"
          className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
        >
          <Trash2 className="h-[18px] w-[18px] shrink-0 text-text-secondary" strokeWidth={1.5} />
          <span className="text-sm font-medium text-text-primary">Deleted conversations</span>
        </Link>
      </div>

      <div className="px-2 pb-2">
        <label htmlFor="conversation-type-filter" className="sr-only">
          Filter conversations by type
        </label>
        <div className="relative">
          <select
            id="conversation-type-filter"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            className="w-full cursor-pointer appearance-none rounded-lg border border-border-subtle bg-bg-primary py-1.5 pl-2.5 pr-7 text-xs text-text-primary transition-colors hover:border-border-default focus:border-accent/60 focus:outline-none"
          >
            {FILTERS.map((option) => (
              <option key={option.value || "all"} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <ChevronDown className="pointer-events-none absolute right-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-muted" />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto pt-1 pb-3 px-2">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-8 text-text-muted">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <p className="px-2 text-sm text-error">{error}</p>
        ) : conversations.length === 0 ? (
          <p className="px-2 text-sm text-text-muted">
            {filter ? "No conversations of this type yet." : "No conversations yet."}
          </p>
        ) : (
          <ul className="space-y-0.5">
            {conversations.map((conversation) => {
              const active = String(conversation.id) === activeConversationId;
              return (
                <li key={conversation.id} className="group relative">
                  <Link
                    href={`/chat/${conversation.id}`}
                    className={
                      active
                        ? "flex items-center gap-2 rounded-lg px-2.5 py-2.5 bg-bg-elevated border border-border-subtle transition-colors hover:border-border-default"
                        : "flex items-center gap-2 rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
                    }
                  >
                    <p
                      className={
                        active
                          ? "flex-1 min-w-0 truncate text-sm font-medium text-text-primary tabular-nums"
                          : "flex-1 min-w-0 truncate text-sm text-text-primary tabular-nums"
                      }
                      title={conversation.title}
                    >
                      {formatSidebarTitle(conversation.title)}
                    </p>
                    <span
                      className="flex shrink-0 items-center opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(event) => event.preventDefault()}
                    >
                      <ConversationRowMenu
                        conversation={conversation}
                        onRename={setRenameTarget}
                        onDelete={setDeleteTarget}
                      />
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        )}

        {hasMore && !loading && (
          <div className="flex justify-center py-3">
            <button
              type="button"
              className="btn-secondary text-xs"
              onClick={() => loadPage(page + 1, true)}
              disabled={loadingMore}
            >
              {loadingMore ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
              Load more
            </button>
          </div>
        )}
      </div>

      <RenameConversationModal
        open={Boolean(renameTarget)}
        onClose={() => setRenameTarget(null)}
        conversation={renameTarget}
        onRenamed={onRenamed}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title="Delete conversation"
        confirmLabel="Delete"
        message={`Delete "${deleteTarget?.title}"? You can't undo this from here, but the data isn't permanently erased.`}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </aside>
  );
}
