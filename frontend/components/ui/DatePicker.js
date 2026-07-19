"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, ChevronDown } from "lucide-react";

const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];
const MONTHS = [
  "Jan", "Feb", "Mar", "Apr", "May", "Jun",
  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
];
// 100 years back from today covers any realistic document date without
// needing a separate decade-grid view — just two dropdowns to jump straight
// to a year/month instead of clicking the month arrow dozens of times.
const YEAR_OPTIONS = Array.from(
  { length: 100 },
  (_, i) => new Date().getFullYear() - i
);

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

// Outside-click must be detected on mousedown, not click: the option buttons
// inside these popovers close themselves (via setOpen(false)) as part of their
// own click handler, which unmounts the clicked node before the click event
// finishes bubbling to document — `ref.contains(e.target)` then sees a
// detached node and reads as "outside", closing the parent calendar too.
function useOutsideClick(ref, active, onOutside) {
  useEffect(() => {
    if (!active) return;
    const onDocMouseDown = (e) => {
      if (ref.current && !ref.current.contains(e.target)) onOutside();
    };
    document.addEventListener("mousedown", onDocMouseDown);
    return () => document.removeEventListener("mousedown", onDocMouseDown);
  }, [active, ref, onOutside]);
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
  const [monthOpen, setMonthOpen] = useState(false);
  const [yearOpen, setYearOpen] = useState(false);
  const [viewDate, setViewDate] = useState(() => fromIsoDate(value) || new Date());
  const ref = useRef(null);
  const monthRef = useRef(null);
  const yearRef = useRef(null);
  const selectedYearRef = useRef(null);

  useOutsideClick(ref, open, () => setOpen(false));
  useOutsideClick(monthRef, monthOpen, () => setMonthOpen(false));
  useOutsideClick(yearRef, yearOpen, () => setYearOpen(false));

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  // Scrolls the year list to the selected year each time it opens — the list
  // itself stays scrollable, only the month grid dropped that need.
  useEffect(() => {
    if (!yearOpen) return;
    selectedYearRef.current?.scrollIntoView({ block: "center" });
  }, [yearOpen]);

  useEffect(() => {
    if (!open) {
      setMonthOpen(false);
      setYearOpen(false);
    }
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
            <div className="flex items-center gap-1">
              <div className="relative" ref={monthRef}>
                <button
                  type="button"
                  aria-label="Month"
                  onClick={() => setMonthOpen((v) => !v)}
                  className="flex items-center gap-0.5 rounded-md border border-border-subtle bg-bg-elevated px-1.5 py-0.5 text-xs font-medium text-text-primary focus:outline-none focus:border-accent"
                >
                  {MONTHS[viewDate.getMonth()]}
                  <ChevronDown className="h-3 w-3" />
                </button>
                {monthOpen && (
                  <div className="absolute left-0 top-full z-10 mt-1 grid w-32 grid-cols-3 gap-0.5 rounded-md border border-border-subtle bg-bg-card p-1 shadow-lg">
                    {MONTHS.map((m, i) => {
                      const isSelected = i === viewDate.getMonth();
                      return (
                        <button
                          key={m}
                          type="button"
                          onClick={() => {
                            setViewDate(new Date(viewDate.getFullYear(), i, 1));
                            setMonthOpen(false);
                          }}
                          className={`rounded px-2 py-1 text-center text-xs transition-colors ${
                            isSelected
                              ? "bg-accent text-white font-medium"
                              : "text-text-primary hover:bg-bg-elevated"
                          }`}
                        >
                          {m}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
              <div className="relative" ref={yearRef}>
                <button
                  type="button"
                  aria-label="Year"
                  onClick={() => setYearOpen((v) => !v)}
                  className="flex items-center gap-0.5 rounded-md border border-border-subtle bg-bg-elevated px-1.5 py-0.5 text-xs font-medium text-text-primary focus:outline-none focus:border-accent"
                >
                  {viewDate.getFullYear()}
                  <ChevronDown className="h-3 w-3" />
                </button>
                {yearOpen && (
                  <div className="absolute left-0 top-full z-10 mt-1 max-h-48 w-20 overflow-y-auto rounded-md border border-border-subtle bg-bg-card shadow-lg">
                    {YEAR_OPTIONS.map((y) => {
                      const isSelected = y === viewDate.getFullYear();
                      return (
                        <button
                          key={y}
                          type="button"
                          ref={isSelected ? selectedYearRef : undefined}
                          onClick={() => {
                            setViewDate(new Date(y, viewDate.getMonth(), 1));
                            setYearOpen(false);
                          }}
                          className={`block w-full px-2 py-1 text-left text-xs transition-colors ${
                            isSelected
                              ? "bg-accent text-white font-medium"
                              : "text-text-primary hover:bg-bg-elevated"
                          }`}
                        >
                          {y}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
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
