import Link from "next/link";
import { Sparkles } from "lucide-react";

const PRODUCT = [
  { href: "/file-storage", label: "File Storage" },
  { href: "/write-email", label: "Write Email" },
  { href: "/write-report", label: "Write Report" },
  { href: "/summary", label: "Summary" },
];

const INTELLIGENCE = [
  { href: "/document-qa", label: "Document QA" },
  { href: "/ai-usage", label: "AI Usage Dashboard" },
  { href: "#", label: "Semantic Search" },
  { href: "#", label: "Model Settings" },
];

const COMPANY = ["Security", "Compliance", "Documentation", "Contact Sales"];

export default function SiteFooter() {
  return (
    <footer className="border-t border-border-subtle bg-bg-secondary">
      <div className="mx-auto max-w-[1440px] px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-1">
            <div className="flex items-center gap-2.5 mb-4">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-bg-elevated border border-border-subtle">
                <Sparkles className="h-4 w-4 text-accent" />
              </span>
              <span className="text-sm font-semibold text-text-primary">
                Enterprise AI Assistant
              </span>
            </div>
            <p className="text-sm text-text-muted leading-relaxed">
              Secure, enterprise-grade AI workflows for document intelligence,
              communication, and analytics.
            </p>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-text-primary mb-4">Product</h3>
            <ul className="space-y-3">
              {PRODUCT.map((l) => (
                <li key={l.label}>
                  <Link
                    href={l.href}
                    className="text-sm text-text-muted hover:text-text-primary transition-colors"
                  >
                    {l.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-text-primary mb-4">
              Intelligence
            </h3>
            <ul className="space-y-3">
              {INTELLIGENCE.map((l) => (
                <li key={l.label}>
                  <Link
                    href={l.href}
                    className="text-sm text-text-muted hover:text-text-primary transition-colors"
                  >
                    {l.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-text-primary mb-4">Company</h3>
            <ul className="space-y-3">
              {COMPANY.map((label) => (
                <li key={label}>
                  <a
                    href="#"
                    className="text-sm text-text-muted hover:text-text-primary transition-colors"
                  >
                    {label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="mt-12 flex flex-col sm:flex-row items-center justify-between gap-4 pt-8 border-t border-border-subtle">
          <p className="text-sm text-text-muted">
            &copy; 2026 Enterprise AI Assistant. All rights reserved.
          </p>
          <div className="flex items-center gap-6">
            <a href="#" className="text-sm text-text-muted hover:text-text-primary transition-colors">
              Privacy Policy
            </a>
            <a href="#" className="text-sm text-text-muted hover:text-text-primary transition-colors">
              Terms of Service
            </a>
            <a href="#" className="text-sm text-text-muted hover:text-text-primary transition-colors">
              SOC 2
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
