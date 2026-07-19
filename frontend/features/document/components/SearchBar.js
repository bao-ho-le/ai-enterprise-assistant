import { Info, Search } from "lucide-react";

export default function SearchBar({ value, onChange }) {
  return (
    <div className="relative w-full max-w-3xl mx-auto">
      <div className="relative">
        <p className="mt-3 mb-2 flex items-center gap-1.5 text-xs text-text-muted">
          <Info className="h-3.5 w-3.5" />
          Powered by vector embeddings — search by meaning, not just file names
        </p>

        <div className="flex h-12 items-center rounded-lg border border-border-subtle bg-bg-card px-5">
          <input
            type="search"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Ex: Q4 revenue projections for enterprise clients"
            className="flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
            aria-label="Semantic search"
          />
          <Search className="h-5 w-5 text-text-muted ml-3" />
        </div>
      </div>
    </div>
  );
}
