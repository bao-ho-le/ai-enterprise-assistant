import NoConversationSelected from "@/features/conversation/components/NoConversationSelected";

export const metadata = {
  title: "Write Report — Enterprise AI Assistant",
};

export default function WriteReportPage() {
  return (
    <NoConversationSelected message="Select a conversation from the sidebar, or start a new one to generate a report." />
  );
}
