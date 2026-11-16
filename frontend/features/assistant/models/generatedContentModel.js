// Helpers for rendering generated content inside the unified timeline.
// Pure module (no imports) so it can be unit-tested with `node --test`.

// Pulls the "Subject: ..." line FakeLLMService prepends to email content back out,
// so the preview can show it as its own field instead of the email's first line.
export function splitEmailContent(content) {
  const match = /^Subject:\s*(.*)\n+([\s\S]*)$/.exec(content || "");
  return match
    ? { subject: match[1].trim(), body: match[2].trim() }
    : { subject: "", body: content || "" };
}

// Props for the existing EmailPreview component, fed from the generation input.
export function emailPreviewProps(inputData, content) {
  const { subject, body } = splitEmailContent(content);
  return {
    from: inputData?.sender || "",
    to: inputData?.recipient || "",
    subject,
    body,
  };
}

// Short "what was asked" summary shown on a generation result card. Mirrors the
// fields the backend actually stores in Generation.inputData.
const INPUT_LABELS = [
  { name: "title", label: "Title" },
  { name: "purpose", label: "Purpose" },
  { name: "instructions", label: "Instructions" },
  { name: "optionalContext", label: "Context" },
  { name: "sender", label: "From" },
  { name: "recipient", label: "To" },
  { name: "style", label: "Style" },
  { name: "tone", label: "Tone" },
  { name: "length", label: "Length" },
  { name: "audience", label: "Audience" },
  { name: "language", label: "Language" },
  { name: "fromDate", label: "From date" },
  { name: "toDate", label: "To date" },
];

const LONG_TEXT_FIELDS = new Set(["purpose", "instructions", "optionalContext"]);

export function describeGenerationInput(inputData) {
  if (!inputData || typeof inputData !== "object") return [];
  return INPUT_LABELS.filter(({ name }) => {
    const value = inputData[name];
    return value !== null && value !== undefined && String(value).trim() !== "" && value !== false;
  }).map(({ name, label }) => ({
    name,
    label,
    value: String(inputData[name]),
    long: LONG_TEXT_FIELDS.has(name),
  }));
}
