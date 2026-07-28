"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { SquarePen, Loader2 } from "lucide-react";
import ConfirmDialog from "@/features/document/components/ConfirmDialog";
import Toast from "@/components/ui/Toast";
import CreateConversationModal from "@/features/conversation/components/CreateConversationModal";
import RenameConversationModal from "@/features/conversation/components/RenameConversationModal";
import ConversationRowMenu from "@/features/conversation/components/ConversationRowMenu";
import {
  getConversations,
  softDeleteConversation,
  hardDeleteConversation,
} from "@/services/conversationService";

// conversationType: which ConversationType this sidebar lists (e.g. "DOCUMENT_QA").
// basePath: route prefix conversations of this type live under (e.g. "/document-qa"),
// paired with a "[conversationId]" dynamic route.
export default function LeftSidebar({ conversationType, basePath }) {
  const pathname = usePathname();
  const router = useRouter();
  const activeConversationId = pathname.startsWith(`${basePath}/`)
    ? pathname.slice(basePath.length + 1).split("/")[0]
    : null;

  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const [createOpen, setCreateOpen] = useState(false);
  const [renameTarget, setRenameTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [hardDeleteTarget, setHardDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState(null);
  const notify = useCallback((type, text) => setToast({ type, text }), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getConversations({ conversationType }, controller.signal)
      .then((page) => {
        if (controller.signal.aborted) return;
        setConversations(page?.content || []);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load conversations");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [conversationType, reloadKey]);

  const onCreated = (conversation) => {
    setCreateOpen(false);
    notify("success", "Conversation created");
    reload();
    router.push(`${basePath}/${conversation.id}`);
  };

  const onRenamed = (updated) => {
    setRenameTarget(null);
    notify("success", "Conversation renamed");
    reload();
    // The detail view (main content) fetched its own copy of the title independently —
    // nudge it to stay in sync without wiring up shared state for one field.
    window.dispatchEvent(new CustomEvent("conversation-renamed", { detail: updated }));
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await softDeleteConversation(deleteTarget.id);
      notify("success", "Conversation deleted");
      if (String(deleteTarget.id) === activeConversationId) router.push(basePath);
      setDeleteTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  const confirmHardDelete = async () => {
    setDeleting(true);
    try {
      await hardDeleteConversation(hardDeleteTarget.id);
      notify("success", "Conversation permanently deleted");
      if (String(hardDeleteTarget.id) === activeConversationId) router.push(basePath);
      setHardDeleteTarget(null);
      reload();
    } catch (err) {
      notify("error", err.message || "Permanent delete failed");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <aside className="hidden md:flex w-64 shrink-0 flex-col border-r border-border-subtle bg-bg-secondary">
      <button
        type="button"
        onClick={() => setCreateOpen(true)}
        className="flex h-14 items-center gap-2 px-4 transition-colors hover:bg-bg-elevated mb-2"
      >
        <SquarePen className="h-5 w-5 text-text-secondary" />
        <span className="text-sm font-medium text-text-secondary">New conversation</span>
      </button>

      <div className="flex-1 overflow-y-auto p-3">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-8 text-text-muted">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">Loading…</span>
          </div>
        ) : error ? (
          <p className="px-2 text-sm text-error">{error}</p>
        ) : conversations.length === 0 ? (
          <p className="px-2 text-sm text-text-muted">No conversations yet.</p>
        ) : (
          <ul className="space-y-0.5">
            {conversations.map((c) => {
              const active = String(c.id) === activeConversationId;
              return (
                <li key={c.id} className="group relative">
                  <Link
                    href={`${basePath}/${c.id}`}
                    className={
                      active
                        ? "flex items-center gap-1 rounded-lg pl-2.5 pr-1 py-2.5 bg-bg-elevated border border-border-subtle transition-colors hover:border-border-default"
                        : "flex items-center gap-1 rounded-lg pl-2.5 pr-1 py-2.5 transition-colors hover:bg-bg-elevated"
                    }
                  >
                    <p
                      className={
                        active
                          ? "flex-1 min-w-0 truncate text-sm font-medium text-text-primary"
                          : "flex-1 min-w-0 truncate text-sm text-text-secondary"
                      }
                      title={c.title}
                    >
                      {c.title}
                    </p>
                    <span
                      className="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => e.preventDefault()}
                    >
                      <ConversationRowMenu
                        conversation={c}
                        onRename={setRenameTarget}
                        onDelete={setDeleteTarget}
                        onHardDelete={setHardDeleteTarget}
                      />
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <CreateConversationModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        conversationType={conversationType}
        onCreated={onCreated}
      />

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

      <ConfirmDialog
        open={Boolean(hardDeleteTarget)}
        onClose={() => setHardDeleteTarget(null)}
        onConfirm={confirmHardDelete}
        loading={deleting}
        title="Permanently delete conversation"
        confirmLabel="Delete permanently"
        message={`Permanently delete "${hardDeleteTarget?.title}"? This erases all messages, attached documents, and generated content for this conversation. This action cannot be undone.`}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </aside>
  );
}
