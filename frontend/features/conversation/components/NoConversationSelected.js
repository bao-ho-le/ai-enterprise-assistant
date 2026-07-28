import { MessageSquarePlus } from "lucide-react";

export default function NoConversationSelected({ message }) {
  return (
    <main className="flex-1 flex items-center justify-center overflow-hidden">
      <div className="text-center max-w-sm px-4">
        <MessageSquarePlus className="h-8 w-8 text-text-muted mx-auto mb-3" />
        <p className="text-sm text-text-secondary">{message}</p>
      </div>
    </main>
  );
}
