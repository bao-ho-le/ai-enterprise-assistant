"use client";

import { useState } from "react";
import Modal from "@/components/ui/Modal";
import { createConversation } from "@/services/conversationService";
import { ApiError } from "@/lib/apiClient";

// conversationType is fixed by the page this sidebar is mounted on (e.g. DOCUMENT_QA) — not user-editable.
export default function CreateConversationModal({ open, onClose, conversationType, onCreated }) {
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const close = () => {
    if (submitting) return;
    setTitle("");
    setError("");
    onClose();
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!title.trim() || submitting) return;
    setSubmitting(true);
    setError("");
    try {
      const conversation = await createConversation({ title: title.trim(), conversationType });
      setTitle("");
      onCreated(conversation);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create conversation. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open={open} onClose={close} title="New conversation" maxWidth="max-w-md">
      <form className="space-y-4" onSubmit={submit}>
        <div>
          <label htmlFor="new-conversation-title" className="label-text">Title</label>
          <input
            id="new-conversation-title"
            type="text"
            className="input-field"
            placeholder="e.g. Q4 Compliance Policy Q&A"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            autoFocus
          />
        </div>

        {error && <p className="text-sm text-error">{error}</p>}

        <div className="flex items-center justify-end gap-3 pt-2">
          <button type="button" className="btn-secondary text-sm" onClick={close} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn-primary text-sm" disabled={!title.trim() || submitting}>
            {submitting ? "Creating…" : "Create"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
