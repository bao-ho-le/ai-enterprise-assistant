"use client";

import { useState } from "react";
import { ArrowLeft, Copy, Save, Download } from "lucide-react";

// Client-side only: builds a minimal RFC 5322-ish .eml file from the already-generated
// email so "Export as .eml" works without a dedicated backend endpoint.
function buildEmlBlob({ from, to, subject, body }) {
  const lines = [
    `From: ${from || ""}`,
    `To: ${to || ""}`,
    `Subject: ${subject || ""}`,
    `Date: ${new Date().toUTCString()}`,
    "Content-Type: text/plain; charset=utf-8",
    "",
    body || "",
  ];
  return new Blob([lines.join("\r\n")], { type: "message/rfc822" });
}

export default function EmailPreview({ from, to, subject, onSubjectChange, body, onBack }) {
  const [copyLabel, setCopyLabel] = useState("Copy to Clipboard");
  const [saveLabel, setSaveLabel] = useState("Save");
  const [bodyCopyLabel, setBodyCopyLabel] = useState("");

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(body || "");
      setCopyLabel("Copied!");
      setTimeout(() => setCopyLabel("Copy to Clipboard"), 1500);
    } catch {
      // Clipboard API unavailable (e.g. insecure context) — nothing more we can do here.
    }
  };

  const copyBody = async () => {
    try {
      await navigator.clipboard.writeText(body || "");
      setBodyCopyLabel("Copied!");
      setTimeout(() => setBodyCopyLabel(""), 1500);
    } catch {
      // Clipboard API unavailable — nothing more we can do here.
    }
  };

  const save = () => {
    // Content is already persisted server-side as GeneratedContent the moment it was
    // generated — this button just confirms that to the user, no extra call needed.
    setSaveLabel("Saved!");
    setTimeout(() => setSaveLabel("Save"), 1500);
  };

  const exportEml = () => {
    const blob = buildEmlBlob({ from, to, subject, body });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${(subject || "email").replace(/[^a-z0-9\- _]/gi, "_").slice(0, 60)}.eml`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-text-primary">Email Preview</h1>
        <button type="button" className="btn-secondary text-sm" onClick={onBack}>
          <ArrowLeft className="h-4 w-4" />
          Back to Form
        </button>
      </div>

      <div className="card overflow-hidden">
        <div className="space-y-3 p-4 border-b border-border-subtle">
          <div className="flex items-center gap-3 text-sm">
            <span className="w-16 shrink-0 text-text-muted">From:</span>
            <span className="text-text-primary">{from || "—"}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="w-16 shrink-0 text-text-muted">To:</span>
            <span className="text-text-primary">{to || "—"}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="w-16 shrink-0 text-text-muted">Subject:</span>
            <input
              className="input-field flex-1 py-1.5"
              value={subject}
              onChange={(e) => onSubjectChange(e.target.value)}
            />
          </div>
        </div>

        <div className="p-4 relative group">
          <button
            type="button"
            onClick={copyBody}
            title="Copy"
            className="absolute top-3 right-3 p-1 rounded-md text-text-muted hover:text-text-primary hover:bg-bg-elevated transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
          >
            <Copy className="h-3.5 w-3.5" />
          </button>
          {bodyCopyLabel && (
            <span className="absolute top-3 right-11 text-xs text-accent font-medium">
              {bodyCopyLabel}
            </span>
          )}
          <p className="text-sm text-text-primary leading-relaxed whitespace-pre-wrap">{body}</p>
        </div>

        <div className="flex flex-wrap items-center gap-3 p-4 border-t border-border-subtle">
          <button type="button" className="btn-primary text-sm" onClick={copy}>
            <Copy className="h-4 w-4" />
            {copyLabel}
          </button>
          <button type="button" className="btn-secondary text-sm" onClick={save}>
            <Save className="h-4 w-4" />
            {saveLabel}
          </button>
          <button type="button" className="btn-secondary text-sm" onClick={exportEml}>
            <Download className="h-4 w-4" />
            Export as .eml
          </button>
        </div>
      </div>
    </div>
  );
}
