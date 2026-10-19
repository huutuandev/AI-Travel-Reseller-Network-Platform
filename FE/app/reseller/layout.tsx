import { RoleWorkspace } from "@/components/role-workspace"

export default function ResellerLayout({ children }: { children: React.ReactNode }) {
  return <RoleWorkspace role="reseller">{children}</RoleWorkspace>
}
