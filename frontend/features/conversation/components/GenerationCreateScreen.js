"use client";

import { useEffect, useState } from "react";
import GenerationForm from "./GenerationForm";
import SelectedDocumentsPanel from "./SelectedDocumentsPanel";
import { requiresSourceDocuments } from "@/constants/conversation";

// Report/Summary generation is grounded in source documents (DocumentContextService),
// so their create-forms get a document picker; Email/Form generation don't use one.
//
// When navigated from File Storage with pre-selected documents (stored in
// sessionStorage by FileStorageView), this component reads them on mount and
// pre-populates the document panel — no auto-generation, user still clicks Generate.
export default function GenerationCreateScreen({ conversationType, basePath }) {
  const [selectedDocuments, setSelectedDocuments] = useState([]);
  const needsDocuments = requiresSourceDocuments(conversationType);

  // Restore documents passed from File Storage via sessionStorage.
  useEffect(() => {
    if (!needsDocuments) return;
    try {
      const raw = sessionStorage.getItem("file-storage-attach");
      if (raw) {
        const docs = JSON.parse(raw);
        sessionStorage.removeItem("file-storage-attach");
        if (Array.isArray(docs) && docs.length > 0) {
          setSelectedDocuments(docs);
        }
      }
    } catch {
      // Ignore parse errors — user can pick documents manually.
    }
  }, [needsDocuments]);

  return (
    <>
      <GenerationForm
        conversationType={conversationType}
        basePath={basePath}
        selectedDocumentVersionIds={needsDocuments ? selectedDocuments.map((d) => d.documentVersionId) : undefined}
      />
      {needsDocuments && <SelectedDocumentsPanel documents={selectedDocuments} onChange={setSelectedDocuments} />}
    </>
  );
}
