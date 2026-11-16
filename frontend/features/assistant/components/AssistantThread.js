"use client";

import { useLayoutEffect, useRef, useState } from "react";
import { Copy, Quote } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import MessageSourcesDialog from "@/features/conversation/components/MessageSourcesDialog";
import GenerationResultCard from "./GenerationResultCard";
import { formatDateTime } from "@/utils/format";
import { isGenerationItem } from "@/features/assistant/models/timelineModel";

// Renders "[1]", "[2]" citation markers (FakeLLMService.qaAnswer) as small numbered
// badges inline instead of literal bracket text — unchanged from the QA screen.
function renderContentWithCitations(content) {
  return (content || "").split(/(\[\d+\])/g).map((part, index) => {
    const match = /^\[(\d+)\]$/.exec(part);
    if (!match) return part;
    return (
      <span
        key={index}
        className="mx-0.5 inline-flex h-4 w-4 items-center justify-center rounded-full bg-bg-elevated text-[10px] text-text-muted align-middle"
      >
        {match[1]}
      </span>
    );
  });
}

export default function AssistantThread({
  conversationId,
  conversationType,
  items,
  emptyState = null,
  hasMore = false,
  loadingOlder = false,
  onLoadOlder,
  contents,
  onLoadContent,
  onRegenerate,
  onOpenOptions,
  regenerating = false,
  canRegenerate = false,
}) {
  const scrollRef = useRef(null);
  const [copiedMessageId, setCopiedMessageId] = useState(null);
  const [sourcesTarget, setSourcesTarget] = useState(null);

  // Loading an older page prepends items; keep the reader at the same distance from the
  // bottom instead of jumping (targeted replaces the old per-message scroll anchoring).
  const keepOffsetRef = useRef(false);
  const offsetFromBottomRef = useRef(0);
  const previousCountRef = useRef(items.length);

  const loadOlder = async () => {
    const container = scrollRef.current;
    offsetFromBottomRef.current = container ? container.scrollHeight - container.scrollTop : 0;
    keepOffsetRef.current = true;
    const result = await onLoadOlder?.();
    if (!result) keepOffsetRef.current = false;
  };

  useLayoutEffect(() => {
    const container = scrollRef.current;
    if (!container) return;

    if (keepOffsetRef.current) {
      keepOffsetRef.current = false;
      container.scrollTop = container.scrollHeight - offsetFromBottomRef.current;
    } else if (items.length > previousCountRef.current) {
      container.scrollTop = container.scrollHeight;
    }
    previousCountRef.current = items.length;
  }, [items]);

  const copyMessage = async (messageId, content) => {
    try {
      await navigator.clipboard.writeText(content || "");
      setCopiedMessageId(messageId);
      setTimeout(() => setCopiedMessageId((previous) => (previous === messageId ? null : previous)), 1500);
    } catch {
      // Clipboard API unavailable (e.g. insecure context) — nothing more we can do here.
    }
  };

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
        <div className="mx-auto max-w-3xl space-y-5">
          {hasMore && (
            <LoadMore
              hasMore={hasMore}
              loading={loadingOlder}
              onClick={loadOlder}
              label={conversationType === "DOCUMENT_QA" ? "Load older messages" : "Load earlier runs"}
            />
          )}

          {items.length === 0
            ? emptyState
            : items.map((item) =>
                isGenerationItem(item) ? (
                  <GenerationResultCard
                    key={item.id}
                    item={item}
                    conversationType={item.conversationType || conversationType}
                    contentEntry={contents?.[item.generatedContentId]}
                    onLoadContent={onLoadContent}
                    onRegenerate={onRegenerate}
                    onOpenOptions={onOpenOptions}
                    regenerating={regenerating}
                    canRegenerate={canRegenerate}
                  />
                ) : (
                  <QaMessage
                    key={item.id}
                    item={item}
                    copied={copiedMessageId === item.messageId}
                    onCopy={() => copyMessage(item.messageId, item.content)}
                    onShowSources={() => setSourcesTarget(item.messageId)}
                  />
                )
              )}
        </div>
      </div>

      {sourcesTarget != null && (
        <MessageSourcesDialog
          open
          onClose={() => setSourcesTarget(null)}
          conversationId={conversationId}
          messageId={sourcesTarget}
        />
      )}
    </div>
  );
}

function QaMessage({ item, copied, onCopy, onShowSources }) {
  const isUser = item.role === "USER";

  return (
    <div className={`flex gap-4 ${isUser ? "flex-row-reverse" : ""}`}>
      <div className={`flex-1 min-w-0 ${isUser ? "flex flex-col items-end" : "w-full"}`}>
        <div
          className={
            isUser
              ? "rounded-xl px-4 py-3 bg-bg-elevated border border-border-subtle max-w-[85%]"
              : ""
          }
        >
          <p
            className={`text-sm text-text-primary leading-relaxed whitespace-pre-wrap ${
              isUser ? "text-left" : "text-justify"
            }`}
          >
            {isUser ? item.content : renderContentWithCitations(item.content)}
          </p>
        </div>
        <div className={`flex items-center gap-2 mt-1.5 px-1 ${isUser ? "flex-row-reverse" : ""}`}>
          {!isUser && (
            <>
              <button
                type="button"
                className="flex items-center gap-1 text-xs text-text-muted hover:text-accent transition-colors"
                onClick={onCopy}
              >
                <Copy className="h-3 w-3" />
                {copied ? "Copied!" : "Copy"}
              </button>
              <button
                type="button"
                className="flex items-center gap-1 text-xs text-text-muted hover:text-accent transition-colors"
                onClick={onShowSources}
              >
                <Quote className="h-3 w-3" />
                Sources
              </button>
            </>
          )}
          <p className="text-xs text-text-muted">{formatDateTime(item.createdAt)}</p>
        </div>
      </div>
    </div>
  );
}
