import test from "node:test";
import assert from "node:assert/strict";

import {
  GENERATION_FORM_TYPES,
  createGenerationFormValues,
  replaceConversationTypeValues,
  setConversationTypeValue,
  valuesForConversationType,
} from "../features/assistant/models/composerState.js";

test("every generation type starts with its schema defaults", () => {
  const values = createGenerationFormValues();
  assert.deepEqual(Object.keys(values).sort(), [...GENERATION_FORM_TYPES].sort());
  assert.equal(values.EMAIL_GENERATION.tone, "Professional");
  assert.equal(values.REPORT_GENERATION.length, "Medium");
  assert.equal(values.SUMMARY_GENERATION.style, "PARAGRAPH");
  assert.equal(values.EMAIL_GENERATION.purpose, "");
});

test("valuesForConversationType falls back to defaults for unknown types", () => {
  const values = createGenerationFormValues();
  assert.equal(valuesForConversationType(values, "REPORT_GENERATION").title, "");
  assert.deepEqual(valuesForConversationType(values, null), {});
});

test("editing one mode never mutates the other modes", () => {
  const initial = createGenerationFormValues();
  const email = setConversationTypeValue(initial, "EMAIL_GENERATION", "purpose", "Follow up on invoice");
  const report = setConversationTypeValue(email, "REPORT_GENERATION", "title", "Q3 Sales");

  assert.equal(valuesForConversationType(report, "EMAIL_GENERATION").purpose, "Follow up on invoice");
  assert.equal(valuesForConversationType(report, "REPORT_GENERATION").title, "Q3 Sales");
  assert.equal(valuesForConversationType(report, "SUMMARY_GENERATION").style, "PARAGRAPH");
  assert.equal(initial.EMAIL_GENERATION.purpose, "", "original state object untouched");
});

test("switching modes preserves typed values (round trip)", () => {
  let state = createGenerationFormValues();
  state = setConversationTypeValue(state, "EMAIL_GENERATION", "purpose", "Apology email");
  state = setConversationTypeValue(state, "EMAIL_GENERATION", "tone", "Apologetic");
  state = setConversationTypeValue(state, "REPORT_GENERATION", "title", "Incident review");
  state = setConversationTypeValue(state, "SUMMARY_GENERATION", "style", "BULLET_POINTS");

  // Back on Email after visiting Report/Summary.
  assert.deepEqual(valuesForConversationType(state, "EMAIL_GENERATION"), {
    ...createGenerationFormValues().EMAIL_GENERATION,
    purpose: "Apology email",
    tone: "Apologetic",
  });
  assert.equal(valuesForConversationType(state, "SUMMARY_GENERATION").style, "BULLET_POINTS");
});

test("setting an unchanged value keeps the same state reference", () => {
  const state = createGenerationFormValues();
  assert.equal(setConversationTypeValue(state, "EMAIL_GENERATION", "tone", "Professional"), state);
});

test("replaceConversationTypeValues swaps a whole mode (regenerate prefill)", () => {
  const state = replaceConversationTypeValues(createGenerationFormValues(), "REPORT_GENERATION", {
    title: "Prefilled",
    length: "Long",
  });
  assert.equal(valuesForConversationType(state, "REPORT_GENERATION").title, "Prefilled");
  assert.equal(valuesForConversationType(state, "REPORT_GENERATION").length, "Long");
});
