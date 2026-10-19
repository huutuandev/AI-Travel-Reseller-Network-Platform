import { WireframeTablePage } from "@/components/wireframe-table-page"
import { supplierProducts } from "@/lib/wireframe-mocks"

export default function SupplierProductsPage() {
  return <WireframeTablePage config={supplierProducts} hrefPrefix="/supplier" />
}
