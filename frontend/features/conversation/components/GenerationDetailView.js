"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Loader2, FileText, FileOutput, History } from "lucide-react";
import GenerationForm, { splitEmailContent } from "./GenerationForm";
import EmailPreview from "./EmailPreview";
import GeneratedContentPreview from "./GeneratedContentPreview";
import GenerationHistoryModal from "./GenerationHistoryModal";
import {
  getGenerationConversationDetail,
  getConversationGenerations,
} from "@/services/conversationService";
import { getGeneratedContentById } from "@/services/generatedContentService";
import { conversationTypeLabel } from "@/constants/conversation";
import { generationStatusBadge } from "@/constants/generatedContent";
import { formatDateTime, formatDateTimeSlash } from "@/utils/format";

const HISTORY_PAGE_SIZE = 10;

export default function GenerationDetailView({ conversationId }) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState("");

  const [history, setHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyHasMore, setHistoryHasMore] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyLoadingMore, setHistoryLoadingMore] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const autoPreviewHandled = useRef(false);

  // Opening a generated content replaces the form with its preview, same as the
  // create-flow does after generating an email.
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  const loadDetail = useCallback(
    (signal) => {
      setLoading(true);
      setNotFound(false);
      setError("");
      return getGenerationConversationDetail(conversationId, signal)
        .then((d) => {
          if (!signal?.aborted) setDetail(d);
          return d;
        })
        .catch((err) => {
          if (signal?.aborted || err.name === "AbortError") return;
          if (err.status === 404) setNotFound(true);
          else setError(err.message || "Failed to load generation detail");
        })
        .finally(() => {
          if (!signal?.aborted) setLoading(false);
        });
    },
    [conversationId]
  );

  useEffect(() => {
    const controller = new AbortController();
    loadDetail(controller.signal);
    return () => controller.abort();
  }, [loadDetail]);

  // Auto-open preview when redirected from the create-flow after a successful
  // generation — matches writing a report/summary for the first time. Runs once
  // (guarded by autoPreviewHandled) and immediately strips the one-time `?preview=true`
  // param from the URL: left in place, it would re-trigger this same auto-preview on
  // every later remount (a refresh, or browser back/forward) even after the user
  // explicitly clicked "Back to Form" — the URL is state too, and it must not linger
  // stale once consumed.
  useEffect(() => {
    if (autoPreviewHandled.current) return;
    if (searchParams?.get('preview') !== 'true') return;
    if (detail?.generatedContentId == null) return;
    autoPreviewHandled.current = true;
    openGeneratedContent(detail.generatedContentId);
    router.replace(pathname, { scroll: false });
  }, [detail, searchParams, router, pathname]);

  const loadHistoryPage = useCallback(
    (page, append) => {
      const loadingSetter = append ? setHistoryLoadingMore : setHistoryLoading;
      loadingSetter(true);
      return getConversationGenerations(conversationId, { page, size: HISTORY_PAGE_SIZE })
        .then((slice) => {
          setHistory((prev) => (append ? [...prev, ...(slice?.content || [])] : slice?.content || []));
          setHistoryHasMore(Boolean(slice?.hasNext));
          setHistoryPage(page);
        })
        .catch(() => {
          // History is a secondary view — a failure here shouldn't block the main detail.
        })
        .finally(() => loadingSetter(false));
    },
    [conversationId]
  );

  useEffect(() => {
    loadHistoryPage(0, false);
  }, [loadHistoryPage]);

  // Regenerating jumps straight to the new content's preview, same as the create flow —
  // still refreshes the detail/history underneath so "Back to Form" shows the new run.
  const onRegenerated = (result) => {
    loadDetail();
    loadHistoryPage(0, false);
    if (result?.generatedContentId != null) {
      openGeneratedContent(result.generatedContentId);
    }
  };

  useEffect(() => {
    const onRenamed = (e) => {
      if (String(e.detail?.id) === String(conversationId)) {
        setDetail((prev) => (prev ? { ...prev, title: e.detail.title } : prev));
      }
    };
    window.addEventListener("conversation-renamed", onRenamed);
    return () => window.removeEventListener("conversation-renamed", onRenamed);
  }, [conversationId]);

  const openGeneratedContent = async (generatedContentId) => {
    // Close the History modal even when a preview load is already in flight, so
    // it never lingers behind/after the preview (the early return below must not
    // skip this).
    setHistoryOpen(false);
    if (generatedContentId == null || previewLoading) return;
    setPreviewLoading(true);
    try {
      setPreview(await getGeneratedContentById(generatedContentId));
    } catch (err) {
      setError(err.message || "Failed to load generated content");
    } finally {
      setPreviewLoading(false);
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
      <main className="flex-1 flex items-center justify-center overflow-hidden">
        <p className="text-sm text-error">{error}</p>
      </main>
    );
  }

  if (preview) {
    // "Back to Form" must also close the Generation History modal — if it was
    // re-opened while the preview was loading, historyOpen would otherwise stay
    // true and the modal would reappear once back on the form.
    const back = () => {
      setHistoryOpen(false);
      setPreview(null);
    };
    return (
      <main className="flex-1 overflow-y-auto">
        {detail?.conversationType === "EMAIL_GENERATION" ? (
          (() => {
            const { subject, body } = splitEmailContent(preview.content);
            return (
              <EmailPreview
                from={detail?.inputData?.sender}
                to={detail?.inputData?.recipient}
                subject={subject}
                onSubjectChange={() => {}}
                body={body}
                onBack={back}
              />
            );
          })()
        ) : (
          <GeneratedContentPreview item={preview} onBack={back} />
        )}
      </main>
    );
  }

  const status = detail ? generationStatusBadge(detail.status) : null;

  return (
    <main className="flex-1 overflow-y-auto">
      <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 space-y-8">
        {notFound ? (
          <div className="card p-6 text-center">
            <p className="text-sm text-text-secondary">
              No generation run yet for this conversation.
            </p>
          </div>
        ) : (
          <>
            <div>
              <div className="flex flex-wrap items-center gap-3 mb-1">
                <h1 className="text-lg font-semibold text-text-primary">
                  {conversationTypeLabel(detail.conversationType)} - {formatDateTimeSlash(detail.runCreatedAt)}
                </h1>
                <span className={`badge ${status.badge}`}>{status.label}</span>
              </div>
              <p className="text-xs text-text-muted">
                Last updated {formatDateTime(detail.runUpdatedAt)}
              </p>
            </div>

            <GenerationForm
              conversationType={detail.conversationType}
              conversationId={conversationId}
              initialValues={detail.inputData}
              onGenerated={onRegenerated}
              attachedDocumentCount={detail.attachedDocuments?.length ?? 0}
              actions={
                <>
                  <button
                    type="button"
                    className="btn-secondary text-sm"
                    disabled={detail.generatedContentId == null || previewLoading}
                    onClick={() => openGeneratedContent(detail.generatedContentId)}
                  >
                    {previewLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <FileOutput className="h-4 w-4" />
                    )}
                    View Generated Content
                  </button>
                  <button
                    type="button"
                    className="btn-secondary text-sm"
                    onClick={() => setHistoryOpen(true)}
                  >
                    <History className="h-4 w-4" />
                    History
                  </button>
                </>
              }
            />

            {detail.attachedDocuments !== null && detail.attachedDocuments !== undefined && (
              <section>
                <h2 className="text-sm font-medium text-text-primary mb-2">
                  Source documents ({detail.attachedDocuments.length})
                </h2>
                {detail.attachedDocuments.length === 0 ? (
                  <p className="text-sm text-text-muted">No source documents attached.</p>
                ) : (
                  <ul className="space-y-2">
                    {detail.attachedDocuments.map((doc) => (
                      <li key={doc.documentVersionId} className="card flex items-center gap-3 p-3">
                        <FileText className="h-4 w-4 text-text-secondary shrink-0" />
                        <span className="truncate text-sm text-text-primary flex-1">{doc.documentTitle}</span>
                        <span className="text-xs text-text-muted shrink-0">v{doc.versionNumber}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            )}
          </>
        )}
      </div>

      <GenerationHistoryModal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        items={history}
        loading={historyLoading}
        hasMore={historyHasMore}
        loadingMore={historyLoadingMore}
        onLoadMore={() => loadHistoryPage(historyPage + 1, true)}
        onSelect={openGeneratedContent}
      />
    </main>
  );
}
