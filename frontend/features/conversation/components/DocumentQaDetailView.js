"use client";

import { useEffect, useRef, useState } from "react";
import { Send, Loader2, Quote } from "lucide-react";
import LoadMore from "@/components/ui/LoadMore";
import MessageSourcesDialog from "./MessageSourcesDialog";
import { getDocumentQaConversationDetail } from "@/services/conversationService";
import { sendMessage, getMessages } from "@/services/messageService";
import { formatDateTime } from "@/utils/format";
import { ApiError } from "@/lib/apiClient";

const RECENT_MESSAGES_LIMIT = 20;

export default function DocumentQaDetailView({ conversationId }) {
  const [conversation, setConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [hasMore, setHasMore] = useState(false);
  const [nextPage, setNextPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const [content, setContent] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState("");

  const [sourcesTarget, setSourcesTarget] = useState(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getDocumentQaConversationDetail(conversationId, RECENT_MESSAGES_LIMIT, controller.signal)
      .then((detail) => {
        if (controller.signal.aborted) return;
        setConversation(detail);
        setMessages(detail?.recentMessages || []);
        setHasMore(Boolean(detail?.hasMoreMessages));
        setNextPage(1);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load conversation");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [conversationId]);

  useEffect(() => {
    const onRenamed = (e) => {
      if (String(e.detail?.id) === String(conversationId)) {
        setConversation((prev) => (prev ? { ...prev, title: e.detail.title } : prev));
      }
    };
    window.addEventListener("conversation-renamed", onRenamed);
    return () => window.removeEventListener("conversation-renamed", onRenamed);
  }, [conversationId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages.length]);

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const slice = await getMessages(conversationId, { page: nextPage, size: RECENT_MESSAGES_LIMIT });
      setMessages((prev) => [...prev, ...(slice?.content || [])]);
      setHasMore(Boolean(slice?.hasNext));
      setNextPage((p) => p + 1);
    } catch (err) {
      setError(err.message || "Failed to load more messages");
    } finally {
      setLoadingMore(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    const trimmed = content.trim();
    if (!trimmed || sending) return;
    setSending(true);
    setSendError("");
    try {
      const { userMessage, assistantMessage } = await sendMessage(conversationId, trimmed);
      // ponytail: appended straight to the tail even if hasMore is still true (older
      // pages not yet loaded) — acceptable for the expected conversation sizes here;
      // reload the full detail instead if conversations regularly exceed a few pages.
      setMessages((prev) => [...prev, userMessage, ...(assistantMessage ? [assistantMessage] : [])]);
      setContent("");
    } catch (err) {
      setSendError(err instanceof ApiError ? err.message : "Failed to send message. Please try again.");
    } finally {
      setSending(false);
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

  return (
    <main className="flex-1 flex flex-col overflow-hidden">
      <div className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
        <div className="max-w-3xl mx-auto space-y-6">
          <h1 className="text-sm font-medium text-text-primary">{conversation?.title}</h1>

          <LoadMore hasMore={hasMore} loading={loadingMore} onClick={loadMore} label="Load earlier messages" />

          {messages.length === 0 ? (
            <p className="text-sm text-text-muted text-center py-10">
              No messages yet. Ask a question about your attached documents below.
            </p>
          ) : (
            messages.map((m) => {
              const isUser = m.role === "USER";
              return (
                <div key={m.id} className={`flex gap-4 ${isUser ? "flex-row-reverse" : ""}`}>
                  <div className={`flex-1 min-w-0 ${isUser ? "flex flex-col items-end" : "w-full"}`}>
                    <div
                      className={
                        isUser
                          ? "rounded-xl px-4 py-3 bg-bg-elevated border border-border-subtle max-w-[85%]"
                          : ""
                      }
                    >
                      <p className="text-sm text-text-primary leading-relaxed whitespace-pre-wrap text-left">
                        {m.content}
                      </p>
                    </div>
                    <div className={`flex items-center gap-2 mt-1.5 px-1 ${isUser ? "flex-row-reverse" : ""}`}>
                      <p className="text-xs text-text-muted">{formatDateTime(m.createdAt)}</p>
                      {!isUser && (
                        <button
                          type="button"
                          className="flex items-center gap-1 text-xs text-text-muted hover:text-accent transition-colors"
                          onClick={() => setSourcesTarget(m.id)}
                        >
                          <Quote className="h-3 w-3" />
                          Sources
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
          <div ref={bottomRef} />
        </div>
      </div>

      <div className="p-4">
        <form className="flex items-end gap-3 max-w-3xl mx-auto" onSubmit={submit}>
          <div className="flex-1">
            <div className="flex items-center gap-2 rounded-xl border border-border-subtle bg-bg-card px-3 py-2 min-h-[48px]">
              <textarea
                rows={1}
                placeholder="Ask a question about your documents..."
                className="flex-1 resize-none bg-transparent text-sm text-text-primary outline-none leading-normal"
                aria-label="Chat message input"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) submit(e);
                }}
                disabled={sending}
              />
              <button
                type="submit"
                className="flex h-8 w-8 items-center justify-center rounded-md bg-bg-elevated text-text-muted hover:bg-bg-card disabled:opacity-50"
                aria-label="Send message"
                disabled={!content.trim() || sending}
              >
                {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </form>
        {sendError ? (
          <p className="text-center text-xs text-error mt-2">{sendError}</p>
        ) : (
          <p className="text-center text-xs text-text-muted mt-2">
            AI responses are grounded in your selected source documents
          </p>
        )}
      </div>

      <MessageSourcesDialog
        open={Boolean(sourcesTarget)}
        onClose={() => setSourcesTarget(null)}
        conversationId={conversationId}
        messageId={sourcesTarget}
      />
    </main>
  );
}
