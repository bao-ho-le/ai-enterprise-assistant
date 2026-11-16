"use client";

import { GenerationField, getFieldGridClass, groupFields } from "./GenerationFields";
import { generationFormSections } from "@/features/assistant/config/generationOptions";

// Renders the complete generation form, grouped into titled sections (details,
// preferences, instructions/context). Every field from the schema is visible here —
// nothing is hidden behind an options modal. The same GenerationField renderer as the
// options sheet/regenerate panel is used, so field behaviour can never drift.
export default function GenerationFormSections({ conversationType, values, onChange, idPrefix = "create" }) {
  const sections = generationFormSections(conversationType);

  return (
    <div className="space-y-6">
      {sections.map((section) => (
        <section key={section.id} className="space-y-3" aria-labelledby={`section-${conversationType}-${section.id}`}>
          <div>
            <h3
              id={`section-${conversationType}-${section.id}`}
              className="text-xs font-semibold uppercase tracking-wider text-text-secondary"
            >
              {section.title}
            </h3>
            {section.description && (
              <p className="mt-1 text-xs leading-relaxed text-text-muted">{section.description}</p>
            )}
          </div>

          <div className="space-y-4">
            {groupFields(section.fields).map((group, index) =>
              group.fields.length > 1 ? (
                <div key={index} className={`grid grid-cols-1 ${getFieldGridClass(group)} gap-3`}>
                  {group.fields.map((field) => (
                    <GenerationField
                      key={field.name}
                      field={field}
                      value={values?.[field.name]}
                      onChange={(value) => onChange(field.name, value)}
                      idPrefix={`${idPrefix}-${section.id}`}
                    />
                  ))}
                </div>
              ) : (
                <GenerationField
                  key={group.fields[0].name}
                  field={group.fields[0]}
                  value={values?.[group.fields[0].name]}
                  onChange={(value) => onChange(group.fields[0].name, value)}
                  idPrefix={`${idPrefix}-${section.id}`}
                />
              )
            )}
          </div>
        </section>
      ))}
    </div>
  );
}
