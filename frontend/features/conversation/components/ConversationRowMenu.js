"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { MoreHorizontal, Pencil, Trash2, ShieldAlert } from "lucide-react";

const MENU_WIDTH = 200;
// ponytail: fixed 3-item menu height; measure menuRef after mount instead if items become dynamic.
const MENU_HEIGHT = 132;

// Anchors to the right of the button, flipping above it when there isn't
// enough room left at the bottom of the viewport.
function computeMenuPosition(buttonRect) {
  const openUpward = window.innerHeight - buttonRect.bottom < MENU_HEIGHT + 8;
  return {
    top: openUpward ? buttonRect.top - MENU_HEIGHT - 4 : buttonRect.bottom + 4,
    left: Math.min(buttonRect.left, window.innerWidth - MENU_WIDTH - 8),
  };
}

// Rename / soft delete / hard delete are three distinct, clearly separated
// actions — never merged into a single button or a single confirm dialog.
export default function ConversationRowMenu({ conversation, onRename, onDelete, onHardDelete }) {
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
      <button
        ref={buttonRef}
        type="button"
        className="btn-ghost p-1"
        aria-label="Conversation actions"
        onClick={toggleOpen}
      >
        <MoreHorizontal className="h-3.5 w-3.5" />
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
              <button
                type="button"
                onClick={run(onHardDelete)}
                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-error hover:bg-error/10 transition-colors"
              >
                <ShieldAlert className="h-4 w-4" />
                Delete permanently
              </button>
            </div>
          </div>,
          document.body
        )}
    </>
  );
}
