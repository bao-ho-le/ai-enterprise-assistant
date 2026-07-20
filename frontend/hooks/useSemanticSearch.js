"use client";

import { useEffect, useState } from "react";
import { semanticSearch } from "@/services/searchService";

// Fires a semantic search whenever `keyword` is non-empty; empty keyword => no results, no request.
export function useSemanticSearch(keyword, topK) {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!keyword) {
      setResults([]);
      setError("");
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError("");
    semanticSearch({ keyword, topK }, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;
        setResults(data || []);
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Semantic search failed");
        setResults([]);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [keyword, topK]);

  return { results, loading, error };
}
