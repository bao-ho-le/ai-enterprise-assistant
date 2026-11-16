import { Suspense } from "react";
import { Loader2 } from "lucide-react";
import AssistantNewChat from "@/features/assistant/components/AssistantNewChat";

export const metadata = {
  title: "New chat — Enterprise AI Assistant",
};

// useSearchParams (?mode=…) requires a Suspense boundary in the App Router.
export default function ChatPage() {
  return (
    <Suspense
      fallback={
        <main className="flex flex-1 items-center justify-center overflow-hidden">
          <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
        </main>
      }
    >
      <AssistantNewChat />
    </Suspense>
  );
}
