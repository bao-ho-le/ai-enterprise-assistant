"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { loadConversationThread } from "@/features/assistant/adapters";
import { mergeTimeline } from "@/features/assistant/models/timelineModel";

const EMPTY_PAGINATION = { hasMore: false, cursor: null, page: 0 };

// Owns everything about one open conversation: the adapter chosen from its
// conversationType, the normalized timeline, attached documents and pagination.
// All requests are guarded by a sequence counter so a slow response from a previously
// opened conversation can never overwrite the current one (the old per-screen views
// used AbortController only, which didn't cover retries/regenerations).
export default function useAssistantThread(conversationId) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notFound, setNotFound] = useState(false);
  const [adapter, setAdapter] = useState(null);
  const [conversation, setConversation] = useState(null);
  const [items, setItems] = useState([]);
  const [pagination, setPagination] = useState(EMPTY_PAGINATION);
  const [loadingOlder, setLoadingOlder] = useState(false);
  // Empty for conversation types without documents (email generation also hides the dock).
  const [documents, setDocuments] = useState([]);
  const [hasDeletedDocuments, setHasDeletedDocuments] = useState(false);
  const [latestInputData, setLatestInputData] = useState(null);

  const requestRef = useRef(0);
  const loadingOlderRef = useRef(false);

  const fetchThread = useCallback(
    async ({ signal } = {}) => {
      const sequence = ++requestRef.current;
      setLoading(true);
      setError("");
      setNotFound(false);
      try {
        const loaded = await loadConversationThread(conversationId, { signal });
        if (sequence !== requestRef.current || signal?.aborted) return;
        setAdapter(loaded.adapter);
        setConversation(loaded.conversation);
        setItems(loaded.items || []);
        setPagination(loaded.pagination || EMPTY_PAGINATION);
        setDocuments(loaded.attachedDocuments ?? []);
        setHasDeletedDocuments(Boolean(loaded.hasDeletedAttachedDocuments));
        setLatestInputData(loaded.latestInputData ?? null);
      } catch (err) {
        if (signal?.aborted || err?.name === "AbortError") return;
        if (sequence !== requestRef.current) return;
        if (err?.status === 404) setNotFound(true);
        else setError(err?.message || "Failed to load conversation");
      } finally {
        if (sequence === requestRef.current && !signal?.aborted) setLoading(false);
      }
    },
    [conversationId]
  );

  useEffect(() => {
    const controller = new AbortController();
    fetchThread({ signal: controller.signal });
    return () => controller.abort();
  }, [fetchThread]);

  const reload = useCallback(() => fetchThread(), [fetchThread]);

  // Older QA messages (keyset) / previous generation runs (page). Guarded synchronously:
  // the state flag alone can lag behind a second click/scroll.
  const loadOlder = useCallback(async () => {
    if (!adapter || loadingOlderRef.current || !pagination.hasMore) return null;
    loadingOlderRef.current = true;
    setLoadingOlder(true);
    try {
      const result = adapter.supportsMessages
        ? await adapter.loadOlder(conversationId, { cursor: pagination.cursor })
        : await adapter.loadOlder(conversationId, { page: (pagination.page ?? 0) + 1 });
      setItems((previous) => mergeTimeline(previous, result.items));
      setPagination((previous) => ({
        hasMore: Boolean(result.hasMore),
        cursor: result.cursor ?? previous.cursor,
        page: result.page ?? previous.page,
      }));
      return result;
    } catch (err) {
      setError(err?.message || "Failed to load older items");
      return null;
    } finally {
      loadingOlderRef.current = false;
      setLoadingOlder(false);
    }
  }, [adapter, conversationId, pagination]);

  const send = useCallback(
    async (content) => {
      if (!adapter?.supportsMessages) {
        throw new Error("This conversation type does not support chat messages.");
      }
      const newItems = await adapter.sendMessage(conversationId, content);
      setItems((previous) => mergeTimeline(previous, newItems));
      return newItems;
    },
    [adapter, conversationId]
  );

  const regenerate = useCallback(
    async (values) => {
      if (!adapter?.regenerate) {
        throw new Error("This conversation type cannot be regenerated.");
      }
      const result = await adapter.regenerate(conversationId, values);
      await fetchThread();
      return result;
    },
    [adapter, conversationId, fetchThread]
  );

  const loadGeneratedContent = useCallback(
    async (generatedContentId) => {
      if (!adapter?.loadGeneratedContent) return null;
      return adapter.loadGeneratedContent(generatedContentId);
    },
    [adapter]
  );

  const refreshDocuments = useCallback(async () => {
    if (!adapter?.loadDocuments) return;
    try {
      const docs = await adapter.loadDocuments(conversationId);
      setDocuments(docs || []);
    } catch {
      // Best-effort resync: a stale doc list only affects the side panel until reload.
    }
  }, [adapter, conversationId]);

  // Sidebar rename updates the title in place (same CustomEvent the old views used).
  useEffect(() => {
    const onRenamed = (event) => {
      if (String(event.detail?.id) !== String(conversationId)) return;
      setConversation((previous) => (previous ? { ...previous, title: event.detail.title } : previous));
    };
    window.addEventListener("conversation-renamed", onRenamed);
    return () => window.removeEventListener("conversation-renamed", onRenamed);
  }, [conversationId]);

  return {
    loading,
    error,
    notFound,
    adapter,
    conversation,
    items,
    pagination,
    loadingOlder,
    send,
    regenerate,
    reload,
    loadOlder,
    loadGeneratedContent,
    documents,
    refreshDocuments,
    hasDeletedDocuments,
    latestInputData,
  };
}
