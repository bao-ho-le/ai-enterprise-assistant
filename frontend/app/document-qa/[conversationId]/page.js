import DocumentQaDetailView from "@/features/conversation/components/DocumentQaDetailView";
import RightDocumentPanel from "@/components/layout/RightDocumentPanel";

export default async function DocumentQaConversationPage({ params }) {
  const { conversationId } = await params;
  return (
    <>
      <DocumentQaDetailView conversationId={conversationId} />
      <RightDocumentPanel conversationId={conversationId} />
    </>
  );
}
