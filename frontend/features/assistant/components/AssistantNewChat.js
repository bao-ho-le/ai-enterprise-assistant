"use client";

import { useSearchParams } from "next/navigation";
import { ShieldAlert } from "lucide-react";
import AssistantCreationPanel from "./AssistantCreationPanel";
import useAssistantComposer from "@/features/assistant/hooks/useAssistantComposer";

export default function AssistantNewChat() {
  const searchParams = useSearchParams();
  const modeParam = searchParams.get("mode") || undefined;
  const composer = useAssistantComposer({ initialModeKey: modeParam });

  if (composer.modes.length === 0) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-2 overflow-hidden">
        <ShieldAlert className="h-6 w-6 text-text-muted" />
        <p className="text-sm text-text-secondary">No AI assistant tool is available for your account.</p>
        <p className="text-xs text-text-muted">Ask an administrator to grant AI permissions to your role.</p>
      </main>
    );
  }

  return (
    <main className="flex-1 overflow-y-auto">
      <div className="mx-auto w-full max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <AssistantCreationPanel composer={composer} />
      </div>
    </main>
  );
}
