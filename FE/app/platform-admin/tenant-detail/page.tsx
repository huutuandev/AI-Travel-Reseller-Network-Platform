"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import { ArrowLeft, Pause, Play, RefreshCw } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { adminApi, formatAdminDate, type AdminPage, type PlatformEvent, type Tenant, toDisplayStatus } from "@/lib/admin/api"

export default function TenantDetailPage() {
  const searchParams = useSearchParams()
  const tenantId = searchParams.get("tenant")
  const [tenant, setTenant] = useState<Tenant | null>(null)
  const [logs, setLogs] = useState<AdminPage<PlatformEvent> | null>(null)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    if (!tenantId) { setLoading(false); return }
    setLoading(true); setError("")
    try {
      const [tenantResult, logResult] = await Promise.all([adminApi.tenant(tenantId), adminApi.tenantLogs(tenantId, 0, 10)])
      setTenant(tenantResult); setLogs(logResult)
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load tenant details.") }
    finally { setLoading(false) }
  }, [tenantId])
  useEffect(() => { void load() }, [load])

  async function toggleStatus() {
    if (!tenant || tenant.status === "PENDING") return
    const nextStatus = tenant.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE"
    const reason = nextStatus === "SUSPENDED" ? window.prompt(`Reason for suspending ${tenant.businessName}:`)?.trim() : undefined
    if (nextStatus === "SUSPENDED" && !reason) return
    setSaving(true); setError("")
    try { setTenant(await adminApi.changeTenantStatus(tenant.id, { status: nextStatus, ...(reason ? { reason } : {}), version: tenant.version })); await load() }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to change tenant status.") }
    finally { setSaving(false) }
  }

  if (!tenantId) return <div className="space-y-4"><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link><Card><CardHeader><CardTitle>Select a tenant to view its details</CardTitle><CardDescription>Open Tenant management and choose a tenant.</CardDescription></CardHeader></Card></div>
  if (loading && !tenant) return <div className="space-y-4"><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link><p className="py-12 text-center text-sm text-slate-500">Loading tenant details…</p></div>
  if (!tenant) return <div className="space-y-4"><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link>{error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error}</div>}<Button variant="outline" onClick={() => void load()}>Retry</Button></div>

  const statusColor = tenant.status === "ACTIVE" ? "bg-emerald-50 text-emerald-800" : tenant.status === "PENDING" ? "bg-amber-50 text-amber-900" : "bg-slate-100 text-slate-700"
  const stats = tenant.stats
  const metricRows = [
    { label: "Active campaigns", value: stats?.activeCampaigns },
    { label: "Approved resellers", value: stats?.totalResellers },
    { label: "Attributed bookings", value: stats?.attributedBookings },
    { label: "Clicks (30 days)", value: stats?.clicksAvailability === "NOT_COLLECTED" || stats?.clicks30Days == null ? null : stats.clicks30Days },
  ]
  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 text-sm text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Tenant management</Link><p className="mt-4 text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Tenant management</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">{tenant.businessName}</h1><p className="mt-1 text-sm text-slate-500">Tenant profile, branding and platform usage from the Admin API.</p></div><div className="flex items-center gap-2"><Badge className={statusColor}>{toDisplayStatus(tenant.status)}</Badge>{tenant.status !== "PENDING" && <Button variant="outline" disabled={saving} onClick={() => void toggleStatus()}>{tenant.status === "ACTIVE" ? <Pause className="size-4"/> : <Play className="size-4"/>}{saving ? "Saving…" : tenant.status === "ACTIVE" ? "Suspend tenant" : "Activate tenant"}</Button>}<Button variant="ghost" size="icon" aria-label="Refresh tenant" onClick={() => void load()} disabled={loading}><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`}/></Button></div></div>
    {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error}</div>}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{metricRows.map((metric) => <Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-3xl">{metric.value == null ? "—" : metric.value.toLocaleString()}</CardTitle></CardHeader><CardContent className="text-xs text-slate-500">{metric.label === "Clicks (30 days)" && metric.value == null ? "Not collected by this API" : "Current tenant totals"}</CardContent></Card>)}</div>
    <div className="grid gap-4 xl:grid-cols-2"><Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-col items-start justify-between gap-2 sm:flex-row sm:items-center"><CardTitle>Tenant information</CardTitle><Badge variant="outline">Business account</Badge></CardHeader><CardContent><dl className="divide-y divide-slate-100">{[{label:"Tenant ID",value:tenant.id},{label:"Tenant code",value:tenant.code},{label:"Subdomain",value:tenant.subdomain},{label:"Custom domain",value:tenant.customDomain || "Not configured"},{label:"Created",value:formatAdminDate(tenant.createdAt)},{label:"Business description",value:tenant.description || "—"},{label:"Primary contact",value:tenant.contact?.name || "—"},{label:"Owner email",value:tenant.contact?.ownerEmail || "—"},{label:"Contact email",value:tenant.contact?.email || "—"},{label:"Phone",value:tenant.contact?.phone || "—"},{label:"Address",value:tenant.contact?.address || "—"}].map((item) => <div key={item.label} className="grid grid-cols-[minmax(0,0.65fr)_minmax(0,1fr)] gap-3 py-3 text-sm sm:grid-cols-[150px_minmax(0,1fr)]"><dt className="text-slate-500">{item.label}</dt><dd className="break-all font-medium">{item.value}</dd></div>)}</dl></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><div><CardTitle>Branding</CardTitle><CardDescription>Saved merchant branding values</CardDescription></div><Badge className={tenant.branding ? "bg-emerald-50 text-emerald-800" : "bg-slate-100 text-slate-700"}>{tenant.branding ? "Configured" : "Not configured"}</Badge></CardHeader><CardContent>{tenant.branding ? <><div className="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-4">{tenant.branding.logoUrl ? <img src={tenant.branding.logoUrl} alt="Tenant logo" className="size-12 rounded-xl object-cover"/> : <span className="grid size-12 place-items-center rounded-xl bg-teal-800 text-xl font-semibold text-white">{tenant.businessName.charAt(0)}</span>}<div className="min-w-0"><strong className="block truncate">{tenant.businessName}</strong><small className="text-slate-500">Tenant storefront branding</small></div></div><div className="mt-4 divide-y divide-slate-100">{[{label:"Logo URL",value:tenant.branding.logoUrl || "—",color:""},{label:"Primary color",value:tenant.branding.primaryColor || "—",color:tenant.branding.primaryColor},{label:"Secondary color",value:tenant.branding.secondaryColor || "—",color:tenant.branding.secondaryColor}].map((item) => <div key={item.label} className="flex items-center justify-between gap-3 py-3 text-sm"><span className="text-slate-600">{item.label}</span><span className="flex items-center gap-2 font-medium">{item.color && <i className="size-4 rounded-full border" style={{backgroundColor:item.color}}/>}{item.value}</span></div>)}</div></> : <p className="rounded-lg bg-slate-50 p-4 text-sm text-slate-500">No branding record is available for this tenant.</p>}</CardContent></Card></div>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Recent tenant activity</CardTitle><CardDescription>Events returned by the tenant log endpoint</CardDescription></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[700px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Event</th><th className="pb-3 font-medium">Area</th><th className="pb-3 font-medium">Result</th><th className="pb-3 font-medium">Status change</th><th className="pb-3 font-medium">When</th></tr></thead><tbody>{(logs?.content ?? []).map((event) => <tr key={event.id} className="border-b last:border-0"><td className="py-3.5 font-medium">{event.eventType.replaceAll("_", " ")}</td><td className="py-3.5 text-slate-600">{event.area}</td><td className="py-3.5"><Badge variant="outline">{event.result}</Badge></td><td className="py-3.5 text-slate-600">{event.previousStatus || "—"}{event.newStatus ? ` → ${event.newStatus}` : ""}{event.reason ? <span className="block text-xs">{event.reason}</span> : null}</td><td className="py-3.5 text-slate-600">{formatAdminDate(event.createdAt)}</td></tr>)}</tbody></table>{logs?.content.length === 0 && <p className="py-8 text-center text-sm text-slate-500">No tenant events recorded.</p>}</div></CardContent></Card>
  </>
}
