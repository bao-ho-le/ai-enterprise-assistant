// Single source of truth for the generation form schemas, moved out of the old
// GenerationForm so the unified composer's options sheet and the "regenerate" panel
// both render exactly the same fields (nothing was dropped or renamed).
//
// Field schemas must match the backend's XxxGenerationInput DTOs
// (ai/knowledge/generation/dto). Pure module (no imports) — unit-testable.
//
// `row` groups adjacent fields into one horizontal row (e.g. Sender/Recipient side by side).

export const LANGUAGE_OPTIONS = ["English", "Vietnamese", "Spanish", "French"];

// Shared with backend PromptBuilderService's toneInstruction()/audienceInstruction() —
// each value maps to a detailed prompt instruction there, not just passed through as-is.
export const TONE_OPTIONS = ["Professional", "Formal", "Friendly", "Persuasive", "Apologetic"];
export const AUDIENCE_OPTIONS = ["General Audience", "Internal Team", "Business Partner", "Customer", "Executive"];
export const REPORT_AUDIENCE_OPTIONS = [
  "General Audience",
  "Internal Team",
  "Business Partner",
  "Customer",
  "Executive Leadership",
  "Board of Directors",
];

export const GENERATION_FIELD_CONFIGS = {
  EMAIL_GENERATION: [
    { name: "purpose", label: "Purpose", type: "textarea", required: true, placeholder: "What should this email accomplish?" },
    { name: "sender", label: "Sender", type: "text", placeholder: "e.g. you@company.com", row: "contacts" },
    { name: "recipient", label: "Recipient", type: "text", placeholder: "e.g. sarah@acmecorp.com", row: "contacts" },
    { name: "optionalContext", label: "Optional Context", type: "textarea", placeholder: "Prior thread context, requests already made, etc." },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS, default: "English", row: "details" },
    { name: "length", label: "Email Length", type: "select", options: ["Short", "Medium", "Long"], default: "Medium", row: "details" },
    { name: "audience", label: "Audience", type: "select", options: AUDIENCE_OPTIONS, default: "General Audience", row: "details" },
    { name: "tone", label: "Tone", type: "select", options: TONE_OPTIONS, default: "Professional", row: "details" },
  ],
  REPORT_GENERATION: [
    { name: "title", label: "Report title", type: "text", required: true, placeholder: "e.g. Q3 Sales Performance" },
    { name: "fromDate", label: "From date", type: "date", row: "period" },
    { name: "toDate", label: "To date", type: "date", row: "period" },
    { name: "instructions", label: "Additional Context", type: "textarea", placeholder: "Focus areas or additional instructions" },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS, default: "English", row: "details" },
    { name: "length", label: "Length", type: "select", options: ["Short", "Medium", "Long"], default: "Medium", row: "details" },
    { name: "audience", label: "Audience", type: "select", options: REPORT_AUDIENCE_OPTIONS, default: "General Audience", row: "details" },
  ],
  SUMMARY_GENERATION: [
    {
      name: "style",
      label: "Summary Type",
      type: "cards",
      required: true,
      default: "PARAGRAPH",
      options: [
        { value: "PARAGRAPH", title: "Paragraph", description: "Natural, connected paragraphs" },
        { value: "BULLET_POINTS", title: "Bullet Points", description: "One main idea per bullet" },
        { value: "STRUCTURED", title: "Structured", description: "Organized under headings (Overview, Key Findings, ...)" },
      ],
    },
    { name: "instructions", label: "Instructions", type: "textarea", placeholder: "Anything specific to focus the summary on" },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS, default: "English", row: "details" },
    { name: "length", label: "Length", type: "select", options: ["Short", "Medium", "Long"], default: "Medium", row: "details" },
  ],
};

// Which inputData field the composer prompt fills for each mode. Email/report/summary
// keep every other option editable in the options sheet; the prompt is the primary
// instruction (purpose / title / focus) users type in the ChatGPT-like composer.
export const PROMPT_FIELD = {
  EMAIL_GENERATION: "purpose",
  REPORT_GENERATION: "title",
  SUMMARY_GENERATION: "instructions",
};

export function generationFields(conversationType) {
  return GENERATION_FIELD_CONFIGS[conversationType] || [];
}

