// Per-mode generation form state for the /chat creation panel.
//
// Each generation tool keeps its own field values, so switching Ask → Email → Report →
// Summary and back never wipes what the user already typed. Documents and the Ask prompt
// are separate state (see useAssistantComposer) and are shared across switches.
//
// Pure module; imports the schema with an explicit extension so `node --test` can load it.

import { initialGenerationValues } from "../config/generationOptions.js";

export const GENERATION_FORM_TYPES = ["EMAIL_GENERATION", "REPORT_GENERATION", "SUMMARY_GENERATION"];

export function createGenerationFormValues() {
  const values = {};
  for (const conversationType of GENERATION_FORM_TYPES) {
    values[conversationType] = initialGenerationValues(conversationType);
  }
  return values;
}

export function valuesForConversationType(valuesByType, conversationType, fallback = null) {
  if (!conversationType) return {};
  return valuesByType?.[conversationType] ?? initialGenerationValues(conversationType, fallback);
}

export function setConversationTypeValue(valuesByType, conversationType, name, value) {
  if (!conversationType) return valuesByType;
  const current = valuesForConversationType(valuesByType, conversationType);
  if (valuesByType?.[conversationType] && current[name] === value) return valuesByType;
  return { ...valuesByType, [conversationType]: { ...current, [name]: value } };
}

export function replaceConversationTypeValues(valuesByType, conversationType, values) {
  if (!conversationType) return valuesByType;
  return { ...valuesByType, [conversationType]: { ...values } };
}
