import { apiClient } from "@/lib/apiClient";
import { filenameFromContentDisposition, saveBlob } from "@/utils/format";

// All document endpoints live here. Components/hooks call these, never fetch directly.

// GET /documents  (filter + Pageable) -> Spring Page<DocumentListResponse>
export function listDocuments(params, signal) {
  return apiClient.get("/documents", { params, signal });
}

// GET /documents/{id} -> DocumentDetailResponse
export function getDocument(id, signal) {
  return apiClient.get(`/documents/${id}`, { signal });
}

// GET /documents/check-title?title= -> boolean (true = already exists)
export function checkTitle(title, signal) {
  return apiClient.get("/documents/check-title", { params: { title }, signal });
}

// POST /documents/upload  (multipart: file + request JSON)
export function uploadDocument({ file, title, description, documentType }) {
  const form = new FormData();
  form.append("file", file);
  form.append(
    "request",
    new Blob([JSON.stringify({ title, description, documentType })], {
      type: "application/json",
    })
  );
  return apiClient.postForm("/documents/upload", form);
}

// POST /documents/{id}/versions  (multipart: file + request JSON)
// title/description/documentType are optional — when sent, the backend updates
// the parent document's metadata alongside creating the new version.
export function uploadNewVersion(id, { file, changeNote, title, description, documentType }) {
  const form = new FormData();
  form.append("file", file);
  form.append(
    "request",
    new Blob([JSON.stringify({ changeNote, title, description, documentType })], {
      type: "application/json",
    })
  );
  return apiClient.postForm(`/documents/${id}/versions`, form);
}

// PUT /documents/{id}
export function updateMetadata(id, { title, description, documentType }) {
  return apiClient.putJson(`/documents/${id}`, { title, description, documentType });
}

// DELETE /documents/{id}
export function deleteDocument(id) {
  return apiClient.del(`/documents/${id}`);
}

// GET /documents/{documentId}/{versionId}/download -> save file to disk
export async function downloadVersion(documentId, versionId, fallbackName) {
  const res = await apiClient.getRaw(
    `/documents/${documentId}/${versionId}/download`
  );
  const blob = await res.blob();
  const name = filenameFromContentDisposition(
    res.headers.get("Content-Disposition"),
    fallbackName || `document-${documentId}-v${versionId}`
  );
  saveBlob(blob, name);
}

// Convenience: download current version (list rows have no versionId -> fetch detail first)
export async function downloadCurrentVersion(documentId) {
  const detail = await getDocument(documentId);
  const versionId = detail?.currentVersion?.versionId;
  if (!versionId) throw new Error("Document has no current version to download");
  return downloadVersion(
    documentId,
    versionId,
    detail?.currentVersion?.fileName
  );
}
