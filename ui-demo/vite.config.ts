import { defineConfig, type Plugin } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function resolveIncludes(html: string, baseDir: string): string {
  return html.replace(/<!--\s*@include\s+([^\s]+)\s*-->/g, (_, relPath) => {
    const filePath = path.resolve(baseDir, relPath)
    const content = fs.readFileSync(filePath, 'utf-8')
    return resolveIncludes(content, path.dirname(filePath))
  })
}

function htmlIncludes(): Plugin {
  return {
    name: 'html-includes',
    transformIndexHtml: {
      order: 'pre',
      handler(html, ctx) {
        const filename = ctx.filename
        if (!filename.endsWith('.html')) return html
        return resolveIncludes(html, path.dirname(filename))
      },
    },
  }
}

const pagesDir = path.resolve(__dirname, 'src/pages')

export default defineConfig({
  plugins: [tailwindcss(), htmlIncludes()],
  build: {
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, 'index.html'),
        landing: path.resolve(pagesDir, 'landing/LandingPage.html'),
        fileStorage: path.resolve(pagesDir, 'filestorage/FileStoragePage.html'),
        writeEmail: path.resolve(pagesDir, 'writeemail/WriteEmailPage.html'),
        writeReport: path.resolve(pagesDir, 'writereport/WriteReportPage.html'),
        summary: path.resolve(pagesDir, 'summary/SummaryPage.html'),
        documentQA: path.resolve(pagesDir, 'documentqa/DocumentQAPage.html'),
        aiUsage: path.resolve(pagesDir, 'aiusagedashboard/AIUsageDashboardPage.html'),
      },
    },
  },
})
