import { RoleWorkspace } from "@/components/role-workspace"

export default function PlatformAdminLayout({ children }: { children: React.ReactNode }) {
  return <RoleWorkspace role="platform-admin">{children}</RoleWorkspace>
}
