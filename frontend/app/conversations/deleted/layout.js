import LeftSidebar from "@/components/layout/LeftSidebar";

// Same shell as the other conversation screens. This page isn't scoped to one type, so
// the sidebar sits on the first routed type — its own filter still browses the others.
export default function DeletedConversationsLayout({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar conversationType="EMAIL_GENERATION" basePath="/write-email" />
      {children}
    </div>
  );
}
