"use client";

import { useState } from "react";
import { Sparkles, ArrowLeft, Copy, Save, Download } from "lucide-react";
import LeftSidebar from "@/components/layout/LeftSidebar";

export default function WriteEmailPage() {
  const [showPreview, setShowPreview] = useState(false);
  const [saveLabel, setSaveLabel] = useState("Save");

  const onSave = () => {
    setSaveLabel("Saved!");
    setTimeout(() => setSaveLabel("Save"), 2000);
  };

  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftSidebar />

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
          {!showPreview ? (
            <form className="space-y-6" aria-label="Email composition form">
              <div>
                <label htmlFor="email-purpose" className="label-text">Purpose</label>
                <textarea
                  id="email-purpose"
                  rows={7}
                  className="textarea-field resize-none"
                  placeholder="Describe the purpose of this email — e.g. Follow up on Q4 partnership proposal with Acme Corp"
                  defaultValue="Follow up on our Q4 partnership proposal with Acme Corp and confirm next steps for the integration timeline."
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="email-sender" className="label-text">Sender</label>
                  <input type="email" id="email-sender" className="input-field" defaultValue="nguyen.tran@company.com" />
                </div>
                <div>
                  <label htmlFor="email-recipient" className="label-text">Recipient</label>
                  <input type="email" id="email-recipient" className="input-field" defaultValue="sarah.chen@acmecorp.com" />
                </div>
              </div>

              <div>
                <label htmlFor="email-context" className="label-text">Optional Context</label>
                <textarea
                  id="email-context"
                  rows={7}
                  className="textarea-field resize-none"
                  placeholder="Additional context for the AI to consider"
                  defaultValue="Previous meeting on June 28 discussed API integration scope. Sarah requested a written summary of deliverables and timeline by end of week."
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                  <label htmlFor="email-language" className="label-text">Language</label>
                  <select id="email-language" className="select-field" defaultValue="en">
                    <option value="en">English</option>
                    <option value="vi">Vietnamese</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="email-length" className="label-text">Email Length</label>
                  <select id="email-length" className="select-field" defaultValue="medium">
                    <option value="short">Short</option>
                    <option value="medium">Medium</option>
                    <option value="long">Long</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="email-audience" className="label-text">Audience</label>
                  <select id="email-audience" className="select-field" defaultValue="customer">
                    <option value="customer">Customer</option>
                    <option value="employee">Employee</option>
                    <option value="manager">Manager</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="email-tone" className="label-text">Tone</label>
                  <select id="email-tone" className="select-field" defaultValue="professional">
                    <option value="formal">Formal</option>
                    <option value="professional">Professional</option>
                    <option value="friendly">Friendly</option>
                    <option value="neutral">Neutral</option>
                  </select>
                </div>
              </div>

              <div className="pt-2">
                <button type="button" className="btn-primary px-6" onClick={() => setShowPreview(true)}>
                  <Sparkles className="h-4 w-4" />
                  Generate Email
                </button>
              </div>
            </form>
          ) : (
            <section aria-label="Email preview">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-text-primary">Email Preview</h2>
                <button type="button" className="btn-secondary text-sm" onClick={() => setShowPreview(false)}>
                  <ArrowLeft className="h-4 w-4" />
                  Back to Form
                </button>
              </div>

              <article className="card overflow-hidden">
                <div className="px-6 py-4 border-b border-border-subtle bg-bg-elevated/50 space-y-3">
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-text-muted w-16 shrink-0">From:</span>
                    <span className="text-text-primary">nguyen.tran@company.com</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-text-muted w-16 shrink-0">To:</span>
                    <span className="text-text-primary">sarah.chen@acmecorp.com</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-text-muted w-16 shrink-0">Subject:</span>
                    <input
                      type="text"
                      className="input-field flex-1 py-1.5 text-sm font-medium"
                      defaultValue="Re: Q4 Partnership Proposal — Next Steps & Integration Timeline"
                    />
                  </div>
                </div>

                <div className="px-6 py-6">
                  <div
                    contentEditable
                    suppressContentEditableWarning
                    className="prose prose-invert max-w-none text-sm text-text-primary leading-relaxed focus:outline-none min-h-[200px]"
                    role="textbox"
                    aria-label="Email body"
                  >
                    <p>Dear Sarah,</p>
                    <p className="mt-4">
                      Thank you for the productive discussion during our meeting on June 28. I appreciate
                      your team&apos;s thorough review of our Q4 partnership proposal.
                    </p>
                    <p className="mt-4">
                      As requested, I&apos;m providing a written summary of the key deliverables and proposed
                      integration timeline:
                    </p>
                    <p className="mt-4"><strong>Phase 1 (August 2026):</strong> API authentication setup and sandbox environment configuration.</p>
                    <p className="mt-2"><strong>Phase 2 (September 2026):</strong> Core data sync implementation and initial testing.</p>
                    <p className="mt-2"><strong>Phase 3 (October 2026):</strong> Production deployment and go-live support.</p>
                    <p className="mt-4">
                      Please let me know if this timeline aligns with your team&apos;s availability. I&apos;m happy
                      to schedule a follow-up call to discuss any adjustments.
                    </p>
                    <p className="mt-4">Best regards,<br />Nguyen Tran<br />Senior Account Manager</p>
                  </div>
                </div>

                <div className="px-6 py-4 border-t border-border-subtle bg-bg-secondary flex items-center gap-3">
                  <button type="button" className="btn-primary text-sm">
                    <Copy className="h-4 w-4" />
                    Copy to Clipboard
                  </button>
                  <button type="button" className="btn-secondary text-sm" onClick={onSave}>
                    <Save className="h-4 w-4" />
                    <span>{saveLabel}</span>
                  </button>
                  <button type="button" className="btn-secondary text-sm">
                    <Download className="h-4 w-4" />
                    Export as .eml
                  </button>
                </div>
              </article>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}
