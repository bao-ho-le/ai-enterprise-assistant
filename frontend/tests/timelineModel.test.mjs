import test from "node:test";
import assert from "node:assert/strict";

import {
  GENERATION_RUN,
  QA_MESSAGE,
  latestGenerationItem,
  mergeTimeline,
  parseInputData,
  replaceGenerationItem,
  sortTimeline,
  toGenerationTimeline,
  toQaMessageItem,
  toQaTimeline,
} from "../features/assistant/models/timelineModel.js";

test("QA messages keep their original id, role, content and timestamp", () => {
  const item = toQaMessageItem({
    id: 42,
    role: "ASSISTANT",
    content: "Answer [1]",
    createdAt: "2026-08-01T10:00:00",
  });

  assert.deepEqual(item, {
    id: "qa-42",
    kind: QA_MESSAGE,
    messageId: 42,
    role: "ASSISTANT",
    content: "Answer [1]",
    createdAt: "2026-08-01T10:00:00",
  });
});

test("QA normalization drops messages without an id", () => {
  const items = toQaTimeline([
    { id: 1, role: "USER", content: "hi", createdAt: "2026-08-01T10:00:00" },
    { role: "USER", content: "no id" },
    null,
  ]);
  assert.equal(items.length, 1);
  assert.equal(items[0].id, "qa-1");
});

test("generation runs stay generation_run items (never fake chat messages)", () => {
  const items = toGenerationTimeline(
    [
      { generationId: 7, status: "COMPLETED", createdAt: "2026-08-01T10:00:00", generatedContentId: 99 },
      { generationId: 8, status: "RUNNING", createdAt: "2026-08-01T11:00:00", generatedContentId: null },
    ],
    { conversationType: "REPORT_GENERATION", latestGenerationId: 8, inputData: { title: "Q3" } }
  );

  assert.equal(items.length, 2);
  assert.equal(items[0].kind, GENERATION_RUN);
  assert.equal(items[0].id, "generation-7");
  assert.equal(items[0].inputData, null, "older runs have no inputData (metadata-only endpoint)");
  assert.equal(items[1].isLatest, true);
  assert.deepEqual(items[1].inputData, { title: "Q3" });
  assert.equal(items[1].conversationType, "REPORT_GENERATION");
});

test("the latest run is synthesised when the history page does not contain it yet", () => {
  const items = toGenerationTimeline([], {
    conversationType: "EMAIL_GENERATION",
    latestGenerationId: 12,
    status: "COMPLETED",
    generatedContentId: 55,
    latestRunCreatedAt: "2026-08-02T09:00:00",
    inputData: { purpose: "Follow up" },
  });

  assert.equal(items.length, 1);
  assert.equal(items[0].generationId, 12);
  assert.equal(items[0].generatedContentId, 55);
  assert.equal(items[0].isLatest, true);
});

test("timeline ordering is oldest first with a numeric tie-break", () => {
  const items = sortTimeline([
    { id: "b", createdAt: "2026-08-01T10:00:00", messageId: 2 },
    { id: "a", createdAt: "2026-08-01T10:00:00", messageId: 1 },
    { id: "c", createdAt: "2026-08-01T09:00:00", messageId: 9 },
  ]);
  assert.deepEqual(
    items.map((item) => item.id),
    ["c", "a", "b"]
  );
});

test("mergeTimeline de-duplicates pages and keeps the richer copy", () => {
  const existing = [
    { id: "generation-1", kind: GENERATION_RUN, generationId: 1, content: null, inputData: null, createdAt: "2026-08-01T10:00:00" },
  ];
  const incoming = [
    { id: "generation-1", kind: GENERATION_RUN, generationId: 1, content: "body", inputData: { title: "Q3" }, createdAt: "2026-08-01T10:00:00" },
    { id: "qa-2", kind: QA_MESSAGE, messageId: 2, createdAt: "2026-08-01T11:00:00" },
  ];

  const merged = mergeTimeline(existing, incoming);
  assert.equal(merged.length, 2);
  assert.equal(merged[0].content, "body");
  assert.deepEqual(merged[0].inputData, { title: "Q3" });
});

test("parseInputData accepts raw objects, JSON strings and garbage", () => {
  assert.deepEqual(parseInputData({ title: "x" }), { title: "x" });
  assert.deepEqual(parseInputData('{"title":"x"}'), { title: "x" });
  assert.equal(parseInputData("not json"), null);
  assert.equal(parseInputData(null), null);
});

test("replaceGenerationItem patches one run and keeps ordering", () => {
  const items = toGenerationTimeline(
    [
      { generationId: 1, status: "COMPLETED", createdAt: "2026-08-01T10:00:00", generatedContentId: 10 },
      { generationId: 2, status: "COMPLETED", createdAt: "2026-08-01T11:00:00", generatedContentId: 11 },
    ],
    { conversationType: "SUMMARY_GENERATION" }
  );
  const patched = replaceGenerationItem(items, { id: "generation-1", content: "hello" });
  assert.equal(patched[0].content, "hello");
  assert.equal(latestGenerationItem(patched).generationId, 2);
});
