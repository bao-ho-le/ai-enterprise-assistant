import { createGenerationAdapter } from "./generationAdapter";

// Email generation: no source documents required, no attachedDocuments in the detail
// response (backend returns null for EMAIL_GENERATION).
export const emailAdapter = createGenerationAdapter("EMAIL_GENERATION");
