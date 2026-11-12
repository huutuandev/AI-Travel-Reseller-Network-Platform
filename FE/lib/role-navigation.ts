import {
  Activity, BadgeDollarSign, BarChart3, Bot, Compass,
  FileCheck2, LayoutDashboard, Link2, Package, Palette, Settings2, ShoppingBag,
  Store, Users, Video, Wallet,
} from "lucide-react"
import type { UserRole } from "@/lib/mock-auth"

export const roleNavigation: Record<UserRole, { label: string; icon: typeof LayoutDashboard; href: string }[]> = {
  "platform-admin": [
    { label: "Platform overview", icon: LayoutDashboard, href: "/platform-admin/dashboard" },
    { label: "Tenant management", icon: Users, href: "/platform-admin/tenants" },
    { label: "System configuration", icon: Settings2, href: "/platform-admin/system-config" },
  ],
  tenant: [
    { label: "Overview", icon: LayoutDashboard, href: "/dashboard" },
    { label: "Products", icon: Package, href: "/tenant/products" },
    { label: "Campaigns", icon: Compass, href: "/tenant/campaigns" },
    { label: "Resellers", icon: Users, href: "/tenant/resellers" },
    { label: "Tracking & attribution", icon: Activity, href: "/tenant/tracking-attribution" },
    { label: "Commissions", icon: Wallet, href: "/tenant/commissions" },
    { label: "Conversion review", icon: FileCheck2, href: "/tenant/conversion-review" },
    { label: "AI Content", icon: Bot, href: "/tenant/ai-content" },
    { label: "Video library", icon: Video, href: "/tenant/video-library" },
    { label: "AI content review", icon: FileCheck2, href: "/tenant/ai-content-review" },
    { label: "Brand settings", icon: Palette, href: "/tenant/settings" },
  ],
  reseller: [
    { label: "Overview", icon: LayoutDashboard, href: "/reseller/dashboard" },
    { label: "Campaign marketplace", icon: Store, href: "/reseller/campaign-marketplace" },
    { label: "My campaigns", icon: ShoppingBag, href: "/reseller/my-campaigns" },
    { label: "Tracking links", icon: Link2, href: "/reseller/tracking-links" },
    { label: "Commissions", icon: BadgeDollarSign, href: "/reseller/commissions" },
    { label: "Analytics", icon: BarChart3, href: "/reseller/analytics" },
  ],
}
