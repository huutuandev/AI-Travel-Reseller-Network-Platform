import { RoleWorkspace } from "@/components/role-workspace"

export default function TenantLayout({ children }: { children: React.ReactNode }) {
  return <RoleWorkspace role="tenant">{children}</RoleWorkspace>
}
