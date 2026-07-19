import { Sparkles, UploadCloud, FileText, X } from "lucide-react";
import LeftSidebar from "@/components/layout/LeftSidebar";
import RightDocumentPanel from "@/components/layout/RightDocumentPanel";

export const metadata = {
  title: "Write Report — Enterprise AI Assistant",
};

export default function WriteReportPage() {
  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar />

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
          <form className="space-y-6" aria-label="Report composition form">
            <div>
              <label className="label-text">Report Type</label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-1">
                <label className="card flex items-center gap-3 p-4 cursor-pointer border-accent/50 bg-accent/5">
                  <input type="radio" name="report-type" value="progress" className="accent-accent scale-125" defaultChecked />
                  <div>
                    <p className="text-sm font-medium text-text-primary">Progress Report</p>
                    <p className="text-xs text-text-muted">Track milestones and deliverables</p>
                  </div>
                </label>
                <label className="card flex items-center gap-3 p-4 cursor-pointer">
                  <input type="radio" name="report-type" value="financial" className="accent-accent scale-125" />
                  <div>
                    <p className="text-sm font-medium text-text-primary">Financial Report</p>
                    <p className="text-xs text-text-muted">Revenue, costs, and forecasts</p>
                  </div>
                </label>
              </div>
            </div>

            <div>
              <label htmlFor="report-purpose" className="label-text">Purpose</label>
              <textarea
                id="report-purpose"
                rows={7}
                className="textarea-field resize-none"
                placeholder="Describe the report purpose"
                defaultValue="Monthly progress report for the Enterprise AI Platform rollout, covering engineering milestones, adoption metrics, and blockers for leadership review."
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="report-period" className="label-text">Report Period</label>
                <input type="text" id="report-period" className="input-field" defaultValue="June 1 – June 30, 2026" />
              </div>
              <div>
                <label htmlFor="report-audience" className="label-text">Audience</label>
                <select id="report-audience" className="select-field" defaultValue="leadership">
                  <option value="leadership">Executive Leadership</option>
                  <option value="management">Department Management</option>
                  <option value="team">Project Team</option>
                  <option value="stakeholders">External Stakeholders</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="report-context" className="label-text">Additional Context</label>
              <textarea
                id="report-context"
                rows={7}
                className="textarea-field resize-none"
                placeholder="Any additional context"
                defaultValue="Focus on the Document QA and File Storage modules. Include adoption numbers from the pilot group of 45 users across 3 departments."
              />
            </div>

            <div>
              <label className="label-text">Attach Documents</label>
              <div className="card border-dashed p-6 text-center">
                <UploadCloud className="h-8 w-8 text-text-muted mx-auto mb-3" />
                <p className="text-sm text-text-secondary mb-1">Drag and drop files here, or click to browse</p>
                <p className="text-xs text-text-muted">PDF, DOCX, XLSX up to 25 MB each</p>
                <button type="button" className="btn-secondary mt-4 text-sm">Browse Files</button>
              </div>
              <ul className="mt-3 space-y-2">
                <li className="flex items-center gap-3 card p-3">
                  <FileText className="h-4 w-4 text-red-400" />
                  <span className="text-sm text-text-primary flex-1 truncate">Sprint-Velocity-June.xlsx</span>
                  <span className="text-xs text-text-muted">245 KB</span>
                  <button type="button" className="btn-ghost p-1" aria-label="Remove">
                    <X className="h-4 w-4" />
                  </button>
                </li>
              </ul>
            </div>

            <div>
              <label htmlFor="report-style" className="label-text">Writing Style</label>
              <select id="report-style" className="select-field" defaultValue="formal">
                <option value="formal">Formal — Board-ready language</option>
                <option value="professional">Professional — Standard business tone</option>
                <option value="concise">Concise — Bullet-heavy, minimal prose</option>
                <option value="detailed">Detailed — Comprehensive narrative</option>
              </select>
            </div>

            <div className="pt-2">
              <button type="button" className="btn-primary px-6">
                <Sparkles className="h-4 w-4" />
                Generate Report
              </button>
            </div>
          </form>
        </div>
      </main>

      <RightDocumentPanel />
    </div>
  );
}
