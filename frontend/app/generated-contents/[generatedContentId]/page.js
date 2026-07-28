import GeneratedContentDetailView from "@/features/generatedContent/components/GeneratedContentDetailView";

export default async function GeneratedContentDetailPage({ params }) {
  const { generatedContentId } = await params;
  return <GeneratedContentDetailView generatedContentId={generatedContentId} />;
}
