import { notFound } from "next/navigation"
import { ResellerSpecialPage } from "@/components/wireframe-special-pages"
import { WireframeTablePage } from "@/components/wireframe-table-page"
import { resellerTables } from "@/lib/wireframe-mocks"

const specialPages = new Set(["campaign-marketplace", "campaign-detail", "profile-settings", "ai-content"])

export function generateStaticParams() {
  return [...Object.keys(resellerTables), ...specialPages].map((slug) => ({ slug }))
}

export default async function ResellerPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  if (specialPages.has(slug)) return <ResellerSpecialPage slug={slug} />
  const config = resellerTables[slug]
  if (!config) notFound()
  return <WireframeTablePage config={config} hrefPrefix="/reseller" />
}
