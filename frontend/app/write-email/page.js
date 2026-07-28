import NoConversationSelected from "@/features/conversation/components/NoConversationSelected";

export const metadata = {
  title: "Write Email — Enterprise AI Assistant",
};

export default function WriteEmailPage() {
  return (
    <NoConversationSelected message="Select a conversation from the sidebar, or start a new one to generate an email." />
  );
}
