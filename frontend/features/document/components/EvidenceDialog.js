"use client";

import Modal from "@/components/ui/Modal";
import { rangeSegments } from "@/utils/format";

// ponytail: highlight paused per product ask — flip to true to re-enable.
const HIGHLIGHT_ENABLED = false;

// chunk.content is already the page slice at [startChar, endChar) — the API
// hands us the chunk's own text, not the full page, so those offsets aren't
// indices into content itself. The chunk's represented range is therefore
// just the whole content; this also serves as the no-position-data fallback.
function chunkHighlightRange({ content }) {
  return content ? [[0, content.length]] : [];
}

export default function EvidenceDialog({ open, onClose, doc, matches }) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={doc ? `Matching chunks — ${doc.title}` : "Matching chunks"}
      maxWidth="max-w-2xl"
    >
      {!matches || matches.length === 0 ? (
        <p className="text-sm text-text-muted">No matching chunks found.</p>
      ) : (
        <div className="space-y-3 max-h-[60vh] overflow-y-auto">
          {matches.map((chunk) => (
            <div key={chunk.chunkId} className="rounded-lg border border-border-subtle p-3">
              <div className="flex items-center justify-between mb-1.5 text-xs text-text-muted">
                <span>{chunk.page ? `Page ${chunk.page}` : "Page —"}</span>
                <span className="badge badge-success">{Math.round(chunk.score * 100)}% match</span>
              </div>
              <p className="text-sm text-text-secondary whitespace-pre-wrap">
                {rangeSegments(chunk.content, HIGHLIGHT_ENABLED ? chunkHighlightRange(chunk) : []).map(
                  (seg, i) =>
                    seg.match ? (
                      <mark key={i} className="bg-yellow-400 text-neutral-900 font-semibold rounded px-0.5">
                        {seg.text}
                      </mark>
                    ) : (
                      <span key={i}>{seg.text}</span>
                    )
                )}
              </p>
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}
