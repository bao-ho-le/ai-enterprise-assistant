import GenerationDetailView from "@/features/conversation/components/GenerationDetailView";
import RightDocumentPanel from "@/components/layout/RightDocumentPanel";

export default async function WriteReportConversationPage({ params }) {
  const { conversationId } = await params;
  return (
    <>
      <GenerationDetailView conversationId={conversationId} />
      <RightDocumentPanel conversationId={conversationId} />
    </>
  );
}
