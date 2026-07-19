import { Upload, Search, FileText, FileSpreadsheet, MoreVertical } from "lucide-react";

const ATTACHED = [
  { name: "Q4-Financial-Report.pdf", Icon: FileText, iconBg: "bg-red-500/10", iconColor: "text-red-400" },
  { name: "Revenue-Forecast-2026.xlsx", Icon: FileSpreadsheet, iconBg: "bg-blue-500/10", iconColor: "text-blue-400" },
  { name: "Board-Meeting-Minutes.docx", Icon: FileText, iconBg: "bg-emerald-500/10", iconColor: "text-emerald-400" },
];

export default function RightDocumentPanel() {
  return (
    <aside className="hidden xl:flex w-80 shrink-0 flex-col border-l border-border-subtle bg-bg-secondary">
      <div className="flex h-14 items-center justify-between p-4">
        <button
          type="button"
          className="btn-secondary w-full text-sm transition-colors hover:bg-bg-elevated mt-4"
        >
          <Upload className="h-4 w-4" />
          Upload Document
        </button>
      </div>

      <div className="p-4 border-b border-border-subtle">
        <div className="relative">
          <input
            type="search"
            placeholder="Search documents..."
            className="input-field h-10 w-full pl-3 pr-10 text-sm"
          />
          <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
            <Search className="h-4 w-4 text-text-muted" />
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <p className="mb-3 text-xs font-medium uppercase tracking-wider text-text-muted">
          Attached ({ATTACHED.length})
        </p>
        <ul className="space-y-2">
          {ATTACHED.map(({ name, Icon, iconBg, iconColor }) => (
            <li key={name} className="card flex items-center gap-3 p-3">
              <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${iconBg}`}>
                <Icon className={`h-4 w-4 ${iconColor}`} />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-text-primary">{name}</p>
              </div>
              <MoreVertical className="h-4 w-4 text-text-muted" />
              <button type="button" className="btn-ghost p-1" aria-label="Remove document">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-accent cursor-pointer rounded-full"
                  aria-label="Select document"
                />
              </button>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}
