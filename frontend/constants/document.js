// Mirrors backend enums: DocumentType, VersionStatus, sort options.

export const DOCUMENT_TYPES = [
  { value: "EMAIL_TEMPLATE", label: "Email Template" },
  { value: "REPORT", label: "Report" },
  { value: "MEETING_MINUTES", label: "Meeting Minutes" },
  { value: "CONTRACT", label: "Contract" },
  { value: "FORM", label: "Form" },
  { value: "OTHER", label: "Other" },
];

export function documentTypeLabel(value) {
  const t = DOCUMENT_TYPES.find((x) => x.value === value);
  return t ? t.label : value || "—";
}

// DocumentVersion.status -> badge class + label ("Processing" column)
export const VERSION_STATUS = {
  PENDING: { label: "PENDING", badge: "badge-warning" },
  PROCESSING: { label: "PROCESSING", badge: "badge-warning" },
  READY: { label: "READY", badge: "badge-success" },
  FAILED: { label: "FAILED", badge: "badge-error" },
};

export function versionStatusBadge(value) {
  return VERSION_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}

// Document.status -> badge class + label ("Status" column) — independent from
// version processing status, never conflate the two.
export const DOCUMENT_STATUS = {
  ACTIVE: { label: "ACTIVE", badge: "badge-success" },
  DELETED: { label: "DELETED", badge: "badge-error" },
};

export function documentStatusBadge(value) {
  return DOCUMENT_STATUS[value] || { label: value || "—", badge: "badge-neutral" };
}

// "Status" filter (Document.status) — no "All" option, defaults to ACTIVE.
export const DOCUMENT_STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Active" },
  { value: "DELETED", label: "Deleted" },
];

// "Processing" filter (DocumentVersion.status)
export const STATUS_OPTIONS = [
  { value: "", label: "All" },
  { value: "READY", label: "Ready" },
  { value: "PROCESSING", label: "Processing" },
  { value: "PENDING", label: "Pending" },
  { value: "FAILED", label: "Failed" },
];

export const SORT_OPTIONS = [
  { value: "newest", label: "Newest" },
  { value: "oldest", label: "Oldest" },
];

export const EXTENSION_OPTIONS = [
  { value: "", label: "All" },
  { value: "pdf", label: "PDF" },
  { value: "docx", label: "DOCX" },
  { value: "xlsx", label: "XLSX" },
  { value: "txt", label: "TXT" },
];

// Accept attribute + allowed MIME types for upload (matches backend DocumentHelper).
export const UPLOAD_ACCEPT =
  ".pdf,.doc,.docx,.xls,.xlsx,.txt";

// Semantic search score (0..1) buckets — mirrors the "Similarity" filter select.
const SIMILARITY_THRESHOLDS = { high: 0.9, medium: 0.7 };

export function matchesSimilarityBucket(score, bucket) {
  if (!bucket) return true;
  if (bucket === "high") return score >= SIMILARITY_THRESHOLDS.high;
  if (bucket === "medium") return score >= SIMILARITY_THRESHOLDS.medium && score < SIMILARITY_THRESHOLDS.high;
  return score < SIMILARITY_THRESHOLDS.medium; // "low"
}
