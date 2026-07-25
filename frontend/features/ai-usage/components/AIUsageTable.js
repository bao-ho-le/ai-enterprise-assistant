"use client";

import { Loader2, Inbox, AlertCircle } from "lucide-react";
import Pagination from "@/features/document/components/Pagination";
import { formatDateTimeSlash } from "@/utils/format";
import { conversationTypeLabel, statusBadge } from "@/constants/aiUsage";

const HEADERS = [
  { label: "Time" },
  { label: "Conversation Type" },
  { label: "Model" },
  { label: "Input Tokens", align: "right" },
  { label: "Output Tokens", align: "right" },
  { label: "Total Tokens", align: "right" },
  { label: "Est. Cost", align: "right" },
  { label: "Status" },
];

function StateRow({ children }) {
  return (
    <tr>
      <td colSpan={HEADERS.length} className="px-4 py-16">
        <div className="flex flex-col items-center justify-center gap-3 text-center text-text-muted">
          {children}
        </div>
      </td>
    </tr>
  );
}

export default function AIUsageTable({
  logs,
  loading,
  error,
  number,
  totalPages,
  totalElements,
  onPrev,
  onNext,
}) {
  return (
    <div className="card overflow-hidden">
      <div className="px-4 py-3 border-b border-border-subtle bg-bg-secondary">
        <h2 className="text-sm font-semibold text-text-primary">Filter Result</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[900px]">
          <thead>
            <tr className="border-b border-border-subtle">
              {HEADERS.map((h) => (
                <th
                  key={h.label}
                  className={`px-4 py-3 text-${h.align || "left"} text-xs font-medium uppercase tracking-wider text-text-muted`}
                >
                  {h.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading && (
              <StateRow>
                <Loader2 className="h-6 w-6 animate-spin" />
                <p className="text-sm">Loading usage logs…</p>
              </StateRow>
            )}

            {!loading && error && (
              <StateRow>
                <AlertCircle className="h-6 w-6 text-error" />
                <p className="text-sm text-error">{error}</p>
              </StateRow>
            )}

            {!loading && !error && logs.length === 0 && (
              <StateRow>
                <Inbox className="h-6 w-6" />
                <p className="text-sm">No usage logs found</p>
              </StateRow>
            )}

            {!loading &&
              !error &&
              logs.map((log, i) => {
                const status = statusBadge(log.status);
                return (
                  <tr key={i} className="border-b border-border-subtle hover:bg-bg-elevated/50 transition-colors">
                    <td className="px-4 py-3 text-sm text-text-secondary whitespace-nowrap">
                      {formatDateTimeSlash(log.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <span className="badge badge-neutral">{conversationTypeLabel(log.conversationType)}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary">{log.model}</td>
                    <td className="px-4 py-3 text-sm text-text-secondary text-right">
                      {Number(log.inputTokens || 0).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary text-right">
                      {Number(log.outputTokens || 0).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-primary text-right font-medium">
                      {Number(log.totalTokens || 0).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary text-right">
                      ${Number(log.estimatedCost || 0).toFixed(4)}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`badge ${status.badge}`}>{status.label}</span>
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>

      <Pagination
        number={number}
        totalPages={totalPages}
        totalElements={totalElements}
        shown={logs.length}
        onPrev={onPrev}
        onNext={onNext}
        itemLabel="logs"
      />
    </div>
  );
}
