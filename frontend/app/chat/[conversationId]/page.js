import AssistantConversation from "@/features/assistant/components/AssistantConversation";

export default async function ChatConversationPage({ params }) {
  const { conversationId } = await params;
  return <AssistantConversation conversationId={conversationId} />;
}
