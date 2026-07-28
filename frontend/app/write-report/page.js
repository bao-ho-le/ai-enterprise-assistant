import GenerationForm from "@/features/conversation/components/GenerationForm";

export const metadata = {
  title: "Write Report — Enterprise AI Assistant",
};

export default function WriteReportPage() {
  return <GenerationForm conversationType="REPORT_GENERATION" basePath="/write-report" />;
}
