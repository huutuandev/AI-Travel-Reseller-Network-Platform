"use client"

import Link from "next/link"
import { useState } from "react"
import { useSearchParams } from "next/navigation"
import { ArrowLeft, Pause, Play } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { platformAdminTenants, platformTenantDetail } from "@/lib/mock-data"

export default function TenantDetailPage() {
  const searchParams = useSearchParams()
  const tenantId = searchParams.get("tenant")
  const tenant = platformAdminTenants.find((item) => item.id === tenantId)
  const [tenantStatuses, setTenantStatuses] = useState<Record<string, string>>({})
  const [feedback, setFeedback] = useState("")
  function toggleStatus() {
    if (!tenant) return
    const nextStatus = active ? "Suspended" : "Active"
    setTenantStatuses((current) => ({ ...current, [tenant.id]: nextStatus }))
    setFeedback(`${tenant.business} marked ${nextStatus.toLowerCase()} in this preview. No server data was changed.`)
  }
  if (!tenant) return <div className="space-y-4"><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link><Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Select a tenant to view its details</CardTitle><CardDescription>Open Tenant management and choose a specific tenant.</CardDescription></CardHeader><CardContent><Link href="/platform-admin/tenants" className="text-sm font-medium text-teal-800 hover:underline">Go to Tenant management</Link></CardContent></Card></div>
  const active = (tenantStatuses[tenant.id] ?? tenant.status) === "Active"

  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link><p className="mt-4 text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Tenant management</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">{tenant.business}</h1><p className="mt-1 text-sm text-slate-500">Tenant profile, branding and platform usage overview.</p></div><div className="flex items-center gap-2"><Badge className={active?"bg-emerald-50 text-emerald-800":tenant.status==="Pending"&&!tenantStatuses[tenant.id]?"bg-amber-50 text-amber-900":"bg-slate-100 text-slate-700"}>{tenantStatuses[tenant.id] ?? tenant.status}</Badge><Button variant="outline" onClick={toggleStatus}>{active?<Pause className="size-4"/>:<Play className="size-4"/>}{active?"Suspend tenant":"Activate tenant"}</Button></div></div>
    {feedback&&<p role="status" className="rounded-lg bg-amber-50 p-3 text-sm text-amber-900">{feedback}</p>}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{platformTenantDetail.metrics.map((metric)=><Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-3xl">{metric.value}</CardTitle></CardHeader><CardContent className="text-xs text-slate-500">{metric.note}</CardContent></Card>)}</div>
    <div className="grid gap-4 xl:grid-cols-2"><Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-col items-start justify-between gap-2 sm:flex-row sm:items-center"><CardTitle>Tenant information</CardTitle><Badge variant="outline">Business account</Badge></CardHeader><CardContent><dl className="divide-y divide-slate-100">{[{label:"Tenant ID",value:tenant.id},{label:"Sample subdomain",value:tenant.domain},{label:"Created",value:tenant.created},{label:"Primary contact",value:tenant.id==="tn_sunworld"?platformTenantDetail.contact:"Sample contact - contact@tenant.example"},{label:"Data scope",value:"Tenant-scoped workspace"}].map((item)=><div key={item.label} className="grid grid-cols-[minmax(0,0.65fr)_minmax(0,1fr)] gap-3 py-3 text-sm sm:grid-cols-[140px_minmax(0,1fr)]"><dt className="text-slate-500">{item.label}</dt><dd className="break-all font-medium">{item.value}</dd></div>)}</dl><p className="mt-4 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900">This sample subdomain illustrates tenant branding only. Domain routing is not implemented.</p></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><div><CardTitle>Brand preview</CardTitle><CardDescription>Illustrative white label preview</CardDescription></div><Badge className="bg-emerald-50 text-emerald-800">Preview</Badge></CardHeader><CardContent><div className="flex items-center gap-3 rounded-xl border border-slate-100 bg-[#f8f5ef] p-4"><span className="flex size-12 items-center justify-center rounded-xl bg-[#b84932] text-xl font-semibold text-white">{tenant.business.charAt(0)}</span><span><strong className="block">{tenant.business}</strong><small className="text-slate-500">Sample tenant storefront</small></span></div><div className="mt-4 divide-y divide-slate-100">{[{label:"Brand palette",value:<span className="flex gap-1.5"><i className="size-4 rounded-full bg-[#b84932]"/><i className="size-4 rounded-full bg-[#164c52]"/><i className="size-4 rounded-full bg-[#f4e8ce]"/></span>},{label:"Storefront status",value:<Badge className="bg-emerald-50 text-emerald-800">Preview ready</Badge>}].map((item)=><div key={item.label} className="flex items-center justify-between py-3 text-sm"><span className="text-slate-600">{item.label}</span>{item.value}</div>)}</div></CardContent></Card></div>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Recent tenant activity</CardTitle><CardDescription>Platform-visible sample events</CardDescription></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[600px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Event</th><th className="pb-3 font-medium">Area</th><th className="pb-3 font-medium">When</th></tr></thead><tbody>{platformTenantDetail.events.map((event)=><tr key={event.event} className="border-b last:border-0"><td className="py-3.5 font-medium">{event.event}</td><td className="py-3.5 text-slate-600">{event.area}</td><td className="py-3.5 text-slate-600">{event.when}</td></tr>)}</tbody></table></div></CardContent></Card>
  </>
}
