// File Storage folder URLs.
//
// Document details already own /file-storage/[id], so folder browsing lives under
// /file-storage/folders/<segment>/<segment>/…  Each segment is "<slug>-<folderId>":
// the slug is there to make the URL readable, only the trailing id is authoritative.
// That keeps a direct load (refresh, pasted link) resolving to exactly one folder
// even when the same folder name exists at several levels.

const BASE = "/file-storage";
const FOLDERS_BASE = `${BASE}/folders`;

function slugify(name) {
  return (name || "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    // đ/Đ carries its stroke in the base letter, so NFD leaves it behind.
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function folderSegment({ id, name }) {
  const slug = slugify(name);
  return slug ? `${slug}-${id}` : String(id);
}

export function folderIdFromSegment(segment) {
  if (!segment) return null;
  const id = Number(segment.slice(segment.lastIndexOf("-") + 1));
  return Number.isInteger(id) && id > 0 ? id : null;
}

export function hrefForSegments(segments) {
  return segments.length ? `${FOLDERS_BASE}/${segments.map(encodeURIComponent).join("/")}` : BASE;
}

// The backend breadcrumb runs root -> current folder inclusive, and root is the page
// root (/file-storage), so it never becomes a segment of its own.
export function hrefForBreadcrumb(breadcrumb, depth = breadcrumb.length) {
  return hrefForSegments(breadcrumb.slice(1, depth).map(folderSegment));
}

// A folder reached by id alone (e.g. "go to folder" from search) — the folder page
// rewrites this to the full path once the backend returns the breadcrumb.
export function hrefForFolderId(folderId) {
  return folderId == null ? BASE : hrefForSegments([String(folderId)]);
}
