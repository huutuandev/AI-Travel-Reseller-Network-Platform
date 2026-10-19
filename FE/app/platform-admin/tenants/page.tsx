"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { platformAdminTenants } from "@/lib/mock-data"

type TenantRow = (typeof platformAdminTenants)[number]
const statusStyles: Record<string, string> = { Active: "bg-emerald-50 text-emerald-800", Pending: "bg-amber-50 text-amber-900", Suspended: "bg-slate-100 text-slate-700" }

export default function TenantManagementPage() {
  const [tenants, setTenants] = useState<TenantRow[]>(platformAdminTenants)
  const [query, setQuery] = useState("")
  const [status, setStatus] = useState("All statuses")
  const [feedback, setFeedback] = useState("")
  const filteredTenants = useMemo(() => tenants.filter((tenant) => {
    const searchMatch = `${tenant.business} ${tenant.id} ${tenant.domain}`.toLowerCase().includes(query.toLowerCase())
    return searchMatch && (status === "All statuses" || tenant.status === status)
  }), [tenants, query, status])

  function toggleTenant(tenantId: string) {
    const tenant = tenants.find((item) => item.id === tenantId)
    if (!tenant) return
    const nextStatus = tenant.status === "Active" ? "Suspended" : "Active"
    setTenants((current) => current.map((item) => item.id === tenantId ? { ...item, status: nextStatus } : item))
    setFeedback(`${tenant.business} marked ${nextStatus.toLowerCase()} for this preview. No server data was changed.`)
  }

  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Platform operations</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">Tenant management</h1><p className="mt-1 text-sm text-slate-500">Review businesses using the shared platform and manage their demo status.</p></div><Badge variant="outline" className="bg-white px-3 py-1.5">248 sample tenants</Badge></div>
    <Card className="border-slate-200/80 shadow-sm"><CardContent className="space-y-4"><div className="flex flex-col gap-3 sm:flex-row"><Input type="search" aria-label="Search tenants" placeholder="Search business, tenant ID or subdomain" value={query} onChange={(event)=>setQuery(event.target.value)} className="sm:max-w-md"/><select aria-label="Filter by tenant status" value={status} onChange={(event)=>setStatus(event.target.value)} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option>All statuses</option><option>Active</option><option>Pending</option><option>Suspended</option></select></div><div className="overflow-x-auto"><table className="w-full min-w-[800px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Business</th><th className="pb-3 font-medium">Tenant ID / subdomain</th><th className="pb-3 font-medium">Status</th><th className="pb-3 font-medium">Created</th><th className="pb-3 font-medium">Actions</th></tr></thead><tbody>{filteredTenants.map((tenant)=><tr key={tenant.id} className="border-b last:border-0"><td className="py-3.5"><strong className="block font-medium">{tenant.business}</strong><span className="text-xs text-slate-500">{tenant.description}</span></td><td className="py-3.5"><span className="block font-mono text-xs">{tenant.id}</span><span className="text-xs text-slate-500">{tenant.domain}</span></td><td className="py-3.5"><Badge className={statusStyles[tenant.status]}>{tenant.status}</Badge></td><td className="py-3.5 text-slate-600">{tenant.created}</td><td className="py-3.5"><div className="flex items-center gap-2"><Link href="/platform-admin/tenant-detail" className="text-sm font-medium text-teal-800 hover:underline">{tenant.status==="Pending"?"Review":"View details"}</Link><Button size="sm" variant="outline" onClick={()=>toggleTenant(tenant.id)}>{tenant.status==="Active"?"Suspend":"Activate"}</Button></div></td></tr>)}</tbody></table>{filteredTenants.length===0&&<p className="py-8 text-center text-sm text-slate-500">No tenants match these filters.</p>}</div>{feedback&&<p role="status" className="text-sm text-emerald-800">{feedback}</p>}</CardContent></Card>
    <p className="rounded-lg border border-slate-200 bg-white p-3 text-xs leading-5 text-slate-500">Subdomains shown here are sample tenant identifiers for the prototype. No DNS or custom-domain resolution is implemented.</p>
  </>
}
