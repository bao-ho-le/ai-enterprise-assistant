"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import Modal from "@/components/ui/Modal";
import { useDebounce } from "@/hooks/useDebounce";
import { DOCUMENT_TYPES, UPLOAD_ACCEPT } from "@/constants/document";
import { checkTitle, uploadDocument } from "@/services/documentService";
import { ApiError } from "@/lib/apiClient";

const EMPTY = { title: "", description: "", documentType: "REPORT" };

export default function UploadDocumentModal({ open, onClose, onUploaded }) {
  const [form, setForm] = useState(EMPTY);
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Title existence check (debounced, doesn't fire on every keystroke)
  const debouncedTitle = useDebounce(form.title.trim(), 450);
  const [titleTaken, setTitleTaken] = useState(false);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    if (!open) {
      setForm(EMPTY);
      setFile(null);
      setError("");
      setTitleTaken(false);
    }
  }, [open]);

  useEffect(() => {
    if (!open || !debouncedTitle) {
      setTitleTaken(false);
      return;
    }
    const controller = new AbortController();
    setChecking(true);
    checkTitle(debouncedTitle, controller.signal)
      .then((exists) => setTitleTaken(Boolean(exists)))
      .catch(() => {})
      .finally(() => setChecking(false));
    return () => controller.abort();
  }, [debouncedTitle, open]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const canSubmit = file && form.title.trim() && form.documentType && !submitting;

  const submit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError("");
    try {
      await uploadDocument({
        file,
        title: form.title.trim(),
        description: form.description.trim(),
        documentType: form.documentType,
      });
      onUploaded();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Upload New Document">
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label className="label-text">File</label>
          <input
            type="file"
            accept={UPLOAD_ACCEPT}
            onChange={(e) => setFile(e.target.files?.[0] || null)}
            className="input-field h-auto py-2 file:mr-3 file:rounded-md file:border-0 file:bg-bg-elevated file:px-3 file:py-1 file:text-sm file:text-text-primary"
          />
          <p className="mt-1 text-xs text-text-muted">PDF, DOC, DOCX, XLS, XLSX, TXT · up to 500 MB</p>
        </div>

        <div>
          <label className="label-text">Title</label>
          <input type="text" className="input-field" value={form.title} onChange={set("title")} placeholder="Document title" />
          {checking && (
            <p className="mt-1 flex items-center gap-1.5 text-xs text-text-muted">
              <Loader2 className="h-3 w-3 animate-spin" /> Checking title…
            </p>
          )}
          {!checking && form.title.trim() && titleTaken && (
            <p className="mt-1 flex items-center gap-1.5 text-xs text-warning">
              <AlertTriangle className="h-3.5 w-3.5" /> A document with this title already exists.
            </p>
          )}
          {!checking && form.title.trim() && !titleTaken && (
            <p className="mt-1 flex items-center gap-1.5 text-xs text-success">
              <CheckCircle2 className="h-3.5 w-3.5" /> Title is available.
            </p>
          )}
        </div>

        <div>
          <label className="label-text">Description</label>
          <textarea className="textarea-field" rows={3} value={form.description} onChange={set("description")} placeholder="Optional description" />
        </div>

        <div>
          <label className="label-text">Document Type</label>
          <select className="select-field" value={form.documentType} onChange={set("documentType")}>
            {DOCUMENT_TYPES.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <div className="flex items-center justify-end gap-3 pt-2">
          <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn-primary text-sm" disabled={!canSubmit}>
            {submitting ? "Uploading…" : "Upload"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
