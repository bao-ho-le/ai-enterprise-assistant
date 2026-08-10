import {
  SORT_OPTIONS,
  DOCUMENT_TYPES,
  EXTENSION_OPTIONS,
  STATUS_OPTIONS,
} from "@/constants/document";
import DatePicker from "@/components/ui/DatePicker";

// Controlled: `filters` object + onChange(patch).
//
// layout="horizontal" (default) is the original inline toolbar: a wrapping row
// of fixed-width controls. layout="vertical" is used inside the filter popover:
// Upload Date stacks as two separate rows (From/To) and every other control is
// ~50% wide in a balanced 2-column grid. The options and onChange contract are
// identical in both layouts — this is purely presentation.
export default function FilterToolbar({ filters, onChange, layout = "horizontal" }) {
  const set = (key) => (e) => onChange({ [key]: e.target.value });
  const setValue = (key) => (value) => onChange({ [key]: value });

  const vertical = layout === "vertical";

  // Width classes only apply in the horizontal row layout; in vertical mode the
  // grid cell sizes the control instead.
  const field = (widthClass, children) => (
    <div className={`filter-toolbar-item ${vertical ? "" : widthClass}`}>{children}</div>
  );

  const sortField = field(
    "filter-toolbar-item--compact",
    <>
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
    </>
  );

  const dateField = field(
    "filter-toolbar-item--wide filter-toolbar-item--date",
    <>
      <span className="text-xs text-text-muted whitespace-nowrap">Upload Date</span>
      <div className="filter-toolbar-range">
        <DatePicker value={filters.fromDate} onChange={setValue("fromDate")} />
        <span className="filter-toolbar-range-separator" aria-hidden="true">-</span>
        <DatePicker value={filters.toDate} onChange={setValue("toDate")} />
      </div>
    </>
  );

  const docTypeField = field(
    "filter-toolbar-item--wide",
    <>
      <label htmlFor="doc-type" className="text-xs text-text-muted whitespace-nowrap">
        Document Type
      </label>
      <select id="doc-type" className="select-field" value={filters.documentType} onChange={set("documentType")}>
        <option value="">All Types</option>
        {/* Display-only: hide the EMAIL_TEMPLATE option from the File Storage
            filter. Backend enum and filtering logic are untouched; other
            document types keep working as before. */}
        {DOCUMENT_TYPES.filter((o) => o.value !== "EMAIL_TEMPLATE").map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </>
  );

  const extensionField = field(
    "filter-toolbar-item--compact filter-toolbar-item--xs",
    <>
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
    </>
  );

  const processingStatusField = field(
    "filter-toolbar-item--compact",
    <>
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
    </>
  );

  if (vertical) {
    return (
      <div className="flex flex-col gap-4">
        {/* Upload Date — full width, start and end date on separate rows. */}
        <div className="filter-toolbar-item w-full">
          <span className="text-xs text-text-muted whitespace-nowrap">Upload Date</span>
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <span className="w-8 shrink-0 text-xs text-text-muted">From</span>
              <div className="min-w-0 flex-1">
                <DatePicker value={filters.fromDate} onChange={setValue("fromDate")} />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-8 shrink-0 text-xs text-text-muted">To</span>
              <div className="min-w-0 flex-1">
                <DatePicker value={filters.toDate} onChange={setValue("toDate")} />
              </div>
            </div>
          </div>
        </div>

        {/* All other controls — ~50% width each in a balanced 2-column grid. */}
        <div className="grid grid-cols-2 gap-4">
          {sortField}
          {docTypeField}
          {extensionField}
          {processingStatusField}
        </div>
      </div>
    );
  }

  return (
    <div className="filter-toolbar mb-6">
      {sortField}
      {dateField}
      {docTypeField}
      {extensionField}
      {processingStatusField}
    </div>
  );
}
