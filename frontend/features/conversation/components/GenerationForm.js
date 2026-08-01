"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, Sparkles, AlertTriangle } from "lucide-react";
import { startGenerationConversation, triggerGeneration } from "@/services/conversationService";
import { getGeneratedContentById } from "@/services/generatedContentService";
import { conversationTypeLabel, requiresSourceDocuments } from "@/constants/conversation";
import { ApiError } from "@/lib/apiClient";
import DatePicker from "@/components/ui/DatePicker";
import EmailPreview from "./EmailPreview";

const LANGUAGE_OPTIONS = ["English", "Vietnamese", "Spanish", "French"];

// Shared with backend PromptBuilderService's toneInstruction()/audienceInstruction() —
// each value maps to a detailed prompt instruction there, not just passed through as-is.
const TONE_OPTIONS = ["Professional", "Formal", "Friendly", "Persuasive", "Apologetic"];
const AUDIENCE_OPTIONS = ["General Audience", "Internal Team", "Business Partner", "Customer", "Executive"];
const REPORT_AUDIENCE_OPTIONS = ["General Audience", "Internal Team", "Business Partner", "Customer", "Executive Leadership", "Board of Directors"];

// Field schema per generation conversationType — keeps the 3 wired generation
// screens on one component instead of near-duplicate forms. Must match the
// backend's XxxGenerationInput DTOs (ai/generation/dto).
// `row` groups adjacent fields into one horizontal row (e.g. Sender/Recipient side by side).
const FIELD_CONFIGS = {
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
    {
      name: "language",
      label: "Language",
      type: "select",
      options: LANGUAGE_OPTIONS,
      default: "English",
      row: "details",
    },
    {
      name: "length",
      label: "Length",
      type: "select",
      options: ["Short", "Medium", "Long"],
      default: "Medium",
      row: "details",
    },
    {
      name: "audience",
      label: "Audience",
      type: "select",
      options: REPORT_AUDIENCE_OPTIONS,
      default: "General Audience",
      row: "details",
    },
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

// Tailwind's scanner needs literal class names in source — can't interpolate `grid-cols-${n}`.
const ROW_GRID_COLS = { 2: "sm:grid-cols-2", 3: "sm:grid-cols-3", 4: "sm:grid-cols-4" };

// Override grid columns for specific row groups to match the 3-column width of
// adjacent detail rows — makes date/filter fields visually consistent.
function getGridClass(group) {
  if (group.fields.length < 2) return "";
  if (group.row === "period" || (group.row === "details" && group.fields.length === 2)) {
    return "sm:grid-cols-3";
  }
  return ROW_GRID_COLS[group.fields.length] || "sm:grid-cols-2";
}

function initialState(fields, initialValues) {
  const state = {};
  for (const f of fields) {
    let value = initialValues?.[f.name] ?? f.default ?? "";
    // Legacy safety: old conversations may have stored option values (e.g. report
    // "Writing Style", summary "Summary Type") that are no longer valid — fall back
    // to the default so the field never renders/submits a stale choice when regenerating.
    if (f.type === "select" && f.options && !f.options.includes(value)) {
      value = f.default ?? "";
    }
    if (f.type === "cards" && f.options && !f.options.some((o) => o.value === value)) {
      value = f.default ?? "";
    }
    state[f.name] = value;
  }
  return state;
}

// Consecutive fields sharing the same `row` render side by side in one grid.
function groupFields(fields) {
  const groups = [];
  for (const f of fields) {
    const last = groups[groups.length - 1];
    if (f.row && last?.row === f.row) {
      last.fields.push(f);
    } else {
      groups.push({ row: f.row || null, fields: [f] });
    }
  }
  return groups;
}

// Pulls the "Subject: ..." line FakeLLMService prepends to email content back out,
// so the preview can show it as its own field instead of the email's first line.
export function splitEmailContent(content) {
  const match = /^Subject:\s*(.*)\n+([\s\S]*)$/.exec(content || "");
  return match ? { subject: match[1].trim(), body: match[2].trim() } : { subject: "", body: content || "" };
}

function Field({ f, value, onChange, onCheckedChange }) {
  if (f.type === "checkbox") {
    return (
      <label htmlFor={`gen-${f.name}`} className="flex items-center gap-2">
        <input
          id={`gen-${f.name}`}
          type="checkbox"
          className="h-4 w-4 accent-accent"
          checked={Boolean(value)}
          onChange={onCheckedChange}
        />
        <span className="text-sm text-text-secondary">{f.label}</span>
      </label>
    );
  }

  if (f.type === "cards") {
    return (
      <div>
        <p className="label-text">
          {f.label}
          {f.required && <span className="text-error"> *</span>}
        </p>          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 auto-rows-fr">
          {f.options.map((opt) => {
            const selected = value === opt.value;
            return (
              <button
                type="button"
                key={opt.value}
                onClick={() => onChange(opt.value)}
                className={`h-full w-full rounded-lg border p-4 transition-colors flex items-center gap-4 ${
                  selected ? "border-accent/50 bg-accent/5" : "border-border-subtle hover:border-border-default"
                }`}
              >
                <span
                  className={`flex h-4 w-4 shrink-0 items-center justify-center rounded-full border ${
                    selected ? "border-accent" : "border-border-default"
                  }`}
                >
                  {selected && <span className="h-2 w-2 rounded-full bg-accent" />}
                </span>
                <div className="flex-1 flex flex-col justify-center min-w-0 text-left">
                  <span className="text-sm font-medium text-text-primary">{opt.title}</span>
                  <p className="text-xs text-text-muted mt-0.5">{opt.description}</p>
                </div>
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  if (f.type === "button-group") {
    return (
      <div>
        <p className="label-text">
          {f.label}
          {f.required && <span className="text-error"> *</span>}
        </p>
        <div className="flex flex-wrap gap-2">
          {f.options.map((o) => {
            const selected = value === o;
            return (
              <button
                type="button"
                key={o}
                onClick={() => onChange(o)}
                className={`rounded-full border px-4 py-1.5 text-sm transition-colors ${
                  selected
                    ? "border-accent/50 bg-accent/5 text-text-primary"
                    : "border-border-subtle text-text-secondary hover:border-border-default"
                }`}
              >
                {o}
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div>
      <label htmlFor={`gen-${f.name}`} className="label-text">
        {f.label}
        {f.required && <span className="text-error"> *</span>}
      </label>
      {f.type === "textarea" ? (
        <textarea
          id={`gen-${f.name}`}
          rows={7}
          className="textarea-field resize-none"
          placeholder={f.placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : f.type === "select" ? (
        <select id={`gen-${f.name}`} className="select-field w-full" value={value} onChange={(e) => onChange(e.target.value)}>
          {!f.required && !f.default && <option value="">—</option>}
          {f.options.map((o) => (
            <option key={o} value={o}>
              {o}
            </option>
          ))}
        </select>
      ) : f.type === "date" ? (
        <DatePicker value={value} onChange={onChange} />
      ) : (
        <input
          id={`gen-${f.name}`}
          type="text"
          className="input-field"
          placeholder={f.placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </div>
  );
}

// mode is implicit: pass conversationId to render in "regenerate" mode (prefilled,
// posts /generate on the existing conversation); omit it for "create" mode (creates
// the conversation with no title, then triggers the first generation run).
export default function GenerationForm({
  conversationType,
  basePath,
  conversationId,
  initialValues,
  onGenerated,
  selectedDocumentVersionIds,
  attachedDocumentCount,
  actions,
}) {
  const router = useRouter();
  const fields = FIELD_CONFIGS[conversationType] || [];
  const [values, setValues] = useState(() => initialState(fields, initialValues));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Email-only: created conversation gets remembered locally so re-submitting from
  // the inline preview regenerates in place instead of creating a second conversation.
  const [createdId, setCreatedId] = useState(null);
  const [preview, setPreview] = useState(null);

  const set = (name) => (value) => setValues((prev) => ({ ...prev, [name]: value }));
  const setChecked = (name) => (e) => setValues((prev) => ({ ...prev, [name]: e.target.checked }));

  const missingRequired = fields.some((f) => f.required && !String(values[f.name] || "").trim());

  // Report/Summary can't generate without a source document — mirrors the backend's
  // GENERATION_SOURCE_DOCUMENTS_REQUIRED so the user is stopped before the round trip.
  const documentCount = selectedDocumentVersionIds
    ? selectedDocumentVersionIds.length
    : attachedDocumentCount ?? 0;
  const missingDocuments = requiresSourceDocuments(conversationType) && documentCount === 0;

  const isEmailPreviewFlow = conversationType === "EMAIL_GENERATION" && !conversationId;

  const submit = async (e) => {
    e.preventDefault();
    if (missingRequired || missingDocuments || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      let id = conversationId || createdId;
      let result;
      if (!id) {
        const started = await startGenerationConversation(conversationType, selectedDocumentVersionIds, values);
        id = started.conversation.id;
        setCreatedId(id);
        result = started.generation;
      } else {
        result = await triggerGeneration(id, values);
      }

      if (isEmailPreviewFlow) {
        const generated = await getGeneratedContentById(result.generatedContentId);
        const { subject, body } = splitEmailContent(generated?.content);
        setPreview({ subject, body, from: values.sender, to: values.recipient });
      } else if (!conversationId) {
        router.push(`${basePath}/${id}?preview=true`);
      } else {
        // Regenerating an existing conversation — let the parent (which owns the
        // Email/Report/Summary preview UI) jump straight to the new content.
        onGenerated?.(result);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to generate. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  if (preview) {
    return (
      <main className="flex-1 overflow-y-auto">
        <EmailPreview
          from={preview.from}
          to={preview.to}
          subject={preview.subject}
          onSubjectChange={(subject) => setPreview((prev) => ({ ...prev, subject }))}
          body={preview.body}
          onBack={() => setPreview(null)}
        />
      </main>
    );
  }

  const isRegenerate = Boolean(conversationId);
  const Wrapper = isRegenerate ? "div" : "main";

  return (
    <Wrapper className={isRegenerate ? "" : "flex-1 overflow-y-auto"}>
      <div className={isRegenerate ? "space-y-6" : "mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 space-y-6"}>
        {/* Regenerate mode is embedded inside GenerationDetailView, which already
            renders its own title/status/last-updated header above this form — so
            skip the heading here and match the create form's bare field layout. */}
        {!isRegenerate && (
          <div>
            <h1 className="text-lg font-semibold text-text-primary">{conversationTypeLabel(conversationType)}</h1>
            <p className="text-sm text-text-muted mt-1">Fill in the details below to generate content.</p>
          </div>
        )}

        <form className="space-y-4" onSubmit={submit}>
          {groupFields(fields).map((group, i) =>
            group.fields.length > 1 ? (
              <div key={i} className={`grid grid-cols-1 ${getGridClass(group)} gap-4`}>
                {group.fields.map((f) => (
                  <Field
                    key={f.name}
                    f={f}
                    value={values[f.name]}
                    onChange={set(f.name)}
                    onCheckedChange={setChecked(f.name)}
                  />
                ))}
              </div>
            ) : (
              <Field
                key={group.fields[0].name}
                f={group.fields[0]}
                value={values[group.fields[0].name]}
                onChange={set(group.fields[0].name)}
                onCheckedChange={setChecked(group.fields[0].name)}
              />
            )
          )}

          {missingDocuments && (
            <p className="flex items-center gap-1.5 text-sm text-warning">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              Attach at least one source document before generating.
            </p>
          )}

          {error && <p className="text-sm text-error">{error}</p>}

          <div className="flex flex-wrap items-center justify-start gap-3 pt-2">
            <button
              type="submit"
              className="btn-primary text-sm"
              disabled={missingRequired || missingDocuments || submitting}
            >
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
              {submitting ? "Generating…" : isRegenerate ? "Regenerate" : `Generate ${conversationTypeLabel(conversationType).replace(" Generation", "")}`}
            </button>
            {actions}
          </div>
        </form>
      </div>
    </Wrapper>
  );
}
