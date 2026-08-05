"use client";

import { Info, Search } from "lucide-react";

// `leftSlot` (optional) renders immediately to the left of the search input —
// used by the File Storage page's filter popover trigger, so the filter icon
// sits directly beside the semantic search bar.
//
// The plain info icon to the right of the input explains what semantic search
// is. It has no button chrome and no click behaviour — the tooltip is hover-only
// (CSS group-hover, desktop).
//
// The input carries aria-describedby pointing at the tooltip, which is always in
// the DOM (only visually hidden). That is deliberate: with a hover-only CSS
// tooltip and no JS state, this is the only way screen-reader users get the
// explanation — don't hide the tooltip from assistive tech.
export default function SearchBar({ value, onChange, leftSlot }) {
  return (
    <div className="relative w-full max-w-3xl mx-auto">
      <div className="relative">
        <div className="flex items-center gap-2">
          {leftSlot}
          <div className="flex h-12 flex-1 items-center rounded-lg border border-border-subtle bg-bg-card px-5">
            <input
              type="search"
              value={value}
              onChange={(e) => onChange(e.target.value)}
              placeholder="Ex: Q4 revenue projections for enterprise clients"
              className="flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
              aria-label="Semantic search"
              aria-describedby="semantic-search-info"
            />
            <Search className="h-5 w-5 text-text-muted ml-3" />
          </div>

          <div className="group relative flex shrink-0 items-center">
            {/* Default cursor on hover — the icon is not interactive, only its
                tooltip is; the color shift alone signals the hover. */}
            <Info
              className="h-5 w-5 text-text-muted transition-colors group-hover:text-text-primary"
              aria-hidden={false}
              aria-label="About semantic search"
            />
            <div
              id="semantic-search-info"
              role="tooltip"
              className="pointer-events-none absolute right-0 top-full z-20 mt-2 w-64 rounded-lg border border-border-subtle bg-bg-card p-3 text-xs leading-relaxed text-text-secondary opacity-0 shadow-lg transition-opacity duration-150 group-hover:opacity-100"
            >
              <p>Powered by vector embeddings</p>
              <p>Search by meaning, not just file names</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
