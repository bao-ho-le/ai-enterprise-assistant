// Mirrors backend enums: ConversationType (ai/usage/enums), ConversationStatus,
// AIMessageRole (ai/conversation/enums).

export const CONVERSATION_TYPES = [
  { value: "EMAIL_GENERATION", label: "Email Generation" },
  { value: "REPORT_GENERATION", label: "Report Generation" },
  { value: "MEETING_MINUTES_GENERATION", label: "Meeting Minutes Generation" },
  { value: "SUMMARY_GENERATION", label: "Summary Generation" },
  { value: "FORM_GENERATION", label: "Form Generation" },
  { value: "DOCUMENT_QA", label: "Document QA" },
  { value: "SEMANTIC_SEARCH", label: "Semantic Search" },
  { value: "DOCUMENT_INDEXING", label: "Document Indexing" },
];

// conversationType values that back a generation flow (GenerationRun-backed
// conversations) — mirrors AIConversationHelper.GENERATION_CONVERSATION_TYPES.
export const GENERATION_CONVERSATION_TYPES = [
  "EMAIL_GENERATION",
  "REPORT_GENERATION",
  "SUMMARY_GENERATION",
  "MEETING_MINUTES_GENERATION",
  "FORM_GENERATION",
];

export function conversationTypeLabel(value) {
  const t = CONVERSATION_TYPES.find((x) => x.value === value);
  return t ? t.label : value || "—";
}

export function isGenerationConversationType(value) {
  return GENERATION_CONVERSATION_TYPES.includes(value);
}

// AIConversation.status
export const CONVERSATION_STATUS = {
  ACTIVE: { label: "ACTIVE", badge: "badge-success" },
  DELETED: { label: "DELETED", badge: "badge-error" },
};

export function conversationStatusBadge(value) {
  return CONVERSATION_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}

// AIMessage.role
export const AI_MESSAGE_ROLES = {
  USER: "USER",
  ASSISTANT: "ASSISTANT",
  SYSTEM: "SYSTEM",
};
