import LeftSidebar from "@/components/layout/LeftSidebar";

export default function WriteReportLayout({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar conversationType="REPORT_GENERATION" basePath="/write-report" />
      {children}
    </div>
  );
}
