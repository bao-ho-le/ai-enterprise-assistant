import { Zap, Hash, DollarSign, CheckCircle2 } from "lucide-react";

function formatCost(value) {
  return `$${Number(value || 0).toFixed(4)}`;
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString();
}

function formatRate(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

export default function AIUsageSummaryCards({ summary }) {
  const cards = [
    {
      Icon: Zap,
      iconColor: "text-accent",
      label: "Today's Requests",
      value: formatNumber(summary?.todayRequest),
      sub: `Last 7 days: ${formatNumber(summary?.last7DayRequests)}`,
    },
    {
      Icon: Hash,
      iconColor: "text-purple-400",
      label: "Today's Tokens",
      value: formatNumber(summary?.todayToken),
      sub: `Last 7 days: ${formatNumber(summary?.last7DayTokens)}`,
    },
    {
      Icon: DollarSign,
      iconColor: "text-amber-400",
      label: "Today's Est. Cost",
      value: formatCost(summary?.todayCost),
      sub: `Last 7 days: ${formatCost(summary?.last7DayCost)}`,
    },
    {
      Icon: CheckCircle2,
      iconColor: "text-emerald-400",
      label: "Today's Success Rate",
      value: formatRate(summary?.todaySuccessRate),
      sub: `Last 7 days: ${formatRate(summary?.last7DaySuccessRate)}`,
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      {cards.map(({ Icon, iconColor, label, value, sub }) => (
        <article key={label} className="card p-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-bg-elevated border border-border-subtle">
              <Icon className={`h-5 w-5 ${iconColor}`} />
            </div>
          </div>
          <p className="text-sm text-text-muted mb-1">{label}</p>
          <p className="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">{value}</p>
          <p className="text-xs text-text-muted mt-2">{sub}</p>
        </article>
      ))}
    </div>
  );
}
