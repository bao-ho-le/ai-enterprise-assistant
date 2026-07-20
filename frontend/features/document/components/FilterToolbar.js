import {
  SORT_OPTIONS,
  DOCUMENT_TYPES,
  EXTENSION_OPTIONS,
  DOCUMENT_STATUS_OPTIONS,
  STATUS_OPTIONS,
} from "@/constants/document";
import DatePicker from "@/components/ui/DatePicker";

// Controlled: `filters` object + onChange(patch). Similarity only takes effect
// while a semantic search keyword is active — it's a no-op otherwise.
export default function FilterToolbar({ filters, onChange }) {
  const set = (key) => (e) => onChange({ [key]: e.target.value });
  const setValue = (key) => (value) => onChange({ [key]: value });

  return (
    <div className="filter-toolbar mb-6">
      <div className="filter-toolbar-item filter-toolbar-item--compact">
        <label htmlFor="sort-order" className="text-xs text-text-muted whitespace-nowrap">
          Sort
        </label>
        <select id="sort-order" className="select-field" value={filters.sort} onChange={set("sort")}>
          {SORT_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--wide filter-toolbar-item--date">
        <span className="text-xs text-text-muted whitespace-nowrap">Upload Date</span>
        <div className="filter-toolbar-range">
          <DatePicker value={filters.fromDate} onChange={setValue("fromDate")} />
          <span className="filter-toolbar-range-separator" aria-hidden="true">-</span>
          <DatePicker value={filters.toDate} onChange={setValue("toDate")} />
        </div>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--wide">
        <label htmlFor="doc-type" className="text-xs text-text-muted whitespace-nowrap">
          Document Type
        </label>
        <select id="doc-type" className="select-field" value={filters.documentType} onChange={set("documentType")}>
          <option value="">All Types</option>
          {DOCUMENT_TYPES.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--compact filter-toolbar-item--xs">
        <label htmlFor="extension" className="text-xs text-text-muted whitespace-nowrap">
          Extension
        </label>
        <select id="extension" className="select-field" value={filters.extension} onChange={set("extension")}>
          {EXTENSION_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--compact filter-toolbar-item--xs">
        <label htmlFor="similarity" className="text-xs text-text-muted whitespace-nowrap">
          Similarity
        </label>
        <select id="similarity" className="select-field" value={filters.similarity} onChange={set("similarity")}>
          <option value="">Any</option>
          <option value="high">&gt; 90%</option>
          <option value="medium">70–90%</option>
          <option value="low">&lt; 70%</option>
        </select>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--compact">
        <label htmlFor="document-status" className="text-xs text-text-muted whitespace-nowrap">
          Status
        </label>
        <select
          id="document-status"
          className="select-field"
          value={filters.documentStatus}
          onChange={set("documentStatus")}
        >
          {DOCUMENT_STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-toolbar-item filter-toolbar-item--compact">
        <label htmlFor="processing-status" className="text-xs text-text-muted whitespace-nowrap">
          Processing
        </label>
        <select
          id="processing-status"
          className="select-field"
          value={filters.status}
          onChange={set("status")}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
