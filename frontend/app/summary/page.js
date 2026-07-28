import GenerationForm from "@/features/conversation/components/GenerationForm";

export const metadata = {
  title: "Summary — Enterprise AI Assistant",
};

export default function SummaryPage() {
  return <GenerationForm conversationType="SUMMARY_GENERATION" basePath="/summary" />;
}
