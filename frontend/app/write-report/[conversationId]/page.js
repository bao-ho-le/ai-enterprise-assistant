import GenerationDetailView from "@/features/conversation/components/GenerationDetailView";

export default async function WriteReportConversationPage({ params }) {
  const { conversationId } = await params;
  return <GenerationDetailView conversationId={conversationId} />;
}
