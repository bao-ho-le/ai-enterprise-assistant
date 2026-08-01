import LeftSidebar from "@/components/layout/LeftSidebar";

export default function WriteEmailLayout({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar conversationType="EMAIL_GENERATION" basePath="/write-email" />
      {children}
    </div>
  );
}
