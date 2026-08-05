"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { MoreHorizontal } from "lucide-react";

const MENU_WIDTH = 192; // w-48
const ITEM_HEIGHT = 36;
const MENU_PADDING = 8;

// Anchors below the button, flipping above it when there isn't enough
// room left at the bottom of the viewport (e.g. the table's last rows).
function computeMenuPosition(buttonRect, menuHeight) {
  const openUpward = window.innerHeight - buttonRect.bottom < menuHeight + 8;
  return {
    top: openUpward ? buttonRect.top - menuHeight - 4 : buttonRect.bottom + 4,
    left: buttonRect.right - MENU_WIDTH,
  };
}

// Dropdown shell shared by the document and folder table rows: the menu is portaled
// to <body> with fixed positioning so it can't get clipped by the table's
// overflow-x-auto scroll container. On scroll/resize it re-anchors to the button
// instead of closing (closing would also fire on the scroll-into-view a click can
// trigger). `children` is a render prop receiving `close` so each item can dismiss
// the menu before running its action.
export default function RowActionsMenu({ itemCount, children }) {
  const menuHeight = itemCount * ITEM_HEIGHT + MENU_PADDING;
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef(null);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!open) return;

    const reposition = () => {
      if (!buttonRef.current) return;
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect(), menuHeight));
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
  }, [open, menuHeight]);

  const toggleOpen = () => {
    if (!open && buttonRef.current) {
      setPosition(computeMenuPosition(buttonRef.current.getBoundingClientRect(), menuHeight));
    }
    setOpen((v) => !v);
  };

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        className="btn-ghost p-1.5"
        aria-label="More actions"
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
            <div className="py-1">{children(() => setOpen(false))}</div>
          </div>,
          document.body
        )}
    </>
  );
}
