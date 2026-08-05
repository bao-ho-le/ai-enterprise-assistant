import FileStorageView from "@/features/document/components/FileStorageView";
import { folderIdFromSegment } from "@/utils/folderPath";

export const metadata = {
  title: "File Storage — Enterprise AI Assistant",
};

// Keyed by folder id so opening a different folder resets the table (page, selection),
// while rewriting the path to its canonical form (same id) does not remount.
export default async function FolderContentsPage({ params }) {
  const { folderPath } = await params;
  const folderId = folderIdFromSegment(folderPath[folderPath.length - 1]);

  return <FileStorageView key={String(folderId)} folderPath={folderPath} folderId={folderId} />;
}
