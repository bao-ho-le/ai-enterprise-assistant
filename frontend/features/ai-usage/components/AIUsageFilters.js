import DatePicker from "@/components/ui/DatePicker";
import { CONVERSATION_TYPES, STATUS_OPTIONS } from "@/constants/aiUsage";

// Controlled: `filters` object + onChange(patch). `models` is the distinct
// model list fetched from the backend (GET /ai-usage/models) — only models
// that have actually been logged show up, no free-text entry.
export default function AIUsageFilters({ filters, onChange, models }) {
  const set = (key) => (e) => onChange({ [key]: e.target.value });
  const setValue = (key) => (value) => onChange({ [key]: value });

  return (
    <div className="card p-4 mb-6">
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <label className="text-sm text-text-muted whitespace-nowrap">From</label>
          <DatePicker value={filters.fromDate} onChange={setValue("fromDate")} />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-text-muted whitespace-nowrap">To</label>
          <DatePicker value={filters.toDate} onChange={setValue("toDate")} />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-text-muted whitespace-nowrap">Conversation Type</label>
          <select
            className="select-field w-auto min-w-[140px] text-sm py-2"
            value={filters.conversationType}
            onChange={set("conversationType")}
          >
            <option value="">All Types</option>
            {CONVERSATION_TYPES.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-text-muted whitespace-nowrap">Model</label>
          <select
            className="select-field w-auto min-w-[140px] text-sm py-2"
            value={filters.model}
            onChange={set("model")}
          >
            <option value="">All Models</option>
            {models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-text-muted whitespace-nowrap">Status</label>
          <select
            className="select-field w-auto min-w-[120px] text-sm py-2"
            value={filters.status}
            onChange={set("status")}
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}
