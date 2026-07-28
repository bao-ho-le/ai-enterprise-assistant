import GenerationDetailView from "@/features/conversation/components/GenerationDetailView";

export default async function WriteEmailConversationPage({ params }) {
  const { conversationId } = await params;
  return <GenerationDetailView conversationId={conversationId} />;
}
