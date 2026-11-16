import AssistantWorkspace from "@/features/assistant/components/AssistantWorkspace";

export const metadata = {
  title: "Assistant — Enterprise AI Assistant",
};

export default function ChatLayout({ children }) {
  return <AssistantWorkspace>{children}</AssistantWorkspace>;
}
