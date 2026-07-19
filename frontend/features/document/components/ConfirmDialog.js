"use client";

import Modal from "@/components/ui/Modal";

export default function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  loading,
  title = "Are you sure?",
  message,
  confirmLabel = "Confirm",
}) {
  return (
    <Modal open={open} onClose={onClose} title={title} maxWidth="max-w-md">
      <p className="text-sm text-text-secondary">{message}</p>
      <div className="flex items-center justify-end gap-3 mt-6">
        <button type="button" className="btn-secondary text-sm" onClick={onClose} disabled={loading}>
          Cancel
        </button>
        <button
          type="button"
          className="btn-primary text-sm bg-error hover:bg-error"
          style={{ backgroundColor: "var(--error)" }}
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? "Deleting…" : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
