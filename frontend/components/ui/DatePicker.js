"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

function toIsoDate(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function fromIsoDate(value) {
  if (!value) return null;
  const [y, m, d] = value.split("-").map(Number);
  return new Date(y, m - 1, d);
}

function formatDisplay(value) {
  const [y, m, d] = value.split("-");
  return `${d}/${m}/${y}`;
}

function isSameDay(a, b) {
  return (
    a &&
    b &&
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

// 6 full weeks (42 cells) so the grid height never shifts between months.
function buildGrid(viewDate) {
  const year = viewDate.getFullYear();
  const month = viewDate.getMonth();
  const startOffset = new Date(year, month, 1).getDay();
  const gridStart = new Date(year, month, 1 - startOffset);

  return Array.from({ length: 42 }, (_, i) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + i);
    return date;
  });
}

/**
 * Dark-themed calendar popover — replaces the native <input type="date">, which
 * always renders in the OS/browser's own (light) theme and, on some platforms,
 * requires scrolling each date segment to change it, which felt sluggish.
 * Selecting a day here is a single click on a grid cell.
 */
export default function DatePicker({ value, onChange, placeholder = "dd/mm/yyyy" }) {
  const [open, setOpen] = useState(false);
  const [viewDate, setViewDate] = useState(() => fromIsoDate(value) || new Date());
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return;
    const onDocClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("click", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("click", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const toggle = () => {
    if (!open) setViewDate(fromIsoDate(value) || new Date());
    setOpen((v) => !v);
  };

  const selected = fromIsoDate(value);
  const today = new Date();
  const grid = buildGrid(viewDate);

  const selectDay = (date) => {
    onChange(toIsoDate(date));
    setOpen(false);
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={toggle}
        className="input-field flex items-center justify-between text-left"
      >
        <span className={value ? "text-text-primary" : "text-text-muted"}>
          {value ? formatDisplay(value) : placeholder}
        </span>
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-1 w-64 rounded-lg border border-border-subtle bg-bg-card p-3 shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <button
              type="button"
              className="btn-ghost p-1"
              onClick={() =>
                setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() - 1, 1))
              }
              aria-label="Previous month"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-sm font-medium text-text-primary">
              {viewDate.toLocaleDateString("en-US", { month: "long", year: "numeric" })}
            </span>
            <button
              type="button"
              className="btn-ghost p-1"
              onClick={() =>
                setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 1))
              }
              aria-label="Next month"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-0.5 mb-1">
            {WEEKDAYS.map((d) => (
              <span key={d} className="text-center text-[10px] font-medium text-text-muted py-1">
                {d}
              </span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-0.5">
            {grid.map((date, i) => {
              const inMonth = date.getMonth() === viewDate.getMonth();
              const isSelected = isSameDay(date, selected);
              const isToday = isSameDay(date, today);
              return (
                <button
                  key={i}
                  type="button"
                  onClick={() => selectDay(date)}
                  className={`h-7 w-7 rounded-md text-xs transition-colors ${
                    isSelected
                      ? "bg-accent text-white font-medium"
                      : inMonth
                        ? "text-text-primary hover:bg-bg-elevated"
                        : "text-text-muted/50 hover:bg-bg-elevated"
                  } ${isToday && !isSelected ? "border border-accent/50" : ""}`}
                >
                  {date.getDate()}
                </button>
              );
            })}
          </div>

          {value && (
            <button
              type="button"
              onClick={() => {
                onChange("");
                setOpen(false);
              }}
              className="mt-2 w-full text-center text-xs text-text-muted hover:text-text-primary transition-colors"
            >
              Clear
            </button>
          )}
        </div>
      )}
    </div>
  );
}
