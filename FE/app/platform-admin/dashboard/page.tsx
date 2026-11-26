"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { ArrowUpRight, Building2, CircleAlert, RefreshCw } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { adminApi, formatAdminDate, type PlatformEvent, type PlatformOverview } from "@/lib/admin/api"

export default function PlatformAdminDashboard() {
  const [overview, setOverview] = useState<PlatformOverview | null>(null)
  const [activities, setActivities] = useState<PlatformEvent[]>([])
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true); setError("")
    try {
      const [nextOverview, nextActivities] = await Promise.all([adminApi.overview(30), adminApi.recentActivities(8)])
      setOverview(nextOverview); setActivities(nextActivities.content ?? [])
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load platform overview.") }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])

  const metrics = overview?.metrics
  const usage = overview?.usage
  const cards = [
    { label: "Total tenants", value: metrics?.totalTenants, note: `${metrics?.newTenantsThisMonth ?? 0} added this month` },
    { label: "Active tenants", value: metrics?.activeTenants, note: `${metrics?.activePercentage ?? 0}% of all tenants` },
    { label: "Pending review", value: metrics?.pendingTenants, note: "Awaiting tenant activation" },
    { label: "Suspended", value: metrics?.suspendedTenants, note: "Currently not active" },
  ]
  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Platform operations</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">Platform overview</h1><p className="mt-1 text-sm text-slate-500">Live tenant activity and usage from the platform service.</p></div><div className="flex gap-2"><Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`}/>Refresh</Button><Link href="/platform-admin/tenants" className="inline-flex items-center gap-2 rounded-md bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900">Manage tenants <ArrowUpRight className="size-4"/></Link></div></div>
    {error && <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800"><span>{error}</span><Button size="sm" variant="outline" onClick={() => void load()}>Retry</Button></div>}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map((metric) => <Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-3xl">{loading ? "—" : (metric.value ?? 0).toLocaleString()}</CardTitle></CardHeader><CardContent className="text-xs text-slate-500">{metric.note}</CardContent></Card>)}</div>
    <div className="grid gap-4 xl:grid-cols-[0.8fr_1.2fr]">
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-center justify-between"><div><CardTitle>Network usage</CardTitle><CardDescription className="mt-1">Platform data · Last 30 days</CardDescription></div><Building2 className="size-5 text-teal-800"/></div></CardHeader><CardContent className="space-y-4"><div className="flex items-center justify-between rounded-lg border border-slate-100 p-3"><span className="text-sm text-slate-600">Running campaigns</span><strong>{loading ? "—" : (usage?.runningCampaigns ?? 0).toLocaleString()}</strong></div><div className="flex items-center justify-between rounded-lg border border-slate-100 p-3"><span className="text-sm text-slate-600">Attributed bookings</span><strong>{loading ? "—" : (usage?.attributedBookings ?? 0).toLocaleString()}</strong></div><div className="flex items-start gap-2 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900"><CircleAlert className="mt-0.5 size-4 shrink-0"/>Click tracking is {usage?.clicksAvailability === "NOT_COLLECTED" || usage?.trackedClicks == null ? "not collected by the current API" : `${usage.trackedClicks.toLocaleString()} clicks recorded`}.</div></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Tenant status</CardTitle><CardDescription>Current distribution from the Admin API</CardDescription></CardHeader><CardContent className="space-y-3">{[{label:"Active tenants",value:metrics?.activeTenants,kind:"bg-emerald-50 text-emerald-800"},{label:"Pending review",value:metrics?.pendingTenants,kind:"bg-amber-50 text-amber-900"},{label:"Suspended",value:metrics?.suspendedTenants,kind:"bg-slate-100 text-slate-700"}].map((row)=><div key={row.label} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-3"><span className="text-sm">{row.label}</span><Badge className={row.kind}>{loading ? "—" : (row.value ?? 0).toLocaleString()}</Badge></div>)}<Link href="/platform-admin/tenants" className="inline-flex items-center gap-1 pt-1 text-sm font-medium text-teal-800 hover:underline">Open tenant directory <ArrowUpRight className="size-4"/></Link></CardContent></Card>
    </div>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><div><CardTitle>Recent tenant activity</CardTitle><CardDescription>Latest events returned by the platform service</CardDescription></div><Link href="/platform-admin/system-config" className="text-sm font-medium text-teal-800">Platform logs</Link></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[680px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Tenant</th><th className="pb-3 font-medium">Activity</th><th className="pb-3 font-medium">Status</th><th className="pb-3 font-medium">Date</th><th className="pb-3 text-right font-medium">Action</th></tr></thead><tbody>{activities.map((row) => <tr key={row.id} className="border-b last:border-0"><td className="py-3.5"><strong className="block font-medium">{row.businessName || row.tenantCode || "Platform event"}</strong><span className="text-xs text-slate-500">{row.subdomain || row.tenantId || "Platform"}</span></td><td className="py-3.5 text-slate-600">{row.eventType.replaceAll("_", " ")}</td><td className="py-3.5"><Badge className={row.newStatus === "ACTIVE" ? "bg-emerald-50 text-emerald-800" : row.newStatus === "SUSPENDED" ? "bg-slate-100 text-slate-700" : "bg-amber-50 text-amber-900"}>{row.newStatus || row.result}</Badge></td><td className="py-3.5 text-slate-600">{formatAdminDate(row.createdAt)}</td><td className="py-3.5 text-right">{row.tenantId ? <Link href={`/platform-admin/tenant-detail?tenant=${encodeURIComponent(row.tenantId)}`} className="inline-flex items-center gap-1 font-medium text-teal-800">View<ArrowUpRight className="size-3.5"/></Link> : <span className="text-slate-400">—</span>}</td></tr>)}</tbody></table>{!loading && activities.length === 0 && <p className="py-8 text-center text-sm text-slate-500">No recent activity.</p>}</div></CardContent></Card>
  </>
}
