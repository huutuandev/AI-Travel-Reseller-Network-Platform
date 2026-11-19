"use client"

import { useEffect, useMemo, useState, useSyncExternalStore } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Bell, CheckCheck, ChevronDown, UserRound } from "lucide-react"
import { clearDemoSession, getDemoSession, getDemoSessionSnapshot, ROLE_HOME, subscribeDemoSession, type UserRole } from "@/lib/mock-auth"
import { AppSidebar } from "@/components/app-sidebar"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"

const roleLabels: Record<UserRole, string> = {
  "platform-admin": "Platform Admin",
  tenant: "Tenant Dashboard",
  reseller: "Reseller Portal",
}

export function RoleWorkspace({ role, children }: { role: UserRole | "any"; children: React.ReactNode }) {
  const router = useRouter()
  const [notifications, setNotifications] = useState([
    { title: "New booking attributed", text: "A booking matched a partner referral link.", time: "12 minutes ago", unread: true },
    { title: "Campaign application received", text: "A partner requested to join a campaign.", time: "Yesterday", unread: true },
    { title: "Commission approved", text: "A commission is ready for payout.", time: "13 Oct 2026", unread: false },
  ])
  const sessionSnapshot = useSyncExternalStore(subscribeDemoSession, getDemoSessionSnapshot, () => "__hydrating__")
  const session = useMemo(() => sessionSnapshot && !sessionSnapshot.startsWith("__") ? getDemoSession() : null, [sessionSnapshot])

  useEffect(() => {
    if (sessionSnapshot === "__hydrating__") return
    if (!session) {
      router.replace("/login")
      return
    }
    if (role !== "any" && session.role !== role) router.replace(ROLE_HOME[session.role])
  }, [role, router, session, sessionSnapshot])

  function logout() {
    clearDemoSession()
    router.replace("/login")
  }

  if (sessionSnapshot === "__hydrating__" || !session || (role !== "any" && session.role !== role)) return <div className="flex min-h-screen items-center justify-center bg-[#f7f9f5] text-sm text-slate-500">Opening your workspace…</div>

  const initials = session.name.split(/\s+/).map((part) => part[0]).join("").slice(0, 2).toUpperCase()
  const activeRole = role === "any" ? session.role : role
  const unreadCount = notifications.filter((item) => item.unread).length
  return <SidebarProvider><AppSidebar role={session.role} onLogout={logout}/><SidebarInset className="min-h-screen bg-[#f7f9f5]">
    <header className="sticky top-0 z-20 flex h-16 min-w-0 items-center gap-3 border-b border-[#e7ece3] bg-white/90 px-4 backdrop-blur sm:px-7"><SidebarTrigger className="size-10 shrink-0 rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm hover:bg-slate-50"/><Separator orientation="vertical" className="h-5 shrink-0"/><span className="min-w-0 truncate text-sm text-slate-500">{roleLabels[activeRole]} <span className="px-1">/</span><strong className="font-medium text-slate-800">Workspace</strong></span><div className="ml-auto flex shrink-0 items-center gap-2">
      <Popover onOpenChange={(open) => { if (open) setNotifications((current) => current.map((item) => ({ ...item, unread: false }))) }}>
        <PopoverTrigger render={<Button variant="ghost" size="icon" aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ""}`} className="relative size-10 rounded-full"><Bell className="size-5"/>{unreadCount > 0 && <span className="absolute right-1 top-1 grid size-3.5 place-items-center rounded-full bg-rose-600 text-[8px] font-bold leading-none text-white ring-2 ring-white">{unreadCount}</span>}</Button>}/>
        <PopoverContent align="end" className="w-[360px] max-w-[calc(100vw-2rem)] rounded-2xl border border-slate-200 bg-white p-0 shadow-xl">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3"><div><p className="text-sm font-semibold text-slate-900">Notifications</p><p className="text-xs text-slate-500">Recent workspace activity</p></div><Button variant="ghost" size="sm" className="h-8 gap-1.5 px-2 text-xs text-slate-500" onClick={() => setNotifications((current) => current.map((item) => ({ ...item, unread: false })))}><CheckCheck className="size-3.5"/> Mark read</Button></div>
          <div className="divide-y divide-slate-100">{notifications.map((item) => <div key={item.title} className="flex gap-3 px-4 py-3"><span className={`mt-1.5 size-2 shrink-0 rounded-full ${item.unread ? "bg-teal-600" : "bg-slate-200"}`}/><div className="min-w-0 flex-1"><p className="text-sm font-medium text-slate-800">{item.title}</p><p className="mt-0.5 text-xs leading-5 text-slate-500">{item.text}</p><p className="mt-1 text-[11px] text-slate-400">{item.time}</p></div></div>)}</div>
          <div className="border-t border-slate-100 p-2"><Link href="/account/notifications" className="flex h-9 items-center justify-center rounded-lg text-sm font-medium text-teal-800 transition-colors hover:bg-teal-50">View all notifications</Link></div>
        </PopoverContent>
      </Popover>
      <DropdownMenu>
        <DropdownMenuTrigger render={<Button variant="ghost" className="ml-1 h-10 gap-2 rounded-full px-1.5 pr-2 hover:bg-slate-100" aria-label="User menu"><span className="grid size-8 place-items-center rounded-full bg-amber-100 text-xs font-semibold text-amber-900" title={`${session.name}, ${session.title}`}>{initials}</span><ChevronDown className="size-4 text-slate-500"/></Button>}/>
        <DropdownMenuContent align="end" className="w-60 rounded-xl p-1.5">
          <DropdownMenuGroup>
            <DropdownMenuLabel className="px-2 py-2"><span className="block text-sm font-semibold text-slate-900">{session.name}</span><span className="mt-0.5 block truncate text-xs font-normal text-slate-500">{session.email}</span></DropdownMenuLabel>
            <DropdownMenuItem render={<Link href="/account/profile"/>} className="h-9 gap-2 rounded-lg px-2"><UserRound className="size-4"/>My profile</DropdownMenuItem>
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div></header>
    <div className="mx-auto w-full max-w-[1440px] space-y-6 p-4 sm:p-7">{children}<p className="text-center text-xs text-slate-400">Development preview · Mock account · No backend connected</p></div>
  </SidebarInset></SidebarProvider>
}

