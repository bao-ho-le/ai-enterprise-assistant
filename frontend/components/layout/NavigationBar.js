"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Sparkles, Bell, Settings, Menu } from "lucide-react";

const NAV_LINKS = [
  { href: "/", label: "Home" },
  { href: "/file-storage", label: "File Storage" },
  { href: "/write-email", label: "Write Email" },
  { href: "/write-report", label: "Write Report" },
  { href: "/summary", label: "Summary" },
  { href: "/document-qa", label: "Document QA" },
  { href: "/ai-usage", label: "AI Usage" },
];

function isActive(pathname, href) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export default function NavigationBar() {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-border-subtle bg-bg-primary/80 backdrop-blur-xl">
      <nav
        className="grid h-16 w-full grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center px-4"
        aria-label="Main navigation"
      >
        {/* Left: Logo */}
        <div className="flex min-w-0 items-center justify-self-start">
          <Link
            href="/"
            className="flex items-center gap-2.5 transition-opacity hover:opacity-80"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-lg border border-border-subtle bg-bg-elevated">
              <Sparkles className="h-4 w-4 text-accent" />
            </span>
            <span className="hidden sm:inline text-sm font-semibold tracking-tight text-text-primary">
              Enterprise AI Assistant
            </span>
          </Link>
        </div>

        {/* Center: Navigation */}
        <div className="hidden justify-self-center lg:flex">
          <div className="flex items-center gap-1 whitespace-nowrap">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={`btn-ghost px-3 py-2 text-sm ${
                  isActive(pathname, link.href) ? "nav-link-active" : ""
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>

        {/* Right: Actions */}
        <div className="flex min-w-0 items-center justify-self-end gap-3">
          <button
            type="button"
            className="btn-ghost hidden sm:inline-flex"
            aria-label="Notifications"
          >
            <Bell className="h-4 w-4" />
          </button>
          <button
            type="button"
            className="btn-ghost hidden sm:inline-flex"
            aria-label="Settings"
          >
            <Settings className="h-4 w-4" />
          </button>
          <div className="hidden items-center gap-2 border-l border-border-subtle pl-2 sm:flex">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-bg-elevated text-xs font-medium text-text-secondary">
              NT
            </span>
            <span className="text-sm text-text-secondary">Nguyen Tran</span>
          </div>
          <button
            type="button"
            className="btn-ghost lg:hidden"
            aria-label="Open menu"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen((v) => !v)}
          >
            <Menu className="h-5 w-5" />
          </button>
        </div>
      </nav>

      {/* Mobile Navigation */}
      <div
        id="mobile-nav-panel"
        className={`border-t border-border-subtle bg-bg-secondary px-4 py-4 lg:hidden ${
          mobileOpen ? "is-open" : ""
        }`}
      >
        <div className="flex flex-col gap-1">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setMobileOpen(false)}
              className={`btn-ghost justify-start px-3 py-2.5 text-sm ${
                isActive(pathname, link.href) ? "nav-link-active" : ""
              }`}
            >
              {link.label}
            </Link>
          ))}
        </div>
      </div>
    </header>
  );
}
