import { notFound } from "next/navigation"
import { TenantSpecialPage } from "@/components/wireframe-special-pages"
import { WireframeTablePage } from "@/components/wireframe-table-page"
import { tenantTables } from "@/lib/wireframe-mocks"

const specialPages = new Set(["products", "campaign-create", "campaign-detail", "ai-content", "settings"])

export function generateStaticParams() {
  return [...new Set([...Object.keys(tenantTables), ...specialPages])].map((slug) => ({ slug }))
}

export default async function TenantPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  if (specialPages.has(slug)) return <TenantSpecialPage slug={slug} />
  const config = tenantTables[slug]
  if (!config) notFound()
  return <WireframeTablePage config={config} hrefPrefix="/tenant" />
}
