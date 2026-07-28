import LeftSidebar from "@/components/layout/LeftSidebar";

export default function DocumentQaLayout({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar conversationType="DOCUMENT_QA" basePath="/document-qa" />
      {children}
    </div>
  );
}
