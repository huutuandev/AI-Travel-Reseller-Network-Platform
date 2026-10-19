"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Compass, ExternalLink, LogOut } from "lucide-react"
import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarRail } from "@/components/ui/sidebar"
import type { UserRole } from "@/lib/mock-auth"
import { accountNavigation, roleNavigation } from "@/lib/role-navigation"

const roleNames: Record<UserRole, string> = {
  "platform-admin": "Platform Operations",
  tenant: "Tenant Dashboard",
  reseller: "Reseller Portal",
  supplier: "Supplier Portal",
}

export function AppSidebar({ role, onLogout, ...props }: React.ComponentProps<typeof Sidebar> & { role: UserRole; onLogout: () => void }) {
  const pathname = usePathname()
  return <Sidebar collapsible="icon" {...props}>
    <SidebarHeader className="px-4 py-4"><Link href="/" className="flex items-center gap-2.5 px-1 font-semibold tracking-tight"><span className="flex size-8 items-center justify-center rounded-lg bg-teal-800 text-white"><Compass className="size-4"/></span><span>Wayfarer <span className="font-normal text-muted-foreground">Network</span></span></Link></SidebarHeader>
    <SidebarContent><SidebarGroup><SidebarGroupLabel>{roleNames[role]}</SidebarGroupLabel><SidebarMenu>{roleNavigation[role].map(({label,icon:Icon,href})=><SidebarMenuItem key={label}><SidebarMenuButton isActive={pathname===href||pathname.startsWith(`${href}/`)||(role==="reseller"&&href==="/reseller/campaign-marketplace"&&pathname==="/reseller/campaign-detail")} tooltip={label} render={<Link href={href}/> }><Icon/><span>{label}</span></SidebarMenuButton></SidebarMenuItem>)}</SidebarMenu></SidebarGroup><SidebarGroup className="mt-auto"><SidebarGroupLabel>Account</SidebarGroupLabel><SidebarMenu>{accountNavigation.map(({label,icon:Icon,href})=><SidebarMenuItem key={href}><SidebarMenuButton isActive={pathname===href} tooltip={label} render={<Link href={href}/> }><Icon/><span>{label}</span></SidebarMenuButton></SidebarMenuItem>)}</SidebarMenu></SidebarGroup></SidebarContent>
    <SidebarFooter><div className="space-y-1"><Link href="/" className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"><ExternalLink className="size-4"/><span>Public home</span></Link><button type="button" onClick={onLogout} className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"><LogOut className="size-4"/><span>Sign out</span></button><p className="px-3 pt-1 text-[11px] text-muted-foreground">{roleNames[role]} · Demo</p></div></SidebarFooter><SidebarRail/>
  </Sidebar>
}
