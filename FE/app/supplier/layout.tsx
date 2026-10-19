import { RoleWorkspace } from "@/components/role-workspace"

export default function SupplierLayout({ children }: { children: React.ReactNode }) {
  return <RoleWorkspace role="supplier">{children}</RoleWorkspace>
}
