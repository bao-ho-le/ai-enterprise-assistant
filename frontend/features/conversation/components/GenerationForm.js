"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { createConversation, triggerGeneration } from "@/services/conversationService";
import { conversationTypeLabel } from "@/constants/conversation";
import { ApiError } from "@/lib/apiClient";

// Field schema per generation conversationType — keeps the 3 wired generation
// screens on one component instead of near-duplicate forms. Must match the
// backend's XxxGenerationInput DTOs (ai/generation/dto).
const LANGUAGE_OPTIONS = ["English", "Vietnamese", "Spanish", "French"];

const FIELD_CONFIGS = {
  EMAIL_GENERATION: [
    { name: "recipient", label: "Recipient", type: "text", placeholder: "e.g. Customer support team" },
    { name: "purpose", label: "Purpose", type: "textarea", required: true, placeholder: "What should this email accomplish?" },
    { name: "tone", label: "Tone", type: "select", options: ["Formal", "Friendly", "Persuasive", "Apologetic"] },
    { name: "length", label: "Length", type: "select", options: ["Short", "Medium", "Long"] },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS },
    { name: "senderName", label: "Sender name", type: "text", placeholder: "e.g. Jane Doe" },
  ],
  REPORT_GENERATION: [
    { name: "title", label: "Report title", type: "text", required: true, placeholder: "e.g. Q3 Sales Performance" },
    { name: "instructions", label: "Instructions", type: "textarea", placeholder: "Focus areas or additional instructions" },
    { name: "audience", label: "Audience", type: "text", placeholder: "e.g. Executives, Team, Clients" },
    { name: "length", label: "Length", type: "select", options: ["Brief", "Standard", "Detailed"] },
    { name: "fromDate", label: "From date", type: "date" },
    { name: "toDate", label: "To date", type: "date" },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS },
  ],
  SUMMARY_GENERATION: [
    {
      name: "style",
      label: "Style",
      type: "select",
      required: true,
      options: ["EXECUTIVE", "BULLET_POINTS", "TIMELINE", "ACTION_ITEMS"],
      default: "EXECUTIVE",
    },
    { name: "instructions", label: "Instructions", type: "textarea", placeholder: "Anything specific to focus the summary on" },
    { name: "length", label: "Length", type: "select", options: ["Short", "Medium", "Long"] },
    { name: "audience", label: "Audience", type: "text", placeholder: "e.g. Executives, Team, Clients" },
    { name: "language", label: "Language", type: "select", options: LANGUAGE_OPTIONS },
    { name: "includeActionItems", label: "Include an action items section", type: "checkbox", default: false },
  ],
};

function initialState(fields, initialValues) {
  const state = {};
  for (const f of fields) {
    state[f.name] = initialValues?.[f.name] ?? f.default ?? "";
  }
  return state;
}

// mode is implicit: pass conversationId to render in "regenerate" mode (prefilled,
// posts /generate on the existing conversation); omit it for "create" mode (creates
// the conversation with no title, then triggers the first generation run).
export default function GenerationForm({ conversationType, basePath, conversationId, initialValues, onGenerated }) {
  const router = useRouter();
  const fields = FIELD_CONFIGS[conversationType] || [];
  const [values, setValues] = useState(() => initialState(fields, initialValues));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const set = (name) => (e) => setValues((prev) => ({ ...prev, [name]: e.target.value }));
  const setChecked = (name) => (e) => setValues((prev) => ({ ...prev, [name]: e.target.checked }));

  const missingRequired = fields.some((f) => f.required && !String(values[f.name] || "").trim());

  const submit = async (e) => {
    e.preventDefault();
    if (missingRequired || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      let id = conversationId;
      if (!id) {
        const conversation = await createConversation({ conversationType });
        id = conversation.id;
      }
      await triggerGeneration(id, values);
      if (!conversationId) {
        router.push(`${basePath}/${id}`);
      } else {
        onGenerated?.();
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to generate. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const isRegenerate = Boolean(conversationId);
  const Wrapper = isRegenerate ? "div" : "main";

  return (
    <Wrapper className={isRegenerate ? "" : "flex-1 overflow-y-auto"}>
      <div className={isRegenerate ? "card p-6 space-y-4" : "mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8 space-y-6"}>
        {isRegenerate ? (
          <h2 className="text-sm font-medium text-text-primary">Generate again</h2>
        ) : (
          <div>
            <h1 className="text-lg font-semibold text-text-primary">{conversationTypeLabel(conversationType)}</h1>
            <p className="text-sm text-text-muted mt-1">Fill in the details below to generate content.</p>
          </div>
        )}

        <form className="space-y-4" onSubmit={submit}>
          {fields.map((f) =>
            f.type === "checkbox" ? (
              <label key={f.name} htmlFor={`gen-${f.name}`} className="flex items-center gap-2">
                <input
                  id={`gen-${f.name}`}
                  type="checkbox"
                  className="h-4 w-4 accent-accent"
                  checked={Boolean(values[f.name])}
                  onChange={setChecked(f.name)}
                />
                <span className="text-sm text-text-secondary">{f.label}</span>
              </label>
            ) : (
              <div key={f.name}>
                <label htmlFor={`gen-${f.name}`} className="label-text">
                  {f.label}
                  {f.required && <span className="text-error"> *</span>}
                </label>
                {f.type === "textarea" ? (
                  <textarea
                    id={`gen-${f.name}`}
                    className="input-field min-h-[100px]"
                    placeholder={f.placeholder}
                    value={values[f.name]}
                    onChange={set(f.name)}
                  />
                ) : f.type === "select" ? (
                  <select
                    id={`gen-${f.name}`}
                    className="select-field w-full"
                    value={values[f.name]}
                    onChange={set(f.name)}
                  >
                    {!f.required && <option value="">—</option>}
                    {f.options.map((o) => (
                      <option key={o} value={o}>
                        {o}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    id={`gen-${f.name}`}
                    type={f.type === "date" ? "date" : "text"}
                    className="input-field"
                    placeholder={f.placeholder}
                    value={values[f.name]}
                    onChange={set(f.name)}
                  />
                )}
              </div>
            )
          )}

          {error && <p className="text-sm text-error">{error}</p>}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button type="submit" className="btn-primary text-sm" disabled={missingRequired || submitting}>
              {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {submitting ? "Generating…" : isRegenerate ? "Regenerate" : "Generate"}
            </button>
          </div>
        </form>
      </div>
    </Wrapper>
  );
}
