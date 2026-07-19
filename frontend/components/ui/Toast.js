"use client";

import { useEffect } from "react";
import { CheckCircle2, AlertCircle } from "lucide-react";

// Minimal auto-dismissing toast. `toast` = { type: 'success'|'error', text } | null
export default function Toast({ toast, onDone, duration = 3000 }) {
  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(onDone, duration);
    return () => clearTimeout(id);
  }, [toast, onDone, duration]);

  if (!toast) return null;
  const isError = toast.type === "error";

  return (
    <div className="fixed bottom-6 right-6 z-[200]">
      <div
        className={`flex items-center gap-2.5 rounded-lg border px-4 py-3 text-sm shadow-lg bg-bg-card ${
          isError ? "border-error/30 text-error" : "border-success/30 text-success"
        }`}
      >
        {isError ? <AlertCircle className="h-4 w-4" /> : <CheckCircle2 className="h-4 w-4" />}
        <span className="text-text-primary">{toast.text}</span>
      </div>
    </div>
  );
}
