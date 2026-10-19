import { SupplierSpecialPage } from "@/components/wireframe-special-pages"

export default async function SupplierProductDetailPage({ searchParams }: { searchParams: Promise<{ product?: string }> }) {
  const { product } = await searchParams
  return <SupplierSpecialPage slug="product-detail" product={product ?? "halong"} />
}
