// Mode catalog + composer validation rules for the unified Assistant workspace.
//
// Pure module (no imports) so the validation/permission/requirement rules can be
// unit-tested with `node --test` without a browser or Next.js path aliases.
//
// `permission` mirrors the backend: AIConversationServiceImpl.requireFeaturePermission
// enforces AI_DOCUMENT_QA for DOCUMENT_QA and AI_DOCUMENT_GENERATION for every
// generation type. This catalog only drives disabled/hidden UI — the backend stays the
// source of truth. `requiresDocuments` mirrors requiresSourceDocuments() in
// constants/conversation (REPORT_GENERATION + SUMMARY_GENERATION + DOCUMENT_QA).

export const ASSISTANT_MODES = [
  {
    key: "qa",
    label: "Ask",
    filterLabel: "Document QA",
    badge: "QA",
    description: "Ask questions and get answers from your documents",
    createLabel: "Ask question",
    conversationType: "DOCUMENT_QA",
    permission: "AI_DOCUMENT_QA",
    supportsMessages: true,
    requiresDocuments: true,
    promptField: null,
    placeholder: "Ask a question about your documents…",
    submitLabel: "Send",
    hint: "Answers are grounded in the attached source documents, with citations.",
  },
  {
    key: "email",
    label: "Email",
    filterLabel: "Email",
    badge: "EG",
    description: "Generate professional emails based on your instructions",
    createLabel: "Generate email",
    conversationType: "EMAIL_GENERATION",
    permission: "AI_DOCUMENT_GENERATION",
    supportsMessages: false,
    requiresDocuments: false,
    promptField: "purpose",
    placeholder: "What should this email accomplish?",
    submitLabel: "Generate email",
    hint: "Emails do not require source documents. Use Options for tone, length and audience.",
  },
  {
    key: "report",
    label: "Report",
    filterLabel: "Report",
    badge: "RG",
    description: "Create structured reports from selected documents",
    createLabel: "Generate report",
    conversationType: "REPORT_GENERATION",
    permission: "AI_DOCUMENT_GENERATION",
    supportsMessages: false,
    requiresDocuments: true,
    promptField: "title",
    placeholder: "Report title, e.g. Q3 Sales Performance",
    submitLabel: "Generate report",
    hint: "Reports are generated from at least one attached source document.",
  },
  {
    key: "summary",
    label: "Summary",
    filterLabel: "Summary",
    badge: "SG",
    description: "Summarize selected documents in your preferred format",
    createLabel: "Generate summary",
    conversationType: "SUMMARY_GENERATION",
    permission: "AI_DOCUMENT_GENERATION",
    supportsMessages: false,
    requiresDocuments: true,
    promptField: "instructions",
    placeholder: "What should the summary focus on? (optional)",
    submitLabel: "Generate summary",
    hint: "Summaries are generated from at least one attached source document.",
  },
];

export const DEFAULT_MODE_KEY = "qa";

export function modeByKey(key) {
  if (!key) return null;
  return ASSISTANT_MODES.find((mode) => mode.key === key) || null;
}

export function modeByConversationType(conversationType) {
  if (!conversationType) return null;
  return ASSISTANT_MODES.find((mode) => mode.conversationType === conversationType) || null;
}

// Same semantics as lib/permissions.hasPermission, taking the permission list directly
// so this module stays dependency-free (and testable).
export function modeAvailability(mode, permissions) {
  if (!mode) return false;
  if (!mode.permission) return true;
  return Array.isArray(permissions) && permissions.includes(mode.permission);
}

export function availableModes(permissions) {
  return ASSISTANT_MODES.filter((mode) => modeAvailability(mode, permissions));
}

// Resolves the mode a screen should show: the requested one when permitted, otherwise
// the first permitted mode, otherwise null (nothing to offer).
export function resolveMode(requestedKey, permissions) {
  const requested = modeByKey(requestedKey);
  if (modeAvailability(requested, permissions)) return requested;
  const permitted = availableModes(permissions);
  return permitted.length ? permitted[0] : null;
}

// Composer-side validation. Mirrors the checks the old screens performed locally before
// calling the API; the backend re-validates everything (field constraints + permissions).
//
// Prompt = the free-text question, which only Ask mode has. Generation modes render their
// own fields instead, and their required fields are reported through `missingOptionFields`
// (same rule the old GenerationForm applied: Generate stays disabled until they're filled).
export function validateComposer({ mode, prompt, documentCount = 0, missingOptionFields = [] }) {
  const errors = { prompt: null, documents: null, options: [] };
  if (!mode) {
    errors.prompt = "No assistant mode is available for your account.";
    return { ok: false, errors };
  }

  const trimmed = String(prompt ?? "").trim();
  if (mode.supportsMessages && !trimmed) {
    errors.prompt = "Enter a question.";
  }

  if (mode.requiresDocuments && documentCount === 0) {
    errors.documents =
      mode.key === "qa"
        ? "Attach at least one source document to ask questions."
        : "Attach at least one source document below before generating.";
  }

  errors.options = missingOptionFields;

  return {
    ok: !errors.prompt && !errors.documents && errors.options.length === 0,
    errors,
  };
}

// Single source of truth for the submit buttons: a request can only be sent when the
// current mode validates and nothing is already in flight (duplicate-submission guard).
export function canSubmitRequest({ validation, submitting = false }) {
  return Boolean(validation?.ok) && !submitting;
}

// Generation conversations cannot accept chat turns (backend rejects them with
// CONVERSATION_TYPE_NOT_CHAT). UI must show regeneration, never a chat input.
export function canSendMessages(conversationType) {
  const mode = modeByConversationType(conversationType);
  return Boolean(mode?.supportsMessages);
}

export function canRegenerate(conversationType) {
  const mode = modeByConversationType(conversationType);
  return Boolean(mode) && !mode.supportsMessages;
}
