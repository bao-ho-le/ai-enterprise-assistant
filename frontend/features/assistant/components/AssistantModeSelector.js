"use client";

import { Check, FileBarChart, ListTree, Mail, MessagesSquare, Sparkles } from "lucide-react";

export const MODE_ICONS = {
  qa: MessagesSquare,
  email: Mail,
  report: FileBarChart,
  summary: ListTree,
};

// Mode switcher for the unified assistant.
//   variant="cards" — creation panel: icon + name + description, active state obvious.
//   variant="chips" — compact, used inside the conversation composer.
// Unavailable modes are filtered out by the caller (availableModes), so a user only ever
// sees tools their role actually grants; the backend still re-checks every request.
export default function AssistantModeSelector({
  modes,
  modeKey,
  onSelect,
  disabled = false,
  variant = "chips",
}) {
  if (!modes || modes.length === 0) return null;

  if (variant === "cards") {
    return (
      <div role="tablist" aria-label="Assistant mode" className="grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-4">
        {modes.map((mode) => {
          const active = mode.key === modeKey;
          const Icon = MODE_ICONS[mode.key] || Sparkles;
          return (
            <button
              key={mode.key}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => onSelect(mode.key)}
              disabled={disabled}
              className={`group flex h-full flex-col rounded-xl border p-3 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                active
                  ? "border-accent bg-accent/5 ring-1 ring-accent/30"
                  : "border-border-subtle bg-bg-primary hover:border-border-default"
              }`}
            >
              <span
                className={`mb-2 flex h-8 w-8 items-center justify-center rounded-lg ${
                  active ? "bg-accent/15 text-accent" : "bg-bg-elevated text-text-secondary"
                }`}
              >
                <Icon className="h-4 w-4" strokeWidth={1.75} />
              </span>
              <span className="flex items-center gap-1.5 text-sm font-medium text-text-primary">
                {mode.label}
                {active && <Check className="h-3.5 w-3.5 text-accent" strokeWidth={2.5} />}
              </span>
              <span className="mt-1 text-[11px] leading-snug text-text-muted">{mode.description}</span>
            </button>
          );
        })}
      </div>
    );
  }

  return (
    <div role="tablist" aria-label="Assistant mode" className="flex flex-wrap items-center gap-1.5">
      {modes.map((mode) => {
        const active = mode.key === modeKey;
        return (
          <button
            key={mode.key}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onSelect(mode.key)}
            disabled={disabled}
            title={mode.description}
            className={`rounded-full border px-3 py-1 text-xs transition-colors ${
              active
                ? "border-accent/50 bg-accent/5 text-text-primary font-medium"
                : "border-border-subtle text-text-secondary hover:border-border-default hover:text-text-primary"
            } disabled:opacity-40 disabled:cursor-not-allowed`}
          >
            {mode.label}
          </button>
        );
      })}
    </div>
  );
}
