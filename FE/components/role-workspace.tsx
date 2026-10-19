"use client"

import { useEffect, useMemo, useSyncExternalStore } from "react"
import { useRouter } from "next/navigation"
import { Bell, Search } from "lucide-react"
import { clearDemoSession, getDemoSession, getDemoSessionSnapshot, ROLE_HOME, subscribeDemoSession, type UserRole } from "@/lib/mock-auth"
import { AppSidebar } from "@/components/app-sidebar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"

const roleLabels: Record<UserRole, string> = {
  "platform-admin": "Platform Admin",
  tenant: "Tenant Dashboard",
  reseller: "Reseller Portal",
  supplier: "Supplier Portal",
}

export function RoleWorkspace({ role, children }: { role: UserRole | "any"; children: React.ReactNode }) {
  const router = useRouter()
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

  if (sessionSnapshot === "__hydrating__" || !session || (role !== "any" && session.role !== role)) return <div className="flex min-h-screen items-center justify-center bg-[#f8faf9] text-sm text-slate-500">Opening your workspace…</div>

  const initials = session.name.split(/\s+/).map((part) => part[0]).join("").slice(0, 2).toUpperCase()
  const activeRole = role === "any" ? session.role : role
  return <SidebarProvider><AppSidebar role={session.role} onLogout={logout}/><SidebarInset className="min-h-screen bg-[#f8faf9]">
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b bg-white/90 px-4 backdrop-blur sm:px-7"><SidebarTrigger/><Separator orientation="vertical" className="h-5"/><span className="text-sm text-slate-500">{roleLabels[activeRole]} <span className="px-1">/</span><strong className="font-medium text-slate-800">Workspace</strong></span><div className="ml-auto flex items-center gap-2"><div className="relative hidden md:block"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"/><Input aria-label="Search this page" placeholder="Search this page" className="h-9 w-56 pl-9"/></div><Button variant="ghost" size="icon" aria-label="Notifications"><Bell className="size-4"/></Button><div className="ml-1 flex size-9 items-center justify-center rounded-full bg-amber-100 text-xs font-semibold text-amber-900" title={`${session.name}, ${session.title}`}>{initials}</div></div></header>
    <div className="mx-auto w-full max-w-[1440px] space-y-6 p-4 sm:p-7">{children}<p className="text-center text-xs text-slate-400">Development preview · Mock account · No backend connected</p></div>
  </SidebarInset></SidebarProvider>
}

