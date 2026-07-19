import { Zap, Calendar, Hash, DollarSign, MoreHorizontal } from "lucide-react";

export const metadata = {
  title: "AI Usage Dashboard — Enterprise AI Assistant",
};

const KPIS = [
  { Icon: Zap, iconColor: "text-accent", trend: "+12.4%", trendClass: "badge-success", label: "Total Requests", value: "48,291", sub: "All time · Since Jan 2026" },
  { Icon: Calendar, iconColor: "text-emerald-400", trend: "+8.2%", trendClass: "badge-success", label: "Today's Requests", value: "342", sub: "Jul 5, 2026 · Updated 10:45 AM" },
  { Icon: Hash, iconColor: "text-purple-400", trend: "+15.1%", trendClass: "badge-success", label: "Total Tokens", value: "12.4M", sub: "8.2M input · 4.2M output" },
  { Icon: DollarSign, iconColor: "text-amber-400", trend: "+6.8%", trendClass: "badge-warning", label: "Estimated Cost", value: "$1,847", sub: "Month to date · July 2026" },
];

const REQUEST_BARS = [
  { d: "Sat", h: 55 }, { d: "Sun", h: 40 }, { d: "Mon", h: 72 }, { d: "Tue", h: 65 },
  { d: "Wed", h: 80 }, { d: "Thu", h: 90 }, { d: "Fri", h: 100, active: true },
];

const TOKEN_BARS = [
  { d: "Mon", input: 60, output: 40 }, { d: "Tue", input: 72, output: 48 },
  { d: "Wed", input: 55, output: 35 }, { d: "Thu", input: 85, output: 55 },
  { d: "Fri", input: 95, output: 62 },
];

const COST_BARS = [
  { d: "Sat", h: 45 }, { d: "Sun", h: 30 }, { d: "Mon", h: 58 }, { d: "Tue", h: 52 },
  { d: "Wed", h: 68 }, { d: "Thu", h: 75 }, { d: "Fri", h: 82, active: true },
];

const ROWS = [
  { time: "10:32 AM", type: "Document QA", model: "GPT-4o", input: "2,840", output: "612", total: "3,452", cost: "$0.042", ok: true },
  { time: "10:18 AM", type: "Write Email", model: "Claude Sonnet", input: "1,205", output: "890", total: "2,095", cost: "$0.028", ok: true },
  { time: "09:54 AM", type: "Summary", model: "GPT-4o", input: "4,120", output: "1,340", total: "5,460", cost: "$0.068", ok: true },
  { time: "09:41 AM", type: "Write Report", model: "Claude Sonnet", input: "6,780", output: "2,410", total: "9,190", cost: "$0.112", ok: true },
  { time: "09:12 AM", type: "Document QA", model: "Gemini Pro", input: "3,200", output: "0", total: "3,200", cost: "$0.019", ok: false },
  { time: "08:45 AM", type: "Write Email", model: "GPT-4o", input: "980", output: "520", total: "1,500", cost: "$0.018", ok: true },
];

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

