// Mirrors backend ai/usage enums: ConversationType, AIUsageStatus.

export const CONVERSATION_TYPES = [
  { value: "EMAIL_GENERATION", label: "Write Email" },
  { value: "REPORT_GENERATION", label: "Write Report" },
  { value: "MEETING_MINUTES_GENERATION", label: "Meeting Minutes" },
  { value: "SUMMARY_GENERATION", label: "Summary" },
  { value: "FORM_GENERATION", label: "Form Generation" },
  { value: "DOCUMENT_QA", label: "Document QA" },
  { value: "SEMANTIC_SEARCH", label: "Semantic Search" },
  { value: "DOCUMENT_INDEXING", label: "Document Indexing" },
];

export function conversationTypeLabel(value) {
  const t = CONVERSATION_TYPES.find((x) => x.value === value);
  return t ? t.label : value || "—";
}

// AIUsageLog.status -> badge class ("Status" column + filter)
export const STATUS_OPTIONS = [
  { value: "", label: "All" },
  { value: "SUCCESS", label: "Success" },
  { value: "FAILED", label: "Failed" },
];

export function statusBadge(value) {
  return value === "SUCCESS"
    ? { label: "Success", badge: "badge-success" }
    : { label: "Failed", badge: "badge-error" };
}
