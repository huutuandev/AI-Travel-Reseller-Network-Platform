import {
  Activity, BadgeDollarSign, BarChart3, Bell, Bot, Building2, Compass,
  FileCheck2, LayoutDashboard, Link2, Package, Palette, Settings2, ShoppingBag,
  Store, UserRound, Users, Wallet,
} from "lucide-react"
import type { UserRole } from "@/lib/mock-auth"

export const roleNavigation: Record<UserRole, { label: string; icon: typeof LayoutDashboard; href: string }[]> = {
  "platform-admin": [
    { label: "Platform overview", icon: LayoutDashboard, href: "/platform-admin/dashboard" },
    { label: "Tenant management", icon: Users, href: "/platform-admin/tenants" },
    { label: "Tenant details", icon: Building2, href: "/platform-admin/tenant-detail" },
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
    { label: "AI Content Library", icon: Bot, href: "/reseller/ai-content" },
  ],
  supplier: [
    { label: "Overview", icon: LayoutDashboard, href: "/supplier/dashboard" },
    { label: "My products", icon: Package, href: "/supplier/products" },
    { label: "Product detail", icon: Building2, href: "/supplier/product-detail" },
  ],
}

export const accountNavigation = [
  { label: "My profile", icon: UserRound, href: "/account/profile" },
  { label: "Notifications", icon: Bell, href: "/account/notifications" },
]