// How the fields are grouped in the creation form (visible immediately, not hidden in a
// modal). Every field of a type must appear in exactly one section — enforced by a unit
// test — so the UI can never silently drop an option.
export const GENERATION_FORM_SECTIONS = {
  EMAIL_GENERATION: [
    {
      id: "details",
      title: "Email details",
      fields: ["purpose", "sender", "recipient"],
    },
    {
      id: "preferences",
      title: "Writing preferences",
      fields: ["language", "length", "audience", "tone"],
    },
    {
      id: "context",
      title: "Additional context",
      fields: ["optionalContext"],
    },
  ],
  REPORT_GENERATION: [
    {
      id: "details",
      title: "Report details",
      fields: ["title", "fromDate", "toDate"],
    },
    {
      id: "preferences",
      title: "Writing preferences",
      fields: ["language", "length", "audience"],
    },
    {
      id: "instructions",
      title: "Additional instructions",
      fields: ["instructions"],
    },
  ],
  SUMMARY_GENERATION: [
    {
      id: "style",
      title: "Summary format",
      fields: ["style"],
    },
    {
      id: "preferences",
      title: "Writing preferences",
      fields: ["language", "length"],
    },
    {
      id: "instructions",
      title: "Additional instructions",
      fields: ["instructions"],
    },
  ],
};

// Field objects for each section, in the order declared above. Unknown field names are
// ignored and empty sections are dropped, so the form is always consistent with the
// schema even if a section config drifts.
export function generationFormSections(conversationType) {
  const fields = generationFields(conversationType);
  const byName = new Map(fields.map((field) => [field.name, field]));
  const sections = GENERATION_FORM_SECTIONS[conversationType] || [];
  return sections
    .map((section) => ({
      ...section,
      fields: section.fields.map((name) => byName.get(name)).filter(Boolean),
    }))
    .filter((section) => section.fields.length > 0);
}

export function initialGenerationValues(conversationType, initialValues) {
  const state = {};
  for (const field of generationFields(conversationType)) {
    let value = initialValues?.[field.name] ?? field.default ?? "";
    // Legacy safety: old conversations may have stored option values that are no
    // longer valid (e.g. an older summary style) — fall back to the default so the
    // field never renders/submits a stale choice when regenerating.
    if (field.type === "select" && field.options && !field.options.includes(value)) {
      value = field.default ?? "";
    }
    if (field.type === "cards" && field.options && !field.options.some((option) => option.value === value)) {
      value = field.default ?? "";
    }
    state[field.name] = value;
  }
  return state;
}

export function validateGenerationValues(conversationType, values, prompt) {
  const missing = [];
  for (const field of generationFields(conversationType)) {
    if (!field.required) continue;
    const value = String(values?.[field.name] ?? "").trim();
    // The composer prompt stands in for the primary required field (purpose/title),
    // exactly as buildGenerationInput fills it in.
    const promptCoversField =
      PROMPT_FIELD[conversationType] === field.name && Boolean(String(prompt ?? "").trim());
    if (!value && !promptCoversField) missing.push(field.label);
  }
  return missing;
}

// Compose the exact inputData payload the backend DTOs expect, with the composer prompt
// applied to the primary field. Report keeps an explicitly configured title (options
// sheet) and only falls back to the prompt when it is empty.
export function buildGenerationInput(conversationType, values, prompt) {
  const input = {};
  for (const field of generationFields(conversationType)) {
    input[field.name] = values?.[field.name] ?? field.default ?? "";
  }

  const promptField = PROMPT_FIELD[conversationType];
  const trimmedPrompt = String(prompt ?? "").trim();
  if (promptField && trimmedPrompt) {
    if (conversationType === "REPORT_GENERATION") {
      if (!String(input.title ?? "").trim()) input.title = trimmedPrompt;
    } else {
      input[promptField] = trimmedPrompt;
    }
  }
  return input;
}

// "3 options changed" hint for the composer's options button — purely presentational.
export function countCustomisedOptions(conversationType, values) {
  let count = 0;
  for (const field of generationFields(conversationType)) {
    const value = values?.[field.name];
    const baseline = field.default ?? "";
    if (value !== undefined && value !== null && String(value) !== String(baseline) && String(value).trim() !== "") {
      count += 1;
    }
  }
  return count;
}
