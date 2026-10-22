"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { 
  LayoutDashboard, 
  Users, 
  Settings, 
  Globe, 
  Briefcase, 
  FileText, 
  PieChart, 
  Bell, 
  Search, 
  Menu, 
  X,
  LogOut,
  ShoppingBag,
  Sparkles,
  Link as LinkIcon
} from "lucide-react";

interface SidebarItem {
  name: string;
  href: string;
  icon: React.ElementType;
}

const ROLES_LINKS: Record<string, SidebarItem[]> = {
  "platform-admin": [
    { name: "Overview", href: "/admin", icon: LayoutDashboard },
    { name: "Tenants (Merchants)", href: "/admin/tenants", icon: Globe },
    { name: "SaaS Plans", href: "/admin/plans", icon: Briefcase },
    { name: "System Users", href: "/admin/users", icon: Users },
    { name: "Settings", href: "/admin/settings", icon: Settings },
  ],
  "tenant-admin": [
    { name: "Dashboard", href: "/tenant", icon: LayoutDashboard },
    { name: "Suppliers", href: "/tenant/suppliers", icon: Briefcase },
    { name: "Products & Campaigns", href: "/tenant/campaigns", icon: ShoppingBag },
    { name: "Resellers", href: "/tenant/resellers", icon: Users },
    { name: "Commissions", href: "/tenant/commissions", icon: PieChart },
    { name: "White Label", href: "/tenant/settings", icon: Settings },
  ],
  "supplier": [
    { name: "Dashboard", href: "/supplier", icon: LayoutDashboard },
    { name: "My Products", href: "/supplier/products", icon: ShoppingBag },
    { name: "Orders", href: "/supplier/orders", icon: FileText },
  ],
  "reseller": [
    { name: "Dashboard", href: "/reseller", icon: LayoutDashboard },
    { name: "Discover Campaigns", href: "/reseller/campaigns", icon: Globe },
    { name: "AI Content Factory", href: "/reseller/ai-content", icon: Sparkles },
    { name: "My Links", href: "/reseller/links", icon: LinkIcon },
    { name: "Earnings", href: "/reseller/earnings", icon: PieChart },
  ]
};

export function DashboardLayout({ 
  children, 
  role = "tenant-admin" 
}: { 
  children: React.ReactNode, 
  role?: keyof typeof ROLES_LINKS 
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const pathname = usePathname();
  const links = ROLES_LINKS[role] || ROLES_LINKS["tenant-admin"];

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 z-20 bg-slate-900/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside 
        className={cn(
          "fixed inset-y-0 left-0 z-30 w-64 transform bg-navy-900 text-white transition-transform duration-300 ease-in-out lg:static lg:translate-x-0",
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex h-16 items-center justify-between px-6 border-b border-white/10">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded bg-teal-500 flex items-center justify-center font-bold text-white">AI</div>
            <span className="text-lg font-bold">Travel Reseller</span>
          </Link>
          <button className="lg:hidden text-slate-300" onClick={() => setSidebarOpen(false)}>
            <X size={20} />
          </button>
        </div>

        <div className="px-4 py-6">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4 px-2">
            {role.replace('-', ' ')}
          </p>
          <nav className="space-y-1">
            {links.map((link) => {
              const isActive = pathname === link.href || (pathname.startsWith(link.href) && link.href !== `/${role.split('-')[0]}`);
              const Icon = link.icon;
              
              return (
                <Link
                  key={link.name}
                  href={link.href}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                    isActive 
                      ? "bg-white/10 text-teal-400" 
                      : "text-slate-300 hover:bg-white/5 hover:text-white"
                  )}
                >
                  <Icon size={18} className={cn(isActive ? "text-teal-400" : "text-slate-400")} />
                  {link.name}
                </Link>
              );
            })}
          </nav>
        </div>
        
        <div className="absolute bottom-0 w-full p-4 border-t border-white/10">
          <Link href="/" className="flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium text-slate-300 hover:bg-white/5 hover:text-white">
            <LogOut size={18} className="text-slate-400" />
            Sign Out
          </Link>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Topbar */}
        <header className="flex h-16 items-center justify-between border-b bg-white px-4 sm:px-6 shadow-sm z-10">
          <div className="flex items-center">
            <button 
              className="mr-4 text-slate-500 lg:hidden focus:outline-none"
              onClick={() => setSidebarOpen(true)}
            >
              <Menu size={24} />
            </button>
            
            <div className="relative hidden sm:block">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search..."
                className="h-9 w-64 rounded-md border border-border bg-slate-50 pl-9 pr-4 text-sm focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <button className="relative text-slate-500 hover:text-navy-900 transition-colors">
              <Bell size={20} />
              <span className="absolute top-0 right-0 h-2 w-2 rounded-full bg-red-500"></span>
            </button>
            <div className="h-8 w-8 rounded-full bg-teal-100 flex items-center justify-center text-teal-700 font-bold border border-teal-200 cursor-pointer">
              {role === 'platform-admin' ? 'PA' : role === 'tenant-admin' ? 'TA' : role === 'supplier' ? 'SP' : 'RS'}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <div className="mx-auto max-w-6xl animate-in fade-in slide-in-from-bottom-4 duration-500">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
