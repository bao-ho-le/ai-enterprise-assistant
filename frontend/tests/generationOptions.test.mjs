import test from "node:test";
import assert from "node:assert/strict";

import {
  GENERATION_FIELD_CONFIGS,
  buildGenerationInput,
  countCustomisedOptions,
  generationFields,
  generationFormSections,
  initialGenerationValues,
  validateGenerationValues,
} from "../features/assistant/config/generationOptions.js";

test("email options keep every field the old form had", () => {
  const names = generationFields("EMAIL_GENERATION").map((field) => field.name);
  assert.deepEqual(names, [
    "purpose",
    "sender",
    "recipient",
    "optionalContext",
    "language",
    "length",
    "audience",
    "tone",
  ]);
});

test("report and summary keep every field the old form had", () => {
  assert.deepEqual(
    generationFields("REPORT_GENERATION").map((field) => field.name),
    ["title", "fromDate", "toDate", "instructions", "language", "length", "audience"]
  );
  assert.deepEqual(
    generationFields("SUMMARY_GENERATION").map((field) => field.name),
    ["style", "instructions", "language", "length"]
  );
});

test("every field appears in exactly one visible form section", () => {
  for (const conversationType of Object.keys(GENERATION_FIELD_CONFIGS)) {
    const sections = generationFormSections(conversationType);
    const rendered = sections.flatMap((section) => section.fields.map((field) => field.name));
    const expected = generationFields(conversationType).map((field) => field.name);

    assert.deepEqual(rendered.slice().sort(), expected.slice().sort(), `${conversationType} renders every field`);
    assert.equal(new Set(rendered).size, rendered.length, `${conversationType} has no duplicated field`);
    assert.ok(sections.length >= 2, `${conversationType} is grouped into sections`);
    assert.ok(sections.every((section) => section.title), `${conversationType} sections are titled`);
  }
});

test("form sections keep the documented grouping", () => {
  assert.deepEqual(
    generationFormSections("EMAIL_GENERATION").map((section) => section.fields.map((field) => field.name)),
    [
      ["purpose", "sender", "recipient"],
      ["language", "length", "audience", "tone"],
      ["optionalContext"],
    ]
  );
  assert.deepEqual(
    generationFormSections("REPORT_GENERATION").map((section) => section.fields.map((field) => field.name)),
    [
      ["title", "fromDate", "toDate"],
      ["language", "length", "audience"],
      ["instructions"],
    ]
  );
  assert.deepEqual(
    generationFormSections("SUMMARY_GENERATION").map((section) => section.fields.map((field) => field.name)),
    [
      ["style"],
      ["language", "length"],
      ["instructions"],
    ]
  );
});

test("initial values fall back to schema defaults", () => {
  const values = initialGenerationValues("EMAIL_GENERATION");
  assert.equal(values.language, "English");
  assert.equal(values.length, "Medium");
  assert.equal(values.tone, "Professional");
  assert.equal(values.purpose, "");
  assert.equal(initialGenerationValues("SUMMARY_GENERATION").style, "PARAGRAPH");
});

test("initial values fall back when stored values are no longer valid", () => {
  const values = initialGenerationValues("SUMMARY_GENERATION", { style: "LEGACY_STYLE" });
  assert.equal(values.style, "PARAGRAPH");
});

test("required fields validate per type", () => {
  assert.deepEqual(
    validateGenerationValues("EMAIL_GENERATION", initialGenerationValues("EMAIL_GENERATION"), "Write a follow-up"),
    []
  );
  assert.deepEqual(
    validateGenerationValues("REPORT_GENERATION", initialGenerationValues("REPORT_GENERATION"), ""),
    ["Report title"]
  );
  assert.deepEqual(
    validateGenerationValues("SUMMARY_GENERATION", { style: "" }, "focus"),
    ["Summary Type"]
  );
});

test("the composer prompt fills the primary field", () => {
  const email = buildGenerationInput("EMAIL_GENERATION", initialGenerationValues("EMAIL_GENERATION"), "Follow up");
  assert.equal(email.purpose, "Follow up");
  assert.equal(email.tone, "Professional");
  assert.equal(email.sender, "", "untouched optional fields stay empty, not dropped");

  const report = buildGenerationInput("REPORT_GENERATION", initialGenerationValues("REPORT_GENERATION"), "Q3 Sales");
  assert.equal(report.title, "Q3 Sales");

  const summary = buildGenerationInput("SUMMARY_GENERATION", initialGenerationValues("SUMMARY_GENERATION"), "Risks only");
  assert.equal(summary.instructions, "Risks only");
});

test("an explicitly configured report title wins over the prompt", () => {
  const values = { ...initialGenerationValues("REPORT_GENERATION"), title: "Q4 Revenue Review" };
  const input = buildGenerationInput("REPORT_GENERATION", values, "ignored prompt");
  assert.equal(input.title, "Q4 Revenue Review");
});

test("empty prompt does not wipe configured values", () => {
  const values = { ...initialGenerationValues("SUMMARY_GENERATION"), instructions: "keep me" };
  const input = buildGenerationInput("SUMMARY_GENERATION", values, "   ");
  assert.equal(input.instructions, "keep me");
});

test("customised options are counted for the options badge", () => {
  assert.equal(countCustomisedOptions("EMAIL_GENERATION", initialGenerationValues("EMAIL_GENERATION")), 0);
  const values = { ...initialGenerationValues("EMAIL_GENERATION"), tone: "Friendly", length: "Long" };
  assert.equal(countCustomisedOptions("EMAIL_GENERATION", values), 2);
});
