import { SquarePen } from "lucide-react";

const TODAY = [
  { label: "Q4 Client Update Email", active: true },
  { label: "Board Meeting Summary" },
  { label: "Compliance Policy Q&A" },
];

const EARLIER = [
  "Financial Report — Nov 2025",
  "Sprint Retrospective Notes",
  "Vendor Onboarding Email",
];

export default function LeftSidebar() {
  return (
    <aside className="hidden md:flex w-64 shrink-0 flex-col border-r border-border-subtle bg-bg-secondary">
      <div className="flex h-14 items-center gap-2 px-4 transition-colors hover:bg-bg-elevated mb-2">
        <SquarePen className="h-5 w-5 text-text-secondary" />
        <span className="text-sm font-medium text-text-secondary">
          New Conversations
        </span>
        <button type="button" className="btn-ghost ml-auto p-1.5" aria-label="New conversation" />
      </div>

      <div className="flex-1 overflow-y-auto p-3">
        <div className="mb-4">
          <p className="px-2 mb-2 text-sm font-bold tracking-wider text-text-primary">
            Today
          </p>
          <ul className="space-y-0.5">
            {TODAY.map((item) => (
              <li key={item.label}>
                <a
                  href="#"
                  className={
                    item.active
                      ? "block rounded-lg px-2.5 py-2.5 bg-bg-elevated border border-border-subtle transition-colors hover:border-border-default"
                      : "block rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
                  }
                >
                  <p
                    className={
                      item.active
                        ? "truncate text-sm font-medium text-text-primary"
                        : "truncate text-sm text-text-secondary"
                    }
                  >
                    {item.label}
                  </p>
                </a>
              </li>
            ))}
          </ul>

          <ul className="space-y-0.5">
            {EARLIER.map((label) => (
              <li key={label}>
                <a
                  href="#"
                  className="block rounded-lg px-2.5 py-2.5 transition-colors hover:bg-bg-elevated"
                >
                  <p className="truncate text-sm text-text-secondary">{label}</p>
                </a>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </aside>
  );
}
