"use client";

import { ROUTED_CONVERSATION_TYPES } from "@/constants/conversation";

// The sidebar history filter, extracted so the deleted-conversations page reuses the
// exact same control. `allowAll` adds an empty-value option for screens that can list
// every type at once (the sidebar itself always sits on one concrete type).
export default function ConversationTypeFilter({
  value,
  onChange,
  allowAll = false,
  className = "select-field-ghost-xs",
  ariaLabel = "Filter conversation history by type",
}) {
  return (
    <select
      className={className}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={ariaLabel}
    >
      {allowAll && <option value="">All types</option>}
      {ROUTED_CONVERSATION_TYPES.map((t) => (
        <option key={t.value} value={t.value}>
          {t.label}
        </option>
      ))}
    </select>
  );
}
