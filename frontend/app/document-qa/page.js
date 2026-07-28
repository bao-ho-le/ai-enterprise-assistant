import NoConversationSelected from "@/features/conversation/components/NoConversationSelected";

export const metadata = {
  title: "Document QA — Enterprise AI Assistant",
};

export default function DocumentQAPage() {
  return (
    <NoConversationSelected message="Select a conversation from the sidebar, or start a new one to ask questions about your documents." />
  );
}
