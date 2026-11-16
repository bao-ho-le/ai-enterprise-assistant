"use client";

import { useEffect } from "react";
import { FileOutput, Loader2, RefreshCw, SlidersHorizontal, Sparkles } from "lucide-react";
import EmailPreview from "@/features/conversation/components/EmailPreview";
import GeneratedContentPreview from "@/features/conversation/components/GeneratedContentPreview";
import { generationStatusBadge } from "@/constants/generatedContent";
import { conversationTypeLabel } from "@/constants/conversation";
import { formatDateTime } from "@/utils/format";
import {
  describeGenerationInput,
  emailPreviewProps,
} from "@/features/assistant/models/generatedContentModel";

// One generation run inside the unified timeline. A run is NOT a chat message: it keeps
// its own generationId/status/generatedContentId, shows the stored inputData (latest run
// only — the history endpoint returns metadata only) and reuses the existing
// EmailPreview / GeneratedContentPreview components for the body.
export default function GenerationResultCard({
  item,
  conversationType,
  contentEntry,
  onLoadContent,
  onRegenerate,
  onOpenOptions,
  regenerating = false,
  canRegenerate = false,
}) {
  const status = generationStatusBadge(item.status);
  const inputSummary = describeGenerationInput(item.inputData);
  const entry = contentEntry || null;
  const hasContent = Boolean(entry?.data);

  // Auto-load the newest run's content once, so the thread opens on the actual result
  // (the old detail screens auto-opened the latest preview the same way).
  useEffect(() => {
    if (!item.isLatest) return;
    if (item.generatedContentId == null || entry) return;
    onLoadContent?.(item);
  }, [item, entry, onLoadContent]);

  const showRegenerate = canRegenerate && item.isLatest;

  return (
    <article className="card p-4 space-y-3">
      <header className="flex flex-wrap items-start gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-bg-elevated">
          <Sparkles className="h-4 w-4 text-accent" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-text-primary">
            {conversationTypeLabel(conversationType)}
          </p>
          <p className="text-xs text-text-muted">
            {formatDateTime(item.createdAt)}
            {item.isLatest ? " · latest run" : ""}
          </p>
        </div>
        <span className={`badge ${status.badge} shrink-0`}>{status.label}</span>
      </header>

      {inputSummary.length > 0 && (
        <dl className="space-y-1.5 rounded-lg border border-border-subtle bg-bg-primary p-3 text-xs">
          {inputSummary.map(({ name, label, value, long }) =>
            long ? (
              <div key={name} className="space-y-0.5">
                <dt className="text-text-muted">{label}</dt>
                <dd className="whitespace-pre-wrap text-text-secondary">{value}</dd>
              </div>
            ) : (
              <div key={name} className="flex gap-2">
                <dt className="w-20 shrink-0 text-text-muted">{label}</dt>
                <dd className="min-w-0 flex-1 truncate text-text-secondary" title={value}>
                  {value}
                </dd>
              </div>
            )
          )}
        </dl>
      )}

      {entry?.loading ? (
        <div className="flex items-center justify-center gap-2 rounded-lg border border-border-subtle py-8 text-text-muted">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="text-sm">Loading generated content…</span>
        </div>
      ) : entry?.error ? (
        <div className="space-y-2 rounded-lg border border-error/30 bg-error/5 p-3">
          <p className="text-sm text-error">{entry.error}</p>
          <button type="button" className="btn-secondary text-xs" onClick={() => onLoadContent?.(item)}>
            <RefreshCw className="h-3.5 w-3.5" />
            Try again
          </button>
        </div>
      ) : hasContent ? (
        entry.data.generatedType === "EMAIL" ? (
          <EmailPreview
            embedded
            {...emailPreviewProps(item.inputData, entry.data.content)}
            onSubjectChange={() => {}}
          />
        ) : (
          <GeneratedContentPreview embedded item={entry.data} />
        )
      ) : (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border-subtle bg-bg-primary px-3 py-2.5">
          <p className="text-xs text-text-muted">
            {item.generatedContentId != null
              ? "Generated content is ready."
              : "No content for this run — the generation did not complete."}
          </p>
          {item.generatedContentId != null && (
            <button
              type="button"
              className="btn-secondary text-xs"
              onClick={() => onLoadContent?.(item)}
            >
              <FileOutput className="h-3.5 w-3.5" />
              View generated content
            </button>
          )}
        </div>
      )}

      {showRegenerate && (
        <div className="flex flex-wrap items-center gap-2 border-t border-border-subtle pt-3">
          <button
            type="button"
            className="btn-primary text-sm"
            onClick={onRegenerate}
            disabled={regenerating}
          >
            {regenerating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
            {regenerating ? "Regenerating…" : "Regenerate"}
          </button>
          {onOpenOptions && (
            <button type="button" className="btn-secondary text-sm" onClick={onOpenOptions} disabled={regenerating}>
              <SlidersHorizontal className="h-4 w-4" />
              Options
            </button>
          )}
        </div>
      )}
    </article>
  );
}
