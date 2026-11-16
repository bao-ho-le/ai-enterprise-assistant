import { createGenerationAdapter } from "./generationAdapter";

// Report generation: grounded in at least one attached source document.
export const reportAdapter = createGenerationAdapter("REPORT_GENERATION");
