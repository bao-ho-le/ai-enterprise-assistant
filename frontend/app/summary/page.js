import NoConversationSelected from "@/features/conversation/components/NoConversationSelected";

export const metadata = {
  title: "Summary — Enterprise AI Assistant",
};

export default function SummaryPage() {
  return (
    <NoConversationSelected message="Select a conversation from the sidebar, or start a new one to generate a summary." />
  );
}
