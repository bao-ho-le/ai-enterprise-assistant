"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Loader2, FileText, FileOutput } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import {
  getGenerationConversationDetail,
  getConversationGeneratedContents,
} from "@/services/conversationService";
import { generationStatusBadge, generatedDocumentTypeLabel } from "@/constants/generatedContent";
import { formatDateTime } from "@/utils/format";

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

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setNotFound(false);
    setError("");
    getGenerationConversationDetail(conversationId, controller.signal)
      .then((d) => {
        if (!controller.signal.aborted) setDetail(d);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        if (err.status === 404) setNotFound(true);
        else setError(err.message || "Failed to load generation detail");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [conversationId]);

  useEffect(() => {
    const onRenamed = (e) => {
      if (String(e.detail?.id) === String(conversationId)) {
        setDetail((prev) => (prev ? { ...prev, title: e.detail.title } : prev));
      }
    };
    window.addEventListener("conversation-renamed", onRenamed);
    return () => window.removeEventListener("conversation-renamed", onRenamed);
  }, [conversationId]);

  const loadHistoryPage = useCallback(
    (page, append) => {
      const loadingSetter = append ? setHistoryLoadingMore : setHistoryLoading;
      loadingSetter(true);
      return getConversationGeneratedContents(conversationId, { page, size: HISTORY_PAGE_SIZE })
        .then((slice) => {
          setHistory((prev) => (append ? [...prev, ...(slice?.content || [])] : slice?.content || []));
          setHistoryHasMore(Boolean(slice?.hasNext));
          setHistoryPage(page);
        })
        .catch(() => {
          // History is a secondary section — a failure here shouldn't block the main detail view.
        })
        .finally(() => loadingSetter(false));
    },
    [conversationId]
  );

  useEffect(() => {
    loadHistoryPage(0, false);
  }, [loadHistoryPage]);

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
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-lg font-semibold text-text-primary">{detail.title}</h1>
                <span className={`badge ${status.badge}`}>{status.label}</span>
              </div>
              <p className="text-xs text-text-muted">Last updated {formatDateTime(detail.updatedAt)}</p>
            </div>

            {detail.generatedContentId != null && (
              <Link
                href={`/generated-contents/${detail.generatedContentId}`}
                className="btn-primary text-sm w-fit"
              >
                <FileOutput className="h-4 w-4" />
                View generated content
              </Link>
            )}

            <section>
              <h2 className="text-sm font-medium text-text-primary mb-2">Submitted input</h2>
              <pre className="card p-4 text-xs text-text-secondary overflow-x-auto whitespace-pre-wrap">
                {detail.inputData ? JSON.stringify(detail.inputData, null, 2) : "—"}
              </pre>
            </section>

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

        <section>
          <h2 className="text-sm font-medium text-text-primary mb-2">Generated content in this conversation</h2>
          {historyLoading ? (
            <div className="flex items-center justify-center gap-2 py-6 text-text-muted">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span className="text-sm">Loading…</span>
            </div>
          ) : history.length === 0 ? (
            <p className="text-sm text-text-muted">No generated content yet.</p>
          ) : (
            <ul className="space-y-2">
              {history.map((item) => (
                <li key={item.id}>
                  <Link
                    href={`/generated-contents/${item.id}`}
                    className="card flex items-center gap-3 p-3 hover:border-border-default transition-colors"
                  >
                    <FileOutput className="h-4 w-4 text-text-secondary shrink-0" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-text-primary">{item.title}</p>
                      <p className="text-xs text-text-muted">{generatedDocumentTypeLabel(item.generatedType)}</p>
                    </div>
                    <span className="text-xs text-text-muted shrink-0">{formatDateTime(item.createdAt)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
          <LoadMore
            hasMore={historyHasMore}
            loading={historyLoadingMore}
            onClick={() => loadHistoryPage(historyPage + 1, true)}
          />
        </section>
      </div>
    </main>
  );
}
