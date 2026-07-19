import DocumentDetailView from "@/features/document/components/DocumentDetailView";

export const metadata = {
  title: "Document Details — Enterprise AI Assistant",
};

export default async function DocumentDetailPage({ params }) {
  const { id } = await params;
  return <DocumentDetailView documentId={id} />;
}