export default function AIUsageDashboardPage() {
  return (
    <main className="flex-1 mx-auto w-full max-w-[1440px] px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">AI Usage Dashboard</h1>
        <p className="mt-2 text-text-secondary">
          Monitor token consumption, costs, and model performance across your organization.
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {KPIS.map(({ Icon, iconColor, trend, trendClass, label, value, sub }) => (
          <article key={label} className="card p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-bg-elevated border border-border-subtle">
                <Icon className={`h-5 w-5 ${iconColor}`} />
              </div>
              <span className={`badge ${trendClass}`}>{trend}</span>
            </div>
            <p className="text-sm text-text-muted mb-1">{label}</p>
            <p className="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">{value}</p>
            <p className="text-xs text-text-muted mt-2">{sub}</p>
          </article>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <article className="card p-6">
          <ChartHeader title="Request Trend" subtitle="Total requests this week" />
          <div className="relative h-48 sm:h-56 flex items-end justify-between gap-1 px-2">
            {REQUEST_BARS.map((b) => (
              <div key={b.d} className="flex-1 flex flex-col items-center gap-2">
                <div className={`w-full max-w-[32px] rounded-t ${b.active ? "bg-accent" : "bg-accent/20"}`} style={{ height: `${b.h}%` }} />
                <span className="text-[10px] text-text-muted">{b.d}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="card p-6">
          <ChartHeader title="Token Usage" subtitle="Total input and output tokens this week" />
          <div className="relative h-48 sm:h-56 flex items-end justify-between gap-2 px-2">
            {TOKEN_BARS.map((b) => (
              <div key={b.d} className="flex-1 flex flex-col items-center gap-1">
                <div className="w-full flex flex-col gap-0.5 items-center">
                  <div className="w-full max-w-[28px] rounded-t bg-purple-400/40" style={{ height: `${b.input}px` }} />
                  <div className="w-full max-w-[28px] rounded-t bg-accent/60" style={{ height: `${b.output}px` }} />
                </div>
                <span className="text-[10px] text-text-muted mt-1">{b.d}</span>
              </div>
            ))}
          </div>
          <div className="flex items-center justify-center gap-6 mt-4 text-xs text-text-muted">
            <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-purple-400/60" /> Input</span>
            <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-accent/60" /> Output</span>
          </div>
        </article>

        <article className="card p-6">
          <ChartHeader title="Estimated Cost" subtitle="Total cost this week" />
          <div className="relative h-48 sm:h-56 flex items-end justify-between gap-1 px-2">
            {COST_BARS.map((b) => (
              <div key={b.d} className="flex-1 flex flex-col items-center gap-2">
                <div className={`w-full max-w-[32px] rounded-t ${b.active ? "bg-amber-400/40" : "bg-amber-400/20"}`} style={{ height: `${b.h}%` }} />
                <span className="text-[10px] text-text-muted">{b.d}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="card p-6">
          <ChartHeader title="Success vs Error" subtitle="Total request outcomes this week" />
          <div className="flex items-center justify-center h-48 sm:h-56">
            <div className="relative h-40 w-40">
              <svg viewBox="0 0 36 36" className="h-full w-full -rotate-90">
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="var(--bg-elevated)" strokeWidth="3" />
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="var(--success)" strokeWidth="3" strokeDasharray="96 100" strokeLinecap="round" />
                <circle cx="18" cy="18" r="12.5" fill="none" stroke="var(--error)" strokeWidth="3" strokeDasharray="2 100" strokeDashoffset="-96" strokeLinecap="round" />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-2xl font-bold text-text-primary">98.2%</span>
                <span className="text-xs text-text-muted">Success rate</span>
              </div>
            </div>
          </div>
          <div className="flex items-center justify-center gap-6 mt-2 text-xs text-text-muted">
            <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-success" /> Success (1,682)</span>
            <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-error" /> Error (31)</span>
          </div>
        </article>
      </div>

      {/* Filters */}
      <div className="card p-4 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <label className="text-sm text-text-muted whitespace-nowrap">From</label>
            <input type="date" className="input-field w-auto text-sm py-2" defaultValue="2026-07-05" />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-text-muted whitespace-nowrap">To</label>
            <input type="date" className="input-field w-auto text-sm py-2" defaultValue="2026-07-05" />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-text-muted whitespace-nowrap">Conversation Type</label>
            <select className="select-field w-auto min-w-[140px] text-sm py-2" defaultValue="">
              <option value="">All Types</option>
              <option value="email">Write Email</option>
              <option value="report">Write Report</option>
              <option value="summary">Summary</option>
              <option value="qa">Document QA</option>
            </select>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-text-muted whitespace-nowrap">Model</label>
            <select className="select-field w-auto min-w-[140px] text-sm py-2" defaultValue="">
              <option value="">All Models</option>
              <option value="gpt-4o">GPT-4o</option>
              <option value="claude-sonnet">Claude Sonnet</option>
              <option value="gemini-pro">Gemini Pro</option>
            </select>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-text-muted whitespace-nowrap">Status</label>
            <select className="select-field w-auto min-w-[120px] text-sm py-2" defaultValue="">
              <option value="">All</option>
              <option value="success">Success</option>
              <option value="error">Error</option>
            </select>
          </div>
          <button type="button" className="btn-secondary text-sm ml-auto">Export CSV</button>
        </div>
      </div>

      {/* Recent Usage Table */}
      <div className="card overflow-hidden">
        <div className="px-4 py-3 border-b border-border-subtle bg-bg-secondary">
          <h2 className="text-sm font-semibold text-text-primary">Filter Result</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px]">
            <thead>
              <tr className="border-b border-border-subtle">
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Time</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Conversation Type</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Model</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">Input Tokens</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">Output Tokens</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">Total Tokens</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">Est. Cost</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Status</th>
              </tr>
            </thead>
            <tbody>
              {ROWS.map((r, i) => (
                <tr key={i} className="border-b border-border-subtle hover:bg-bg-elevated/50 transition-colors">
                  <td className="px-4 py-3 text-sm text-text-secondary whitespace-nowrap">{r.time}</td>
                  <td className="px-4 py-3"><span className="badge badge-neutral">{r.type}</span></td>
                  <td className="px-4 py-3 text-sm text-text-secondary">{r.model}</td>
                  <td className="px-4 py-3 text-sm text-text-secondary text-right">{r.input}</td>
                  <td className="px-4 py-3 text-sm text-text-secondary text-right">{r.output}</td>
                  <td className="px-4 py-3 text-sm text-text-primary text-right font-medium">{r.total}</td>
                  <td className="px-4 py-3 text-sm text-text-secondary text-right">{r.cost}</td>
                  <td className="px-4 py-3">
                    <span className={`badge ${r.ok ? "badge-success" : "badge-error"}`}>{r.ok ? "Success" : "Error"}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  );
}
