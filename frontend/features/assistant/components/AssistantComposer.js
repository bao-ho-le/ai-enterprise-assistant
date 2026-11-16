"use client";

import { Loader2, Send, Paperclip, SlidersHorizontal, AlertTriangle } from "lucide-react";
import AssistantModeSelector from "./AssistantModeSelector";

// One composer for all four tools. The mode decides which existing workflow runs; the
// prompt is the primary instruction (QA question / email purpose / report title /
// summary focus). Validation messages come from models/assistantRules.validateComposer.
export default function AssistantComposer({
  modes,
  mode,
  onSelectMode,
  showModeSelector = true,
  prompt,
  onPromptChange,
  onSubmit,
  submitting = false,
  disabled = false,
  error = "",
  documentCount = 0,
  onAttach,
  attachDisabled = false,
  onOpenOptions,
  optionsCount = 0,
  hint = "",
  lockReason = "",
}) {
  const locked = Boolean(lockReason);
  const canSubmit = Boolean(mode) && promptAllowed(mode, prompt) && !submitting && !disabled && !locked;

  const submit = (event) => {
    event.preventDefault();
    if (!canSubmit) return;
    onSubmit?.();
  };

  return (
    <div className="border-t border-border-subtle bg-bg-primary p-4">
      <div className="mx-auto max-w-3xl space-y-2">
        {(showModeSelector || onOpenOptions) && (
          <div className="flex items-center justify-between gap-3">
            {showModeSelector ? (
              <AssistantModeSelector
                modes={modes}
                modeKey={mode?.key}
                onSelect={onSelectMode}
                disabled={submitting}
              />
            ) : (
              <span className="text-xs text-text-muted">
                {mode?.label} conversation
              </span>
            )}

            {onOpenOptions && (
              <button
                type="button"
                className="btn-ghost text-xs"
                onClick={onOpenOptions}
                disabled={disabled}
              >
                <SlidersHorizontal className="h-3.5 w-3.5" />
                Options{optionsCount > 0 ? ` (${optionsCount})` : ""}
              </button>
            )}
          </div>
        )}

        <form onSubmit={submit} className="flex items-end gap-3">
          <div className="flex-1">
            <div className="flex min-h-[48px] items-center gap-2 rounded-xl border border-border-subtle bg-bg-card px-3 py-2">
              <textarea
                rows={1}
                placeholder={mode?.placeholder || "Ask anything…"}
                className="flex-1 resize-none bg-transparent text-sm text-text-primary outline-none leading-normal disabled:opacity-50"
                aria-label="Prompt"
                value={prompt}
                onChange={(event) => onPromptChange(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.shiftKey) {
                    event.preventDefault();
                    submit(event);
                  }
                }}
                disabled={submitting || disabled || locked}
              />

              {onAttach && (
                <button
                  type="button"
                  className="flex h-8 items-center gap-1 rounded-md px-1.5 text-xs text-text-muted hover:bg-bg-elevated hover:text-text-primary disabled:opacity-50"
                  onClick={onAttach}
                  disabled={attachDisabled || submitting}
                  title="Attach documents"
                >
                  <Paperclip className="h-4 w-4" />
                  {documentCount > 0 ? documentCount : ""}
                </button>
              )}

              <button
                type="submit"
                className="flex h-8 w-8 items-center justify-center rounded-md bg-accent text-white hover:bg-accent-hover disabled:opacity-40"
                aria-label={mode?.submitLabel || "Send"}
                title={mode?.submitLabel || "Send"}
                disabled={!canSubmit}
              >
                {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </form>

        {error ? (
          <p className="text-center text-xs text-error">{error}</p>
        ) : lockReason ? (
          <p className="flex items-center justify-center gap-1.5 text-center text-xs text-warning">
            <AlertTriangle className="h-3 w-3 shrink-0" />
            {lockReason}
          </p>
        ) : (
          hint && <p className="text-center text-xs text-text-muted">{hint}</p>
        )}
      </div>
    </div>
  );
}

// Summary can run without an extra instruction (the backend prompt handles it); every
// other mode needs a non-empty primary field. Mirrors validateComposer's prompt rule.
function promptAllowed(mode, prompt) {
  if (!mode) return false;
  if (mode.key === "summary") return true;
  return Boolean(String(prompt ?? "").trim());
}
