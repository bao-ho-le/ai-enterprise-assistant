"use client";

import { Loader2, Sparkles } from "lucide-react";
import AssistantModeSelector from "./AssistantModeSelector";
import AssistantChatComposer from "./AssistantChatComposer";
import GenerationFormSections from "./GenerationFormSections";
import SelectedDocumentsSection from "./SelectedDocumentsSection";
import { canSubmitRequest } from "@/features/assistant/models/assistantRules";

// The /chat creation surface: mode selector, then the selected mode's real content.
//
//   * Ask / Document QA keeps the original chatbox: one input box with attached
//     documents as chips, an attach button and a send button.
//   * Email / Report / Summary render their full original form inline (grouped sections),
//     followed by the document picker and a single submit bar.
export default function AssistantCreationPanel({ composer }) {
  const { mode, modes } = composer;
  if (!mode) return null;

  const isChat = mode.supportsMessages;
  const documentsError = composer.validation.errors.documents || "";
  const missingOptions = composer.validation.errors.options || [];
  const canSubmit = canSubmitRequest({ validation: composer.validation, submitting: composer.submitting });

  const addDocuments = (selected) => {
    composer.setDocuments((previous) => {
      const seen = new Set(previous.map((doc) => doc.documentVersionId));
      return [...previous, ...selected.filter((doc) => !seen.has(doc.documentVersionId))];
    });
  };

  const removeDocument = (doc) => {
    composer.setDocuments((previous) =>
      previous.filter((item) => item.documentVersionId !== doc.documentVersionId)
    );
  };

  const header = (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="min-w-0">
        <h2 className="text-base font-semibold text-text-primary">{mode.filterLabel}</h2>
        <p className="mt-1 text-sm leading-relaxed text-text-secondary">{mode.description}</p>
      </div>
      <div className="flex flex-wrap items-center gap-1.5">
        <span className={`badge ${mode.requiresDocuments ? "badge-warning" : "badge-neutral"}`}>
          {mode.requiresDocuments ? "Documents required" : "No documents required"}
        </span>
        <span className="badge badge-neutral">{isChat ? "Multi-turn" : "One-shot"}</span>
      </div>
    </div>
  );

  return (
    <section className="card overflow-hidden">
      {/* Mode selection */}
      <div className="border-b border-border-subtle bg-bg-secondary/40 p-4 sm:p-6">
        <AssistantModeSelector
          variant="cards"
          modes={modes}
          modeKey={mode.key}
          onSelect={composer.selectMode}
          disabled={composer.submitting}
        />
      </div>

      {isChat ? (
        /* Ask — the chatbox the Document QA screen always had, centred in the panel */
        <div className="p-4 sm:p-6">
          <div className="mx-auto w-full max-w-2xl space-y-4">
            <div className="space-y-2 text-center">
              <div>
                <h2 className="text-base font-semibold text-text-primary">{mode.filterLabel}</h2>
                <p className="mt-1 text-sm leading-relaxed text-text-secondary">{mode.description}</p>
              </div>
              <div className="flex flex-wrap items-center justify-center gap-1.5">
                <span className={`badge ${mode.requiresDocuments ? "badge-warning" : "badge-neutral"}`}>
                  {mode.requiresDocuments ? "Documents required" : "No documents required"}
                </span>
                <span className="badge badge-neutral">Multi-turn</span>
              </div>
            </div>
            <AssistantChatComposer
              mode={mode}
              prompt={composer.prompt}
              onPromptChange={composer.setPrompt}
              onSubmit={composer.submit}
              documents={composer.documents}
              onPick={addDocuments}
              onRemove={removeDocument}
              submitting={composer.submitting}
              canSubmit={canSubmit}
              error={composer.error}
              documentsError={documentsError}
              hint={mode.hint}
            />
          </div>
        </div>
      ) : (
        <>
          {/* Generation form — every original field, always visible */}
          <div className="space-y-5 border-b border-border-subtle p-4 sm:p-6">
            {header}
            <GenerationFormSections
              conversationType={mode.conversationType}
              values={composer.values}
              onChange={composer.setOption}
              idPrefix={`create-${mode.key}`}
            />
          </div>

          {/* Documents (only for modes that ground the request in them) */}
          {mode.requiresDocuments && (
            <div className="border-b border-border-subtle p-4 sm:p-6">
              <SelectedDocumentsSection
                documents={composer.documents}
                onPick={addDocuments}
                onRemove={removeDocument}
                required={mode.requiresDocuments}
                error={documentsError}
                description="The generated content uses these documents as its only source material."
              />
            </div>
          )}

          {/* Submit bar */}
          <div className="flex flex-col gap-3 bg-bg-secondary/40 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-6">
            <div className="min-w-0 space-y-1 text-xs">
              {composer.error ? (
                <p className="text-error">{composer.error}</p>
              ) : documentsError ? (
                <p className="text-warning">{documentsError}</p>
              ) : missingOptions.length > 0 ? (
                <p className="text-warning">Required: {missingOptions.join(", ")}</p>
              ) : (
                <p className="text-text-muted">
                  This creates a conversation whose result you can preview and regenerate later.
                </p>
              )}
            </div>

            <button
              type="button"
              className="btn-primary w-full shrink-0 sm:w-auto sm:min-w-[180px]"
              disabled={!canSubmit}
              onClick={composer.submit}
            >
              {composer.submitting ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Sparkles className="h-4 w-4" />
              )}
              {composer.submitting ? "Working…" : mode.createLabel}
            </button>
          </div>
        </>
      )}
    </section>
  );
}
