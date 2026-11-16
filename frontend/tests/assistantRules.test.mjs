import test from "node:test";
import assert from "node:assert/strict";

import {
  ASSISTANT_MODES,
  availableModes,
  canRegenerate,
  canSendMessages,
  canSubmitRequest,
  modeAvailability,
  modeByConversationType,
  modeByKey,
  resolveMode,
  validateComposer,
} from "../features/assistant/models/assistantRules.js";

const ALL_PERMISSIONS = ["AI_DOCUMENT_QA", "AI_DOCUMENT_GENERATION"];

test("modes map to the four backend conversation types", () => {
  assert.equal(modeByKey("qa").conversationType, "DOCUMENT_QA");
  assert.equal(modeByKey("email").conversationType, "EMAIL_GENERATION");
  assert.equal(modeByKey("report").conversationType, "REPORT_GENERATION");
  assert.equal(modeByKey("summary").conversationType, "SUMMARY_GENERATION");
  assert.equal(modeByConversationType("EMAIL_GENERATION").key, "email");
  assert.equal(modeByKey("nope"), null);
});

test("permission-based availability mirrors backend feature permissions", () => {
  assert.equal(modeAvailability(modeByKey("qa"), ["AI_DOCUMENT_QA"]), true);
  assert.equal(modeAvailability(modeByKey("qa"), ["AI_DOCUMENT_GENERATION"]), false);
  assert.equal(modeAvailability(modeByKey("report"), ["AI_DOCUMENT_GENERATION"]), true);
  assert.equal(modeAvailability(modeByKey("report"), []), false);
  assert.equal(modeAvailability(null, ALL_PERMISSIONS), false);
  assert.deepEqual(availableModes(["AI_DOCUMENT_QA"]).map((mode) => mode.key), ["qa"]);
  assert.equal(availableModes(ALL_PERMISSIONS).length, ASSISTANT_MODES.length);
});

test("resolveMode falls back to the first permitted mode", () => {
  assert.equal(resolveMode("report", ["AI_DOCUMENT_QA"]).key, "qa");
  assert.equal(resolveMode("report", ALL_PERMISSIONS).key, "report");
  assert.equal(resolveMode("email", ["AI_DOCUMENT_QA"]).key, "qa");
  assert.equal(resolveMode("email", []), null);
});

test("Document QA requires both a question and a document", () => {
  const mode = modeByKey("qa");
  assert.equal(validateComposer({ mode, prompt: "", documentCount: 0 }).ok, false);
  assert.equal(validateComposer({ mode, prompt: "what?", documentCount: 0 }).errors.documents !== null, true);
  assert.equal(validateComposer({ mode, prompt: "", documentCount: 1 }).errors.prompt !== null, true);
  assert.equal(validateComposer({ mode, prompt: "what?", documentCount: 1 }).ok, true);
});

test("email does not require documents", () => {
  const mode = modeByKey("email");
  assert.equal(validateComposer({ mode, prompt: "Say hi", documentCount: 0 }).ok, true);
  // Email purpose lives in the form now, not in the prompt: completeness is reported
  // through missingOptionFields, and an empty prompt alone never blocks e-mail.
  assert.equal(validateComposer({ mode, prompt: "", documentCount: 0 }).errors.prompt, null);
  assert.equal(validateComposer({ mode, prompt: "", documentCount: 0 }).ok, true);
  assert.equal(
    validateComposer({ mode, prompt: "", documentCount: 0, missingOptionFields: ["Purpose"] }).ok,
    false
  );
});

test("report and summary require a source document", () => {
  for (const key of ["report", "summary"]) {
    const mode = modeByKey(key);
    const result = validateComposer({ mode, prompt: "something", documentCount: 0 });
    assert.equal(result.ok, false);
    assert.match(result.errors.documents, /source document/i);
  }
  // Generation modes validate their own fields (title/style/purpose) rather than a prompt.
  assert.equal(validateComposer({ mode: modeByKey("report"), prompt: "", documentCount: 1 }).errors.prompt, null);
  assert.equal(validateComposer({ mode: modeByKey("summary"), prompt: "", documentCount: 1 }).ok, true);
  // Report needs a title: reported as a missing required option.
  assert.equal(
    validateComposer({ mode: modeByKey("report"), prompt: "", documentCount: 1, missingOptionFields: ["Report title"] }).ok,
    false
  );
  assert.equal(
    validateComposer({ mode: modeByKey("report"), prompt: "", documentCount: 1, missingOptionFields: [] }).ok,
    true
  );
});

test("missing required options block submission", () => {
  const result = validateComposer({
    mode: modeByKey("summary"),
    prompt: "focus",
    documentCount: 1,
    missingOptionFields: ["Summary Type"],
  });
  assert.equal(result.ok, false);
  assert.deepEqual(result.errors.options, ["Summary Type"]);
});

test("every mode exposes the copy the selector and submit button use", () => {
  for (const mode of ASSISTANT_MODES) {
    assert.equal(typeof mode.description, "string");
    assert.ok(mode.description.length > 10, `${mode.key} has a description`);
    assert.equal(typeof mode.createLabel, "string");
    assert.ok(mode.filterLabel);
  }
  assert.equal(modeByKey("qa").createLabel, "Ask question");
  assert.equal(modeByKey("summary").createLabel, "Generate summary");
});

test("duplicate submissions are blocked while one is in flight", () => {
  const ready = validateComposer({ mode: modeByKey("email"), prompt: "hi", documentCount: 0 });
  assert.equal(canSubmitRequest({ validation: ready, submitting: false }), true);
  assert.equal(canSubmitRequest({ validation: ready, submitting: true }), false);
  const blocked = validateComposer({ mode: modeByKey("report"), prompt: "", documentCount: 0 });
  assert.equal(canSubmitRequest({ validation: blocked, submitting: false }), false);
});

test("only Document QA accepts chat turns", () => {
  assert.equal(canSendMessages("DOCUMENT_QA"), true);
  assert.equal(canSendMessages("EMAIL_GENERATION"), false);
  assert.equal(canSendMessages("REPORT_GENERATION"), false);
  assert.equal(canSendMessages("SUMMARY_GENERATION"), false);
  assert.equal(canRegenerate("EMAIL_GENERATION"), true);
  assert.equal(canRegenerate("DOCUMENT_QA"), false);
});
