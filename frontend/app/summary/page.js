import { Sparkles } from "lucide-react";
import LeftSidebar from "@/components/layout/LeftSidebar";
import RightDocumentPanel from "@/components/layout/RightDocumentPanel";

export const metadata = {
  title: "Summary — Enterprise AI Assistant",
};

const TYPES = [
  { value: "executive", title: "Executive Summary", desc: "High-level overview for leadership", checked: true },
  { value: "bullet", title: "Bullet Summary", desc: "Key points in scannable format" },
  { value: "detailed", title: "Detailed Summary", desc: "Comprehensive section-by-section" },
  { value: "timeline", title: "Timeline", desc: "Chronological event sequence" },
  { value: "actions", title: "Action Items", desc: "Tasks, owners, and deadlines" },
  { value: "meeting", title: "Meeting Notes", desc: "Structured meeting recap" },
];

const LENGTHS = [
  { value: "short", label: "Short" },
  { value: "medium", label: "Medium", checked: true },
  { value: "long", label: "Long" },
];

export default function SummaryPage() {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar />

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
          <form className="space-y-8" aria-label="Summary options">
            <div>
              <label className="label-text">Summary Type</label>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mt-2">
                {TYPES.map((t) => (
                  <label
                    key={t.value}
                    className={`card flex items-center gap-3 p-4 cursor-pointer ${
                      t.checked ? "border-accent/50 bg-accent/5" : ""
                    }`}
                  >
                    <input type="radio" name="summary-type" value={t.value} className="accent-accent scale-125" defaultChecked={t.checked} />
                    <div>
                      <p className="text-sm font-medium text-text-primary">{t.title}</p>
                      <p className="text-xs text-text-muted mt-0.5">{t.desc}</p>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="label-text">Length</label>
              <div className="flex flex-wrap items-center gap-3 mt-2">
                {LENGTHS.map((l) => (
                  <label
                    key={l.value}
                    className={`card flex items-center gap-2 px-4 py-2.5 cursor-pointer ${
                      l.checked ? "border-accent/50 bg-accent/5" : ""
                    }`}
                  >
                    <input type="radio" name="summary-length" value={l.value} className="accent-accent scale-125" defaultChecked={l.checked} />
                    <span className="text-sm text-text-primary">{l.label}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="pt-2">
              <button type="button" className="btn-primary px-6">
                <Sparkles className="h-4 w-4" />
                Generate Summary
              </button>
            </div>
          </form>
        </div>
      </main>

      <RightDocumentPanel />
    </div>
  );
}
