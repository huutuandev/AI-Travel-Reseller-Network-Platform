"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Compass, ExternalLink, LogOut } from "lucide-react"
import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarRail } from "@/components/ui/sidebar"
import type { UserRole } from "@/lib/mock-auth"
import { roleNavigation } from "@/lib/role-navigation"

const roleNames: Record<UserRole, string> = {
  "platform-admin": "Platform Admin",
  tenant: "Tenant Dashboard",
  reseller: "Reseller Dashboard",
}

export function AppSidebar({ role, onLogout, ...props }: React.ComponentProps<typeof Sidebar> & { role: UserRole; onLogout: () => void }) {
  const pathname = usePathname()

  return (
    <Sidebar collapsible="icon" className="workspace-sidebar" {...props}>
      <SidebarHeader className="px-3 pb-3 pt-5">
        <Link href="/" className="workspace-brand group flex items-center gap-3 rounded-2xl px-2 py-2.5 transition-colors hover:bg-white/80">
          <span className="workspace-brand-mark relative grid size-11 shrink-0 place-items-center rounded-2xl text-white">
            <Compass className="size-[22px]" strokeWidth={1.7} />
            <i className="absolute right-[7px] top-[7px] size-1.5 rounded-full bg-amber-300 ring-2 ring-[#294e3b]" />
          </span>
          <span className="min-w-0 leading-tight group-data-[collapsible=icon]:hidden">
            <span className="block whitespace-nowrap text-[15px] font-bold tracking-[-.02em] text-slate-800">Wayfarer <span className="font-medium text-emerald-800">Network</span></span>
            <span className="mt-1 block whitespace-nowrap text-[9px] font-semibold uppercase tracking-[.16em] text-slate-400">Travel partner platform</span>
          </span>
        </Link>
        <p className="px-3 pt-2 text-xs font-semibold uppercase tracking-[.12em] text-slate-500 group-data-[collapsible=icon]:hidden">{roleNames[role]}</p>
      </SidebarHeader>

      <SidebarContent className="px-2">
        <SidebarGroup className="pt-2">
          <SidebarGroupLabel className="px-3 text-[10px] font-bold uppercase tracking-[.15em] text-slate-400">Workspace menu</SidebarGroupLabel>
          <SidebarMenu className="gap-1">
            {roleNavigation[role].map(({ label, icon: Icon, href }) => {
              const isActive = pathname === href || pathname.startsWith(`${href}/`) || (role === "reseller" && href === "/reseller/campaign-marketplace" && pathname === "/reseller/campaign-detail")
              return (
                <SidebarMenuItem key={label}>
                  <SidebarMenuButton
                    isActive={isActive}
                    tooltip={label}
                    render={<Link href={href} aria-current={isActive ? "page" : undefined} />}
                    className={`workspace-nav-link group/nav-link h-11 rounded-xl px-2.5 text-[13px] font-medium text-slate-600 transition-all duration-200 hover:translate-x-0.5 hover:bg-white hover:text-emerald-900 hover:shadow-[0_5px_16px_rgba(44,83,57,0.07)] [&>svg]:hidden ${isActive ? "workspace-nav-link-active" : ""}`}
                  >
                    <span className={`workspace-nav-icon grid size-8 shrink-0 place-items-center rounded-[10px] bg-white/70 text-slate-500 ring-1 ring-slate-200/80 transition-colors group-data-[collapsible=icon]:size-4 group-data-[collapsible=icon]:bg-transparent group-data-[collapsible=icon]:ring-0 ${isActive ? "workspace-nav-icon-active" : ""}`}>
                      <Icon className="size-[17px]" strokeWidth={1.9} />
                    </span>
                    <span>{label}</span>
                    {isActive && <span aria-hidden="true" className="ml-auto mr-1 size-2.5 shrink-0 rounded-full bg-emerald-300 ring-[3px] ring-emerald-100/70 group-data-[collapsible=icon]:hidden" />}
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )
            })}
          </SidebarMenu>
        </SidebarGroup>

      </SidebarContent>

      <SidebarFooter className="px-3 pb-4 pt-2">
        <div className="workspace-sidebar-footer space-y-1 border-t border-slate-200/80 pt-3">
          <Link href="/" className="flex h-10 items-center gap-2.5 rounded-xl px-3 text-[12px] font-medium text-slate-500 transition-colors hover:bg-white hover:text-emerald-900">
            <ExternalLink className="size-4" strokeWidth={1.9} /><span className="group-data-[collapsible=icon]:hidden">Public home</span>
          </Link>
          <button type="button" onClick={onLogout} className="flex h-10 w-full items-center gap-2.5 rounded-xl px-3 text-left text-[12px] font-medium text-slate-500 transition-colors hover:bg-rose-50 hover:text-rose-700">
            <LogOut className="size-4" strokeWidth={1.9} /><span className="group-data-[collapsible=icon]:hidden">Sign out</span>
          </button>
          <p className="px-3 pt-2 text-[10px] font-medium text-slate-400 group-data-[collapsible=icon]:hidden">{roleNames[role]} · Demo workspace</p>
        </div>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
