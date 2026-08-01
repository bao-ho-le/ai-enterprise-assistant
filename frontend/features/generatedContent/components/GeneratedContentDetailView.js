"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Loader2, ArrowLeft, Save, AlertCircle } from "lucide-react";
import Toast from "@/components/ui/Toast";
import { getGeneratedContentById, updateGeneratedContent } from "@/services/generatedContentService";
import { generatedDocumentTypeLabel } from "@/constants/generatedContent";
import { formatDateTime } from "@/utils/format";
import { ApiError } from "@/lib/apiClient";

// Only these generatedType values have a conversation detail page wired up
// (write-email/write-report/summary) — MEETING_MINUTES and FORM have no
// nav page yet, so their "back to conversation" link is skipped.
const CONVERSATION_BASE_PATH = {
  EMAIL: "/write-email",
  REPORT: "/write-report",
  SUMMARY: "/summary",
};

export default function GeneratedContentDetailView({ generatedContentId }) {
  const [item, setItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [toast, setToast] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getGeneratedContentById(generatedContentId, controller.signal)
      .then((d) => {
        if (controller.signal.aborted) return;
        setItem(d);
        setTitle(d?.title || "");
        setContent(d?.content || "");
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message || "Failed to load generated content");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [generatedContentId]);

  const dirty = item && (title !== item.title || content !== item.content);

  const save = async () => {
    if (!title.trim() || !content.trim() || saving) return;
    setSaving(true);
    setSaveError("");
    try {
      const updated = await updateGeneratedContent(generatedContentId, {
        title: title.trim(),
        content,
      });
      setItem(updated);
      setTitle(updated.title);
      setContent(updated.content);
      setToast({ type: "success", text: "Saved" });
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : "Save failed. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center overflow-hidden">
        <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
      </main>
    );
  }

  if (error) {
    return (
      <main className="flex-1 flex flex-col items-center justify-center gap-2 overflow-hidden text-error">
        <AlertCircle className="h-6 w-6" />
        <p className="text-sm">{error}</p>
      </main>
    );
  }

  const conversationBasePath = CONVERSATION_BASE_PATH[item.generatedType];

  return (
    <main className="flex-1 overflow-y-auto">
      <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-6">
          <Link href="/generated-contents" className="btn-secondary text-sm">
            <ArrowLeft className="h-4 w-4" />
            Back to list
          </Link>
          <span className="badge badge-neutral">{generatedDocumentTypeLabel(item.generatedType)}</span>
        </div>

        <article className="card overflow-hidden">
          <div className="px-6 py-4 border-b border-border-subtle bg-bg-elevated/50 space-y-3">
            <input
              type="text"
              className="input-field text-sm font-medium"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              aria-label="Title"
            />
            <p className="text-xs text-text-muted">
              Created {formatDateTime(item.createdAt)} · Updated {formatDateTime(item.updatedAt)}
              {conversationBasePath && (
                <>
                  {" · "}
                  <Link
                    href={`${conversationBasePath}/${item.aiConversationId}`}
                    className="text-accent hover:underline"
                  >
                    View source conversation
                  </Link>
                </>
              )}
            </p>
          </div>

          <div className="px-6 py-6">
            <textarea
              className="textarea-field w-full min-h-[300px] text-sm leading-relaxed"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              aria-label="Content"
            />
          </div>

          <div className="px-6 py-4 border-t border-border-subtle bg-bg-secondary flex items-center gap-3">
            <button
              type="button"
              className="btn-primary text-sm"
              onClick={save}
              disabled={!dirty || !title.trim() || !content.trim() || saving}
            >
              <Save className="h-4 w-4" />
              {saving ? "Saving…" : "Save"}
            </button>
            {saveError && <p className="text-sm text-error">{saveError}</p>}
          </div>
        </article>
      </div>

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
