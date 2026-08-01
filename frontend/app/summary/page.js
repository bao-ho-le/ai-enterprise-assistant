import GenerationCreateScreen from "@/features/conversation/components/GenerationCreateScreen";

export const metadata = {
  title: "Summary — Enterprise AI Assistant",
};

export default function SummaryPage() {
  return <GenerationCreateScreen conversationType="SUMMARY_GENERATION" basePath="/summary" />;
}
