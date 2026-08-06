"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { MoreHorizontal, Pencil, Trash2 } from "lucide-react";

const MENU_WIDTH = 200;
// ponytail: fixed 2-item menu height; measure menuRef after mount instead if items become dynamic.
const MENU_HEIGHT = 88;

// Anchors to the right of the button, flipping above it when there isn't
// enough room left at the bottom of the viewport.
function computeMenuPosition(buttonRect) {
  const openUpward = window.innerHeight - buttonRect.bottom < MENU_HEIGHT + 8;
  return {
    top: openUpward ? buttonRect.top - MENU_HEIGHT - 4 : buttonRect.bottom + 4,
    left: Math.min(buttonRect.left, window.innerWidth - MENU_WIDTH - 8),
  };
}

// Only rename + soft delete live here; permanent delete is on the Deleted
// Conversations screen, so it can't be hit by mistake from the active list.
export default function ConversationRowMenu({ conversation, onRename, onDelete }) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef(null);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!open) return;

    const reposition = () => {
      if (!buttonRef.current) return;
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect()));
    };
    const onOutsideClick = (e) => {
      if (!buttonRef.current?.contains(e.target) && !menuRef.current?.contains(e.target)) {
        setOpen(false);
      }
    };

    document.addEventListener("click", onOutsideClick);
    window.addEventListener("scroll", reposition, true);
    window.addEventListener("resize", reposition);
    return () => {
      document.removeEventListener("click", onOutsideClick);
      window.removeEventListener("scroll", reposition, true);
      window.removeEventListener("resize", reposition);
    };
  }, [open]);

  const toggleOpen = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!open && buttonRef.current) {
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect()));
    }
    setOpen((v) => !v);
  };

  const run = (fn) => (e) => {
    e.preventDefault();
    e.stopPropagation();
    setOpen(false);
    fn(conversation);
  };

  return (
    <>
      {/* .btn-ghost is unlayered CSS (globals.css) so its own padding beats a
          Tailwind p-* utility here — sized via inline style instead (same
          workaround ConfirmDialog/DeletedConversationsView use). 0.125rem keeps
          the button's height at 20px (= 16px icon + 4px), matching the sidebar
          row's text line-height so this row is no taller than the New
          conversation / Deleted conversations buttons above it. */}
      <button
        ref={buttonRef}
        type="button"
        className="btn-ghost"
        style={{ padding: "0.125rem" }}
        aria-label="Conversation actions"
        onClick={toggleOpen}
      >
        <MoreHorizontal className="h-4 w-4" />
      </button>
      {open &&
        createPortal(
          <div
            ref={menuRef}
            style={{ position: "fixed", top: position.top, left: position.left, width: MENU_WIDTH }}
            className="rounded-lg border border-border-subtle bg-bg-card shadow-lg z-[200]"
          >
            <div className="py-1">
              <button
                type="button"
                onClick={run(onRename)}
                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
              >
                <Pencil className="h-4 w-4 text-text-muted" />
                Rename
              </button>
              <button
                type="button"
                onClick={run(onDelete)}
                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-text-primary hover:bg-bg-elevated transition-colors"
              >
                <Trash2 className="h-4 w-4 text-text-muted" />
                Delete
              </button>
            </div>
          </div>,
          document.body
        )}
    </>
  );
}
