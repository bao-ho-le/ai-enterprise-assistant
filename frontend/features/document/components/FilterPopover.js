"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { SlidersHorizontal } from "lucide-react";
import FilterToolbar from "./FilterToolbar";

const POPOVER_WIDTH = 340;
// Estimated popover height used to decide whether it opens below or above the
// trigger (date rows + filter grid + footer); the real max-height is clamped
// to the viewport and the control list scrolls when it doesn't fit.
const ESTIMATED_HEIGHT = 460;

// Anchors below the trigger button, flipping above it when there is more room
// there; clamps to the viewport and reports how tall the popover may be.
function computePosition(buttonRect) {
  const spaceBelow = window.innerHeight - buttonRect.bottom;
  const openUpward = spaceBelow < ESTIMATED_HEIGHT && buttonRect.top > spaceBelow;

  let top;
  let maxHeight;
  if (openUpward) {
    top = buttonRect.top - ESTIMATED_HEIGHT - 8;
    maxHeight = buttonRect.top - 8;
  } else {
    top = buttonRect.bottom + 8;
    maxHeight = window.innerHeight - top - 16;
  }

  return {
    top: Math.max(8, top),
    left: Math.max(8, Math.min(buttonRect.left, window.innerWidth - POPOVER_WIDTH - 8)),
    maxHeight: Math.max(240, maxHeight),
  };
}

// Filter trigger + popover for the File Storage page. The popover is portaled to
// <body> with fixed positioning so it can't be clipped by the page layout, and
// it re-anchors to the trigger on scroll/resize.
//
// Editing happens on a local draft so changes only take effect when the user
// presses "Apply Filters" (or "Clear All"); closing without applying discards
// the draft. Filtering/search logic lives entirely in FileStorageView — this
// component only decides when the committed `filters` value changes.
export default function FilterPopover({ filters, defaults, onApply, active = false }) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(filters);
  const [position, setPosition] = useState({ top: 0, left: 0, maxHeight: 240 });
  const triggerRef = useRef(null);
  const popoverRef = useRef(null);

  useEffect(() => {
    if (!open) return;

    const reposition = () => {
      if (triggerRef.current) {
        setPosition(computePosition(triggerRef.current.getBoundingClientRect()));
      }
    };
    // mousedown, not click: the DatePickers inside the popover unmount their own
    // popover during a click, which would make `contains` read "outside" and
    // close this popover the moment a date is picked. See DatePicker's notes.
    const onDocMouseDown = (e) => {
      if (triggerRef.current?.contains(e.target) || popoverRef.current?.contains(e.target)) return;
      // The calendar panel is portaled to <body> so it escapes this popover's
      // scroll clipping — it is visually inside, so it must not count as outside.
      if (e.target instanceof Element && e.target.closest("[data-datepicker-panel]")) return;
      setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", onDocMouseDown);
    document.addEventListener("keydown", onKey);
    window.addEventListener("scroll", reposition, true);
    window.addEventListener("resize", reposition);
    return () => {
      document.removeEventListener("mousedown", onDocMouseDown);
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("scroll", reposition, true);
      window.removeEventListener("resize", reposition);
    };
  }, [open]);

  const toggleOpen = () => {
    if (!open) {
      // Re-seed the draft from the committed filters each time the popover opens.
      setDraft(filters);
      if (triggerRef.current) {
        setPosition(computePosition(triggerRef.current.getBoundingClientRect()));
      }
    }
    setOpen((v) => !v);
  };

  const handleDraftChange = (patch) => {
    // Same guard as before: EMAIL_TEMPLATE is hidden from the Document Type
    // filter (display-only), so ignore any stale value.
    if (patch.documentType === "EMAIL_TEMPLATE") patch.documentType = "";
    setDraft((d) => ({ ...d, ...patch }));
  };

  const handleApply = () => {
    onApply(draft);
    setOpen(false);
  };

  const handleClearAll = () => {
    const reset = { ...defaults };
    setDraft(reset);
    onApply(reset);
    setOpen(false);
  };

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        onClick={toggleOpen}
        aria-label="Filters"
        aria-haspopup="dialog"
        aria-expanded={open}
        className={`relative flex h-12 w-12 shrink-0 items-center justify-center rounded-lg border transition-colors ${
          open
            ? "border-accent/60 bg-bg-card text-accent"
            : "border-border-subtle bg-bg-card text-text-secondary hover:border-border-default hover:text-text-primary"
        }`}
      >
        <SlidersHorizontal className="h-5 w-5" />
        {active && (
          <span
            className="absolute right-1 top-1 h-1.5 w-1.5 rounded-full bg-accent ring-2 ring-bg-card"
            aria-hidden="true"
          />
        )}
      </button>

      {open &&
        createPortal(
          <div
            ref={popoverRef}
            role="dialog"
            aria-label="Filters"
            style={{
              position: "fixed",
              top: position.top,
              left: position.left,
              width: POPOVER_WIDTH,
              maxHeight: position.maxHeight,
            }}
            className="z-[200] flex flex-col overflow-hidden rounded-lg border border-border-subtle bg-bg-card shadow-lg"
          >
            <div className="min-h-0 flex-1 overflow-y-auto px-4 py-4">
              <FilterToolbar filters={draft} onChange={handleDraftChange} layout="vertical" />
            </div>

            <div className="flex shrink-0 items-center justify-end gap-2 border-t border-border-subtle px-4 py-3">
              <button
                type="button"
                onClick={handleClearAll}
                className="btn-secondary py-2 px-3 text-sm"
              >
                Clear All
              </button>
              <button
                type="button"
                onClick={handleApply}
                className="btn-primary px-4 py-2 text-sm"
              >
                Apply Filters
              </button>
            </div>
          </div>,
          document.body
        )}
    </>
  );
}
