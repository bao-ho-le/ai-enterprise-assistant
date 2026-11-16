import AssistantWorkspace from "@/features/assistant/components/AssistantWorkspace";

// Keeps the unified conversation sidebar while viewing deleted conversations, so
// restoring a conversation and returning to it is one click.
export default function DeletedConversationsLayout({ children }) {
  return <AssistantWorkspace>{children}</AssistantWorkspace>;
}
