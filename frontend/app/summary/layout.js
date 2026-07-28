import LeftSidebar from "@/components/layout/LeftSidebar";

export default function SummaryLayout({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar conversationType="SUMMARY_GENERATION" basePath="/summary" />
      {children}
    </div>
  );
}
