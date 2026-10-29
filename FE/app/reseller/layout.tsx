import { RoleWorkspace } from "@/components/role-workspace"
import { ResellerCampaignMembershipProvider } from "@/lib/reseller-campaign-membership"

export default function ResellerLayout({ children }: { children: React.ReactNode }) {
  return <ResellerCampaignMembershipProvider><RoleWorkspace role="reseller">{children}</RoleWorkspace></ResellerCampaignMembershipProvider>
}
