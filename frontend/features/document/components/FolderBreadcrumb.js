"use client";

import { useState } from "react";
import Link from "next/link";
import { Check, Copy } from "lucide-react";
import { hrefForBreadcrumb } from "@/utils/folderPath";

// breadcrumb comes from the backend as root -> current folder (inclusive), so the
// last segment is always the folder currently open. Each crumb is a real link to the
// folder URL for that depth, so it navigates (and adds history) like any other link.
export default function FolderBreadcrumb({ breadcrumb }) {
  const [copied, setCopied] = useState(false);

  if (!breadcrumb || breadcrumb.length === 0) return null;

  const path = breadcrumb.map((item) => item.name).join("/");

  const copyPath = async () => {
    try {
      await navigator.clipboard.writeText(path);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard is blocked outside a secure context — nothing useful to show here.
    }
  };

  return (
    <div className="flex items-center gap-2">
      <nav aria-label="Folder path" className="flex min-w-0 flex-wrap items-center">
        {breadcrumb.map((item, index) => (
          <span key={item.id} className="flex items-center">
            <Link
              href={hrefForBreadcrumb(breadcrumb, index + 1)}
              className="max-w-[220px] truncate rounded-md px-1.5 py-1 text-sm text-text-secondary transition-colors hover:bg-bg-elevated hover:text-text-primary"
              title={item.name}
            >
              {item.name}
            </Link>
            <span className="text-sm text-text-muted">/</span>
          </span>
        ))}
      </nav>

      <button
        type="button"
        onClick={copyPath}
        className="btn-ghost p-1.5 shrink-0"
        aria-label="Copy folder path"
        title={copied ? "Copied" : `Copy "${path}"`}
      >
        {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
      </button>
    </div>
  );
}
