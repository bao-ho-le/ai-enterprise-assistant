"use client";

import { useEffect, useState } from "react";
import AIUsageSummaryCards from "./AIUsageSummaryCards";
import AIUsageCharts from "./AIUsageCharts";
import AIUsageFilters from "./AIUsageFilters";
import AIUsageTable from "./AIUsageTable";
import { dateInputToIso } from "@/utils/format";
import { SUPPORTED_MODELS } from "@/constants/aiUsage";
import { getUsageLogs, getUsageSummary, getUsageDaily, getUsageModels } from "@/services/aiUsageService";

const PAGE_SIZE = 10;
const CHART_DAYS = 7;

// The backend's /ai-usage/models only returns models that have actually been logged.
// Union with the system's supported models so the Model filter always offers every
// model the system can use, even ones with no usage records yet (e.g. embedding).
function mergeModelOptions(loggedModels) {
  return [...new Set([...SUPPORTED_MODELS, ...(loggedModels || [])])].sort();
}

const INITIAL_FILTERS = {
  fromDate: "",
  toDate: "",
  conversationType: "",
  model: "",
  status: "",
};

export default function AIUsageView() {
  const [summary, setSummary] = useState(null);
  const [daily, setDaily] = useState([]);
  const [models, setModels] = useState([]);

  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [page, setPage] = useState(0);

  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0, number: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getUsageSummary(controller.signal)
      .then((s) => {
        if (!controller.signal.aborted) setSummary(s);
      })
      .catch(() => {
        // Summary cards just stay blank on failure — the table below still works.
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    getUsageDaily(CHART_DAYS, controller.signal)
      .then((d) => {
        if (!controller.signal.aborted) setDaily(d || []);
      })
      .catch(() => {
        // Charts just stay empty on failure — the rest of the dashboard still works.
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    getUsageModels(controller.signal)
      .then((m) => {
        if (!controller.signal.aborted) setModels(mergeModelOptions(m));
      })
      .catch(() => {
        // If the models fetch fails, still offer the known supported models so the
        // filter stays usable — distinct logged models are just missing from the list.
        if (!controller.signal.aborted) setModels([...SUPPORTED_MODELS]);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const params = {
      fromDate: dateInputToIso(filters.fromDate, false),
      toDate: dateInputToIso(filters.toDate, true),
      conversationType: filters.conversationType || undefined,
      model: filters.model || undefined,
      status: filters.status || undefined,
      page,
      size: PAGE_SIZE,
    };

    setLoading(true);
    setError("");
    getUsageLogs(params, controller.signal)
      .then((pageData) => {
        if (controller.signal.aborted) return;
        setData(pageData || { content: [], totalElements: 0, totalPages: 0, number: 0 });
      })
      .catch((err) => {
        if (controller.signal.aborted || err.name === "AbortError") return;
        setError(err.message || "Failed to load usage logs");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [filters.fromDate, filters.toDate, filters.conversationType, filters.status, filters.model, page]);

  const onFilterChange = (patch) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPage(0);
  };

  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">AI Usage Dashboard</h1>
        <p className="mt-2 text-text-secondary">
          Monitor token consumption, costs, and model performance across your organization.
        </p>
      </div>

      <AIUsageSummaryCards summary={summary} />

      {daily.length > 0 && <AIUsageCharts daily={daily} />}

      <AIUsageFilters filters={filters} onChange={onFilterChange} models={models} />

      <AIUsageTable
        logs={data.content}
        loading={loading}
        error={error}
        number={data.number}
        totalPages={data.totalPages}
        totalElements={data.totalElements}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
      />
    </main>
  );
}
