import test from "node:test";
import assert from "node:assert/strict";

import {
  describeGenerationInput,
  emailPreviewProps,
  splitEmailContent,
} from "../features/assistant/models/generatedContentModel.js";

test("splitEmailContent separates the Subject line from the body", () => {
  assert.deepEqual(splitEmailContent("Subject: Quarterly update\n\nHello team,"), {
    subject: "Quarterly update",
    body: "Hello team,",
  });
  assert.deepEqual(splitEmailContent("No subject here"), { subject: "", body: "No subject here" });
  assert.deepEqual(splitEmailContent(null), { subject: "", body: "" });
});

test("emailPreviewProps maps stored inputData to the existing preview props", () => {
  const props = emailPreviewProps(
    { sender: "me@corp.com", recipient: "you@corp.com" },
    "Subject: Hi\n\nBody"
  );
  assert.deepEqual(props, {
    from: "me@corp.com",
    to: "you@corp.com",
    subject: "Hi",
    body: "Body",
  });
});

test("describeGenerationInput only lists non-empty known fields", () => {
  const summary = describeGenerationInput({
    title: "Q3",
    purpose: "",
    instructions: "Focus on risks",
    language: "English",
    includeActionItems: false,
    unknownField: "ignored",
  });

  assert.deepEqual(summary.map((entry) => entry.name), ["title", "instructions", "language"]);
  assert.equal(summary.find((entry) => entry.name === "instructions").long, true);
  assert.deepEqual(describeGenerationInput(null), []);
});
