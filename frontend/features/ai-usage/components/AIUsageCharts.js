"use client";

import { MoreHorizontal } from "lucide-react";

// Plot area height in px, shared by every bar chart's Y axis and bar tracks
// so the axis ticks and bar heights stay pixel-aligned with each other.
const TRACK_HEIGHT = 160;

function weekdayLabel(dateStr) {
  const d = new Date(`${dateStr}T00:00:00`);
  return d.toLocaleDateString("en-US", { weekday: "short" });
}

function formatCompactNumber(value) {
  return new Intl.NumberFormat("en-US", { notation: "compact" }).format(value);
}

// Decimals fixed per-chart (from `max`), not per-tick — otherwise max/half/zero
// render with different precision ($1.00 next to $0.5000) and look inconsistent.
function makeCostFormatter(max) {
  const decimals = max < 1 ? 4 : 2;
  return (value) => `$${value.toFixed(decimals)}`;
}

function ChartHeader({ title, subtitle }) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div>
        <h3 className="text-sm font-semibold text-text-primary">{title}</h3>
        <p className="text-xs text-text-muted mt-0.5">{subtitle}</p>
      </div>
      <button type="button" className="btn-ghost p-1.5" aria-label="Chart options">
        <MoreHorizontal className="h-4 w-4" />
      </button>
    </div>
  );
}

// Y axis: 3 ticks (max, half, zero) spanning the same TRACK_HEIGHT as the bars.
function YAxis({ max, format }) {
  return (
    <div
      className="flex flex-col justify-between text-right text-[10px] text-text-muted pr-2 shrink-0"
      style={{ height: TRACK_HEIGHT }}
    >
      <span>{format(max)}</span>
      <span>{format(max / 2)}</span>
      <span>{format(0)}</span>
    </div>
  );
}

// value/max -> px within TRACK_HEIGHT. Floors to a sliver so zero-value days
// still render a hairline bar instead of disappearing entirely.
function barHeight(value, max) {
  if (max <= 0) return 0;
  return Math.max(2, (value / max) * TRACK_HEIGHT);
}

// One day's column: a fixed-height (TRACK_HEIGHT) bar track — so its bottom
// edge (0) always lines up with the Y axis regardless of label height below —
// with the weekday label directly underneath, both at equal flex-1 width so
// the bar sits exactly above its own label instead of drifting from
// justify-between spacing out narrower (max-width-capped) bars unevenly.
function DayColumn({ label, children }) {
  return (
    <div className="flex-1 flex flex-col items-center gap-2">
      <div className="w-full flex items-end justify-center" style={{ height: TRACK_HEIGHT }}>
        {children}
      </div>
      <span className="text-[10px] text-text-muted">{label}</span>
    </div>
  );
}

export default function AIUsageCharts({ daily }) {
  const maxRequests = Math.max(1, ...daily.map((d) => d.requestCount));
  const maxTokens = Math.max(1, ...daily.map((d) => d.inputTokens + d.outputTokens));
  const maxCost = Math.max(1, ...daily.map((d) => Number(d.cost)));
  const costFormat = makeCostFormatter(maxCost);

  const totalSuccess = daily.reduce((sum, d) => sum + d.successCount, 0);
  const totalFailed = daily.reduce((sum, d) => sum + d.failedCount, 0);
  const totalRequests = totalSuccess + totalFailed;
  const successRate = totalRequests === 0 ? 0 : (totalSuccess / totalRequests) * 100;
  const errorDash = totalRequests === 0 ? 0 : (totalFailed / totalRequests) * 100;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <article className="card p-6">
        <ChartHeader title="Request Trend" subtitle={`Total requests, last ${daily.length} days`} />
        <div className="flex gap-1">
          <YAxis max={maxRequests} format={formatCompactNumber} />
          <div className="flex-1 flex items-end justify-between gap-1">
            {daily.map((d, i) => (
              <DayColumn key={d.date} label={weekdayLabel(d.date)}>
                <div
                  className={`w-full max-w-[32px] rounded-t ${i === daily.length - 1 ? "bg-accent" : "bg-accent/20"}`}
                  style={{ height: `${barHeight(d.requestCount, maxRequests)}px` }}
                />
              </DayColumn>
            ))}
          </div>
        </div>
      </article>

      <article className="card p-6">
        <ChartHeader title="Token Usage" subtitle={`Input and output tokens, last ${daily.length} days`} />
        <div className="flex gap-1">
          <YAxis max={maxTokens} format={formatCompactNumber} />
          <div className="flex-1 flex items-end justify-between gap-2">
            {daily.map((d) => (
              <DayColumn key={d.date} label={weekdayLabel(d.date)}>
                <div className="w-full max-w-[28px] flex flex-col justify-end items-center gap-0.5 h-full">
                  <div
                    className="w-full rounded-t bg-purple-400/40"
                    style={{ height: `${(d.inputTokens / maxTokens) * TRACK_HEIGHT}px` }}
                  />
                  <div
                    className="w-full rounded-t bg-accent/60"
                    style={{ height: `${(d.outputTokens / maxTokens) * TRACK_HEIGHT}px` }}
                  />
                </div>
              </DayColumn>
            ))}
          </div>
        </div>
        <div className="flex items-center justify-center gap-6 mt-4 text-xs text-text-muted">
          <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-purple-400/60" /> Input</span>
          <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-accent/60" /> Output</span>
        </div>
      </article>

      <article className="card p-6">
        <ChartHeader title="Estimated Cost" subtitle={`Total cost, last ${daily.length} days`} />
        <div className="flex gap-1">
          <YAxis max={maxCost} format={costFormat} />
          <div className="flex-1 flex items-end justify-between gap-1">
            {daily.map((d, i) => (
              <DayColumn key={d.date} label={weekdayLabel(d.date)}>
                <div
                  className={`w-full max-w-[32px] rounded-t ${i === daily.length - 1 ? "bg-amber-400/40" : "bg-amber-400/20"}`}
                  style={{ height: `${barHeight(Number(d.cost), maxCost)}px` }}
                />
              </DayColumn>
            ))}
          </div>
        </div>
      </article>

      <article className="card p-6">
        <ChartHeader title="Success vs Error" subtitle={`Total request outcomes, last ${daily.length} days`} />
        <div className="flex items-center justify-center h-48 sm:h-56">
          <div className="relative h-40 w-40">
            <svg viewBox="0 0 36 36" className="h-full w-full -rotate-90">
              <circle cx="18" cy="18" r="15.9" fill="none" stroke="var(--bg-elevated)" strokeWidth="3" />
              <circle
                cx="18" cy="18" r="15.9" fill="none" stroke="var(--success)" strokeWidth="3"
                strokeDasharray={`${successRate} ${100 - successRate}`} strokeLinecap="round"
              />
              <circle
                cx="18" cy="18" r="12.5" fill="none" stroke="var(--error)" strokeWidth="3"
                strokeDasharray={`${errorDash} ${100 - errorDash}`} strokeDashoffset={`-${successRate}`} strokeLinecap="round"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-2xl font-bold text-text-primary">{successRate.toFixed(1)}%</span>
              <span className="text-xs text-text-muted">Success rate</span>
            </div>
          </div>
        </div>
        <div className="flex items-center justify-center gap-6 mt-2 text-xs text-text-muted">
          <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-success" /> Success ({totalSuccess.toLocaleString()})</span>
          <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-error" /> Error ({totalFailed.toLocaleString()})</span>
        </div>
      </article>
    </div>
  );
}
