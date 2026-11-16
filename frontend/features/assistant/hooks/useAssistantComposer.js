"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/AuthContext";
import { ApiError } from "@/lib/apiClient";
import { getAdapter } from "@/features/assistant/adapters";
import {
  availableModes,
  modeByKey,
  resolveMode,
  validateComposer,
} from "@/features/assistant/models/assistantRules";
import {
  buildGenerationInput,
  validateGenerationValues,
} from "@/features/assistant/config/generationOptions";
import {
  createGenerationFormValues,
  setConversationTypeValue,
  valuesForConversationType,
} from "@/features/assistant/models/composerState";
import useFileStorageAttach from "./useFileStorageAttach";

// Drives /chat (new conversation): mode selection, per-mode generation form values,
// staged documents and the submit that calls the same start endpoints the old screens used.
//
// State rules:
//   * each generation mode keeps its own field values (valuesByType) — switching modes
//     never wipes what was typed, and switching back restores it;
//   * the Ask prompt and the document selection are shared across switches (documents are
//     only sent for modes that require them);
//   * only the selected mode is validated, and the payload is always built from that
//     mode's own configuration.
export default function useAssistantComposer({ initialModeKey } = {}) {
  const { user } = useAuth();
  const router = useRouter();
  const permissions = user?.permissions;

  const modes = useMemo(() => availableModes(permissions), [permissions]);
  const [modeKey, setModeKey] = useState(() => resolveMode(initialModeKey, permissions)?.key ?? null);
  const mode = modeByKey(modeKey);

  // Keep the requested mode in sync (?mode= changes, permission list arrives later).
  useEffect(() => {
    const next = resolveMode(initialModeKey, permissions);
    setModeKey(next?.key ?? null);
  }, [initialModeKey, permissions]);

  const [prompt, setPrompt] = useState("");
  const [valuesByType, setValuesByType] = useState(createGenerationFormValues);
  const [documents, setDocuments] = useFileStorageAttach();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const conversationType = mode && !mode.supportsMessages ? mode.conversationType : null;

  const values = useMemo(
    () => (conversationType ? valuesForConversationType(valuesByType, conversationType) : {}),
    [valuesByType, conversationType]
  );

  // Switching modes only changes which slice is visible — values, prompt and documents
  // all stay exactly as the user left them.
  const selectMode = useCallback((nextKey) => {
    setModeKey(nextKey);
    setError("");
  }, []);

  const setOption = useCallback(
    (name, value) => {
      if (!conversationType) return;
      setValuesByType((previous) => setConversationTypeValue(previous, conversationType, name, value));
    },
    [conversationType]
  );

  // Required-field validation for the selected generation mode (same rule the old forms
  // used: Generate is disabled until required fields are filled).
  const missingOptionFields = useMemo(
    () => (conversationType ? validateGenerationValues(conversationType, values) : []),
    [conversationType, values]
  );

  const validation = validateComposer({
    mode,
    prompt,
    documentCount: documents.length,
    missingOptionFields,
  });

  const documentVersionIds = useMemo(
    () => documents.map((doc) => doc.documentVersionId).filter((id) => id != null),
    [documents]
  );

  const submit = useCallback(async () => {
    if (!mode || submitting) return null;
    const current = validateComposer({
      mode,
      prompt,
      documentCount: documents.length,
      missingOptionFields,
    });
    if (!current.ok) return null;

    setSubmitting(true);
    setError("");
    try {
      const adapter = getAdapter(mode.conversationType);
      const result = await adapter.start({
        // Ask sends its question as the first message; generation modes send their form
        // values only (the adapter builds the exact XxxGenerationInput payload).
        prompt: mode.supportsMessages ? prompt.trim() : "",
        documentVersionIds: mode.requiresDocuments ? documentVersionIds : undefined,
        values: mode.supportsMessages ? undefined : values,
      });
      if (result?.conversationId == null) {
        throw new Error("The server did not return a conversation id.");
      }
      router.push(`/chat/${result.conversationId}`);
      return result;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to start the conversation. Please try again.");
      return null;
    } finally {
      setSubmitting(false);
    }
  }, [mode, submitting, prompt, documents.length, missingOptionFields, documentVersionIds, values, router]);

  return {
    modes,
    mode,
    modeKey,
    selectMode,
    conversationType,
    prompt,
    setPrompt,
    values,
    setOption,
    documents,
    setDocuments,
    documentVersionIds,
    validation,
    missingOptionFields,
    submitting,
    error,
    submit,
    canSubmit: Boolean(mode) && validation.ok && !submitting,
    // Preview helper so the exact payload can be inspected without sending it.
    buildInputData: () =>
      conversationType ? buildGenerationInput(conversationType, values, "") : null,
  };
}
