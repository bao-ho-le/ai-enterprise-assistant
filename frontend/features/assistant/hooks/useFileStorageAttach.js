"use client";

import { useEffect, useState } from "react";

// File Storage's "attach to feature" action stashes the resolved document versions in
// sessionStorage and navigates. The unified workspace reads and clears that stash on
// mount, exactly like the old per-feature create screens did — the key itself is
// unchanged so FileStorageView keeps working.
export const FILE_STORAGE_ATTACH_KEY = "file-storage-attach";

export default function useFileStorageAttach() {
  const [documents, setDocuments] = useState([]);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(FILE_STORAGE_ATTACH_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      sessionStorage.removeItem(FILE_STORAGE_ATTACH_KEY);
      if (Array.isArray(parsed) && parsed.length > 0) {
        setDocuments(parsed.filter((doc) => doc?.documentVersionId != null));
      }
    } catch {
      // Ignore malformed/stale payloads — the user can still pick documents manually.
    }
  }, []);

  return [documents, setDocuments];
}
