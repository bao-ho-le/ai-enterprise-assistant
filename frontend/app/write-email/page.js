import GenerationForm from "@/features/conversation/components/GenerationForm";

export const metadata = {
  title: "Write Email — Enterprise AI Assistant",
};

export default function WriteEmailPage() {
  return <GenerationForm conversationType="EMAIL_GENERATION" basePath="/write-email" />;
}
