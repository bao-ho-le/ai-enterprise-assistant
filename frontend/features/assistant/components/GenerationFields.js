"use client";

import DatePicker from "@/components/ui/DatePicker";
import { generationFields } from "@/features/assistant/config/generationOptions";

// Field renderer carried over from the old GenerationForm so every generation option
// (including the summary "cards" picker and the date pickers) keeps working unchanged.
// Used by GenerationOptionsSheet and by the regenerate panel.

const ROW_GRID_COLS = { 2: "sm:grid-cols-2", 3: "sm:grid-cols-3", 4: "sm:grid-cols-4" };

// Tailwind's scanner needs literal class names, so no `grid-cols-${n}` interpolation.
export function getFieldGridClass(group) {
  if (group.fields.length < 2) return "";
  if (group.row === "period" || (group.row === "details" && group.fields.length === 2)) {
    return "sm:grid-cols-3";
  }
  return ROW_GRID_COLS[group.fields.length] || "sm:grid-cols-2";
}

// Consecutive fields sharing the same `row` render side by side in one grid.
export function groupFields(fields) {
  const groups = [];
  for (const field of fields) {
    const last = groups[groups.length - 1];
    if (field.row && last?.row === field.row) last.fields.push(field);
    else groups.push({ row: field.row || null, fields: [field] });
  }
  return groups;
}

export function GenerationField({ field, value, onChange, idPrefix = "gen" }) {
  const inputId = `${idPrefix}-${field.name}`;

  if (field.type === "checkbox") {
    return (
      <label htmlFor={inputId} className="flex items-center gap-2">
        <input
          id={inputId}
          type="checkbox"
          className="h-4 w-4 accent-accent"
          checked={Boolean(value)}
          onChange={(event) => onChange(event.target.checked)}
        />
        <span className="text-sm text-text-secondary">{field.label}</span>
      </label>
    );
  }

  if (field.type === "cards") {
    return (
      <div>
        <p className="label-text">
          {field.label}
          {field.required && <span className="text-error"> *</span>}
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 auto-rows-fr">
          {field.options.map((option) => {
            const selected = value === option.value;
            return (
              <button
                type="button"
                key={option.value}
                onClick={() => onChange(option.value)}
                className={`h-full w-full rounded-lg border p-4 transition-colors flex items-center gap-4 ${
                  selected ? "border-accent/50 bg-accent/5" : "border-border-subtle hover:border-border-default"
                }`}
              >
                <span
                  className={`flex h-4 w-4 shrink-0 items-center justify-center rounded-full border ${
                    selected ? "border-accent" : "border-border-default"
                  }`}
                >
                  {selected && <span className="h-2 w-2 rounded-full bg-accent" />}
                </span>
                <div className="flex-1 flex flex-col justify-center min-w-0 text-left">
                  <span className="text-sm font-medium text-text-primary">{option.title}</span>
                  <p className="text-xs text-text-muted mt-0.5">{option.description}</p>
                </div>
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  if (field.type === "button-group") {
    return (
      <div>
        <p className="label-text">
          {field.label}
          {field.required && <span className="text-error"> *</span>}
        </p>
        <div className="flex flex-wrap gap-2">
          {field.options.map((option) => {
            const selected = value === option;
            return (
              <button
                type="button"
                key={option}
                onClick={() => onChange(option)}
                className={`rounded-full border px-4 py-1.5 text-sm transition-colors ${
                  selected
                    ? "border-accent/50 bg-accent/5 text-text-primary"
                    : "border-border-subtle text-text-secondary hover:border-border-default"
                }`}
              >
                {option}
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div>
      <label htmlFor={inputId} className="label-text">
        {field.label}
        {field.required && <span className="text-error"> *</span>}
      </label>
      {field.type === "textarea" ? (
        <textarea
          id={inputId}
          rows={4}
          className="textarea-field resize-none"
          placeholder={field.placeholder}
          value={value ?? ""}
          onChange={(event) => onChange(event.target.value)}
        />
      ) : field.type === "select" ? (
        <select
          id={inputId}
          className="select-field w-full"
          value={value ?? ""}
          onChange={(event) => onChange(event.target.value)}
        >
          {!field.required && !field.default && <option value="">—</option>}
          {field.options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      ) : field.type === "date" ? (
        <DatePicker value={value ?? ""} onChange={onChange} />
      ) : (
        <input
          id={inputId}
          type="text"
          className="input-field"
          placeholder={field.placeholder}
          value={value ?? ""}
          onChange={(event) => onChange(event.target.value)}
        />
      )}
    </div>
  );
}

export default function GenerationFields({ conversationType, values, onChange, idPrefix = "gen" }) {
  const fields = generationFields(conversationType);

  return (
    <div className="space-y-4">
      {groupFields(fields).map((group, index) =>
        group.fields.length > 1 ? (
          <div key={index} className={`grid grid-cols-1 ${getFieldGridClass(group)} gap-4`}>
            {group.fields.map((field) => (
              <GenerationField
                key={field.name}
                field={field}
                value={values?.[field.name]}
                onChange={(value) => onChange(field.name, value)}
                idPrefix={idPrefix}
              />
            ))}
          </div>
        ) : (
          <GenerationField
            key={group.fields[0].name}
            field={group.fields[0]}
            value={values?.[group.fields[0].name]}
            onChange={(value) => onChange(group.fields[0].name, value)}
            idPrefix={idPrefix}
          />
        )
      )}
    </div>
  );
}
