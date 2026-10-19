import { RoleWorkspace } from "@/components/role-workspace"

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return <RoleWorkspace role="any">{children}</RoleWorkspace>
}
