import { Paperclip, Send } from "lucide-react";
import LeftSidebar from "@/components/layout/LeftSidebar";
import RightDocumentPanel from "@/components/layout/RightDocumentPanel";

export const metadata = {
  title: "Document QA — Enterprise AI Assistant",
};

const ANSWER = [
  { n: "1.", text: "Customer data must be retained for a minimum of 7 years after account closure, per Section 4.2 of the Compliance Policy.", cites: ["1", "3"] },
  { n: "2.", text: "Financial records require 10-year retention aligned with SOX requirements, as noted in the Board Meeting Minutes (Item 3.1).", cites: ["1", "2", "3"] },
  { n: "3.", text: "Employee records follow a 5-year post-termination retention period, with immediate deletion of biometric data upon separation.", cites: ["1", "5"] },
  { n: "4.", text: "All retention schedules must be reviewed annually by the Data Governance Committee, with the next review scheduled for Q3 2026.", cites: ["3"] },
];

function Cite({ value }) {
  return (
    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-bg-elevated border border-border-subtle text-[10px] text-text-secondary">
      {value}
    </span>
  );
}

export default function DocumentQAPage() {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar />

      <main className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
          <div className="max-w-3xl mx-auto space-y-6">
            {/* User message */}
            <div className="flex gap-4 flex-row-reverse">
              <div className="flex-1 min-w-0 flex flex-col items-end">
                <div className="rounded-xl px-4 py-3 bg-bg-elevated border border-border-subtle max-w-[85%]">
                  <p className="text-sm text-text-primary leading-relaxed text-left">
                    What are the key compliance requirements mentioned in our 2026 policy documents
                    regarding data retention?
                  </p>
                </div>
                <p className="text-xs text-text-muted mt-1.5 px-1 text-right">Jul 5, 2026 · 10:32 AM</p>
              </div>
            </div>

            {/* Assistant message */}
            <div className="flex gap-4">
              <div className="flex-1 min-w-0 w-full">
                <div>
                  <p className="text-sm text-text-primary leading-relaxed">
                    Based on the Compliance Policy 2026 and Board Meeting Minutes, the key data retention
                    requirements are:
                  </p>
                  <ul className="mt-3 space-y-2 text-sm text-text-secondary">
                    {ANSWER.map((item) => (
                      <li key={item.n} className="flex items-start justify-between gap-3">
                        <div className="flex gap-2">
                          <span className="text-accent">{item.n}</span>
                          <span>{item.text}</span>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          {item.cites.map((c, i) => (
                            <Cite key={i} value={c} />
                          ))}
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
                <p className="text-xs text-text-muted mt-1.5 px-1">Jul 5, 2026 · 10:32 AM</p>
              </div>
            </div>

            {/* User message */}
            <div className="flex gap-4 flex-row-reverse">
              <div className="flex-1 min-w-0 flex flex-col items-end">
                <div className="rounded-xl px-4 py-3 bg-bg-elevated border border-border-subtle max-w-[85%]">
                  <p className="text-sm text-text-primary leading-relaxed text-left">
                    Are there any exceptions for EU customers under GDPR?
                  </p>
                </div>
                <p className="text-xs text-text-muted mt-1.5 px-1 text-right">Jul 5, 2026 · 10:34 AM</p>
              </div>
            </div>

            {/* Typing indicator */}
            <div className="flex items-center gap-2 text-sm text-text-muted">
              <div className="flex items-center gap-1">
                <span className="h-1 w-1 rounded-full bg-text-muted animate-pulse" />
                <span className="h-1 w-1 rounded-full bg-text-muted animate-pulse [animation-delay:0.2s]" />
                <span className="h-1 w-1 rounded-full bg-text-muted animate-pulse [animation-delay:0.4s]" />
              </div>
              <span className="tracking-wide">Working</span>
            </div>
          </div>
        </div>

        {/* Chat input */}
        <div className="p-4">
          <div className="flex items-end gap-3 max-w-3xl mx-auto">
            <div className="flex-1">
              <div className="flex items-center gap-2 rounded-xl border border-border-subtle bg-bg-card px-3 py-2 min-h-[48px]">
                <textarea
                  rows={1}
                  placeholder="Ask a question about your documents..."
                  className="flex-1 resize-none bg-transparent text-sm text-text-primary outline-none leading-normal"
                  aria-label="Chat message input"
                />
                <button type="button" className="flex h-8 w-8 items-center justify-center rounded-md text-text-muted hover:bg-bg-elevated" aria-label="Attach file">
                  <Paperclip className="h-4 w-4" />
                </button>
                <button type="button" className="flex h-8 w-8 items-center justify-center rounded-md bg-bg-elevated text-text-muted hover:bg-bg-card" aria-label="Send message">
                  <Send className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
          <p className="text-center text-xs text-text-muted mt-2">
            AI responses are grounded in your selected source documents
          </p>
        </div>
      </main>

      <RightDocumentPanel />
    </div>
  );
}
