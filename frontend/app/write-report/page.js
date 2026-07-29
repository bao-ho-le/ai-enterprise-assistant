import GenerationCreateScreen from "@/features/conversation/components/GenerationCreateScreen";

export const metadata = {
  title: "Write Report — Enterprise AI Assistant",
};

export default function WriteReportPage() {
  return <GenerationCreateScreen conversationType="REPORT_GENERATION" basePath="/write-report" />;
}
