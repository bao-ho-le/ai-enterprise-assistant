/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",

  // The four AI features were consolidated into /chat (+ /chat/[conversationId]).
  // Old URLs stay valid: list routes land on the composer with the right mode, detail
  // routes open the same conversation in the unified workspace. `permanent: false` so
  // these can still be adjusted without poisoning browser caches.
  async redirects() {
    return [
      { source: "/write-email", destination: "/chat?mode=email", permanent: false },
      { source: "/write-email/:conversationId", destination: "/chat/:conversationId", permanent: false },
      { source: "/write-report", destination: "/chat?mode=report", permanent: false },
      { source: "/write-report/:conversationId", destination: "/chat/:conversationId", permanent: false },
      { source: "/summary", destination: "/chat?mode=summary", permanent: false },
      { source: "/summary/:conversationId", destination: "/chat/:conversationId", permanent: false },
      { source: "/document-qa", destination: "/chat?mode=qa", permanent: false },
      { source: "/document-qa/:conversationId", destination: "/chat/:conversationId", permanent: false },
    ];
  },
};

export default nextConfig;
