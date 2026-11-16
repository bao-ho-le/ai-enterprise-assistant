// Normalized timeline for the unified Assistant workspace.
//
// The four consolidated features do NOT share a conversation model:
//   - DOCUMENT_QA        -> AIMessage[] (roles, RAG sources, keyset pagination)
//   - EMAIL/REPORT/SUMMARY -> GenerationRun + GeneratedContent (no chat turns)
//
// The timeline keeps that distinction instead of faking assistant messages for
// generation runs: every item is a discriminated record whose `kind` says what it
// really is. Pure module (no imports) so it can be unit-tested with `node --test`.

export const QA_MESSAGE = "qa_message";
export const GENERATION_RUN = "generation_run";

// AIMessageResponse -> qa_message timeline item. Original id/timestamp/role are kept.
export function toQaMessageItem(message) {
  if (!message || message.id == null) return null;
  return {
    id: `qa-${message.id}`,
    kind: QA_MESSAGE,
    messageId: message.id,
    role: message.role,
    content: message.content ?? "",
    createdAt: message.createdAt ?? null,
  };
}

export function toQaTimeline(messages) {
  return (messages || []).map(toQaMessageItem).filter(Boolean);
}

// GenerationResponse (+ optional extras the detail endpoint knows about) -> generation_run.
// `inputData` is only available for the latest run (GET /generation-detail), older runs
// carry metadata only — the body is fetched on demand via GET /generated-contents/{id}.
export function toGenerationRunItem(run, extras = {}) {
  if (!run || run.generationId == null) return null;
  return {
    id: `generation-${run.generationId}`,
    kind: GENERATION_RUN,
    generationId: run.generationId,
    conversationType: extras.conversationType ?? null,
    status: run.status ?? extras.status ?? null,
    createdAt: run.createdAt ?? extras.createdAt ?? null,
    generatedContentId: run.generatedContentId ?? extras.generatedContentId ?? null,
    inputData: extras.inputData ?? null,
    content: extras.content ?? null,
    isLatest: Boolean(extras.isLatest),
  };
}

export function toGenerationTimeline(runs, options = {}) {
  const latestId = options.latestGenerationId ?? null;
  const items = (runs || [])
    .map((run) =>
      toGenerationRunItem(run, {
        conversationType: options.conversationType,
        isLatest: latestId != null && run.generationId === latestId,
        inputData: latestId != null && run.generationId === latestId ? options.inputData ?? null : null,
      })
    )
    .filter(Boolean);

  // The detail endpoint can know about a run the history page hasn't returned yet
  // (a run inserted between the two calls). Never drop the latest result.
  if (latestId != null && !items.some((item) => item.generationId === latestId)) {
    const synthesized = toGenerationRunItem(
      {
        generationId: latestId,
        status: options.status,
        createdAt: options.latestRunCreatedAt,
        generatedContentId: options.generatedContentId,
      },
      { conversationType: options.conversationType, inputData: options.inputData, isLatest: true }
    );
    if (synthesized) items.push(synthesized);
  }

  return sortTimeline(items);
}

// Backend serializes Generation.inputData as a raw JSON value in the detail response,
// but tolerate a string too (older payloads / defensive parsing).
export function parseInputData(value) {
  if (value == null) return null;
  if (typeof value === "string") {
    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === "object" ? parsed : null;
    } catch {
      return null;
    }
  }
  if (typeof value === "object") return value;
  return null;
}

function itemTime(item) {
  if (!item?.createdAt) return null;
  const time = new Date(item.createdAt).getTime();
  return Number.isNaN(time) ? null : time;
}

// deepseek: Mongo-style stable ordering — oldest first, undated items last, and a
// numeric tie-break so equal timestamps keep a deterministic order across pages.
function itemSequence(item) {
  const raw = item?.messageId ?? item?.generationId;
  const num = Number(raw);
  return Number.isFinite(num) ? num : 0;
}

export function sortTimeline(items) {
  return [...(items || [])].sort((a, b) => {
    const ta = itemTime(a);
    const tb = itemTime(b);
    if (ta !== null && tb !== null && ta !== tb) return ta - tb;
    if (ta === null && tb !== null) return 1;
    if (tb === null && ta !== null) return -1;
    return itemSequence(a) - itemSequence(b);
  });
}

// Merge a page of older/newer items into the current timeline: de-duplicate by id and
// keep whichever copy carries more data (content/inputData may arrive later).
export function mergeTimeline(existing, incoming) {
  const byId = new Map();
  for (const item of existing || []) byId.set(item.id, item);
  for (const item of incoming || []) {
    if (!item) continue;
    const previous = byId.get(item.id);
    byId.set(
      item.id,
      previous
        ? {
            ...previous,
            ...item,
            content: item.content ?? previous.content ?? null,
            inputData: item.inputData ?? previous.inputData ?? null,
          }
        : item
    );
  }
  return sortTimeline([...byId.values()]);
}

export function isQaItem(item) {
  return item?.kind === QA_MESSAGE;
}

export function isGenerationItem(item) {
  return item?.kind === GENERATION_RUN;
}

export function latestGenerationItem(items) {
  const runs = (items || []).filter(isGenerationItem);
  return runs.length ? runs[runs.length - 1] : null;
}

export function replaceGenerationItem(items, updated) {
  if (!updated) return items;
  return sortTimeline((items || []).map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
}
