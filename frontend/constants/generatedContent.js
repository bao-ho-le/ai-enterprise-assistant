// Mirrors backend enums: GeneratedDocumentType, GenerationStatus (generated/enums).

export const GENERATED_DOCUMENT_TYPES = [
  { value: "EMAIL", label: "Email" },
  { value: "REPORT", label: "Report" },
  { value: "MEETING_MINUTES", label: "Meeting Minutes" },
  { value: "FORM", label: "Form" },
  { value: "SUMMARY", label: "Summary" },
];

export function generatedDocumentTypeLabel(value) {
  const t = GENERATED_DOCUMENT_TYPES.find((x) => x.value === value);
  return t ? t.label : value || "—";
}

// GenerationRun.status
export const GENERATION_STATUS = {
  PENDING: { label: "PENDING", badge: "badge-neutral" },
  RUNNING: { label: "RUNNING", badge: "badge-warning" },
  COMPLETED: { label: "COMPLETED", badge: "badge-success" },
  CANCELLED: { label: "CANCELLED", badge: "badge-neutral" },
  FAILED: { label: "FAILED", badge: "badge-error" },
};

export function generationStatusBadge(value) {
  return GENERATION_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}
