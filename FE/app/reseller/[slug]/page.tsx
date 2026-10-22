import { notFound, redirect } from "next/navigation"
import { ResellerSpecialPage } from "@/components/wireframe-special-pages"
import { WireframeTablePage } from "@/components/wireframe-table-page"
import { resellerTables } from "@/lib/wireframe-mocks"

const specialPages = new Set(["campaign-marketplace", "campaign-detail", "campaign-content", "campaign-videos", "profile-settings", "ai-content"])

export function generateStaticParams() {
  return [...Object.keys(resellerTables), ...specialPages].map((slug) => ({ slug }))
}

export default async function ResellerPage({ params, searchParams }: { params: Promise<{ slug: string }>; searchParams: Promise<{ campaign?: string }> }) {
  const { slug } = await params
  const { campaign } = await searchParams
  if (slug === "ai-content") redirect("/reseller/my-campaigns")
  if (specialPages.has(slug)) return <ResellerSpecialPage slug={slug} campaignName={campaign} />
  const config = resellerTables[slug]
  if (!config) notFound()
  return <WireframeTablePage config={config} hrefPrefix="/reseller" />
}
