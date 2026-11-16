"use client";

import Modal from "@/components/ui/Modal";
import GenerationFields from "./GenerationFields";
import {
  generationFields,
  initialGenerationValues,
} from "@/features/assistant/config/generationOptions";

// Advanced options for the generation modes, reusing the exact field schema the old
// GenerationForm rendered. The composer stays minimal; everything else lives here.
export default function GenerationOptionsSheet({
  open,
  onClose,
  conversationType,
  values,
  onChange,
  title = "Generation options",
  submitSlot = null,
}) {
  const reset = () => {
    const defaults = initialGenerationValues(conversationType);
    for (const field of generationFields(conversationType)) {
      onChange(field.name, defaults[field.name]);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={title} maxWidth="max-w-2xl">
      <div className="max-h-[65vh] overflow-y-auto pr-1">
        <GenerationFields
          conversationType={conversationType}
          values={values}
          onChange={onChange}
          idPrefix={`options-${conversationType?.toLowerCase()}`}
        />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 pt-5">
        <button type="button" className="btn-ghost text-sm" onClick={reset}>
          Reset to defaults
        </button>
        <div className="flex items-center gap-3">
          {submitSlot}
          <button type="button" className="btn-primary text-sm" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    </Modal>
  );
}
