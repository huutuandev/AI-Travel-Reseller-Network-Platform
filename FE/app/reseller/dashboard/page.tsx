"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { ArrowUpRight, Check, Copy, Eye, Link2, Music2, Sparkles } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { resellerDashboard } from "@/lib/mock-data"
import { useBackendResellerSession } from "@/lib/use-backend-reseller-session"
import { displayStatus, resellerApi, type BackendApplication, type BackendCommission, type BackendTrackingLink } from "@/lib/reseller-api"

function FacebookBrandIcon({ className }: { className?: string; "aria-hidden"?: boolean }) {
  return <svg aria-hidden="true" viewBox="0 0 24 24" className={className}><rect width="24" height="24" rx="5" fill="#1877F2"/><path fill="white" d="M13.5 21v-7h2.35l.4-2.8H13.5V9.38c0-.81.27-1.38 1.41-1.38h1.5V5.5c-.26-.04-1.16-.12-2.2-.12-2.18 0-3.67 1.33-3.67 3.78v2.04H8.2V14h2.34v7h2.96Z"/></svg>
}

export default function ResellerDashboard() {
  const backendMode = useBackendResellerSession()
  const [accountDataLoaded, setAccountDataLoaded] = useState(false)
  const loadingAccountData = backendMode && !accountDataLoaded
  const [accountDataError, setAccountDataError] = useState("")
  const [liveApplications, setLiveApplications] = useState<BackendApplication[]>([])
  const [liveTrackingLinks, setLiveTrackingLinks] = useState<BackendTrackingLink[]>([])
  const [liveCommissions, setLiveCommissions] = useState<BackendCommission[]>([])
  useEffect(() => {
    if (!backendMode) return
    let cancelled = false
    Promise.all([
      resellerApi<BackendApplication[]>("campaigns/applications"),
      resellerApi<BackendTrackingLink[]>("tracking-links"),
      resellerApi<BackendCommission[]>("commissions"),
    ]).then(([applications, links, commissions]) => {
      if (cancelled) return
      setLiveApplications(applications)
      setLiveTrackingLinks(links)
      setLiveCommissions(commissions)
    }).catch((error) => {
      if (!cancelled) setAccountDataError(error instanceof Error ? error.message : "Could not load reseller account data.")
    }).finally(() => { if (!cancelled) setAccountDataLoaded(true) })
    return () => { cancelled = true }
  }, [backendMode])
  const clicks = liveTrackingLinks.reduce((total, link) => total + link.clicks, 0)
  const conversions = liveTrackingLinks.reduce((total, link) => total + link.conversions, 0)
  const commissionByStatus = (status: string) => liveCommissions.filter((item) => item.status.toUpperCase() === status).reduce((total, item) => total + Number(item.commissionAmount || 0), 0)
  const currency = liveCommissions[0]?.currency || "USD"
  const money = (amount: number) => new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount)
  const commissionSummary = backendMode ? [
    { label: "Commission pending", value: money(commissionByStatus("PENDING")), note: "Awaiting confirmation" },
    { label: "Commission approved", value: money(commissionByStatus("APPROVED")), note: "Approved by the campaign owner" },
    { label: "Commission paid", value: money(commissionByStatus("PAID")), note: "Marked paid by the campaign owner" },
  ] : resellerDashboard.commissionSummary
  const metrics = backendMode ? [
    { label: "Total clicks", value: clicks.toLocaleString(), note: "From your tracking links" },
    { label: "Conversions", value: conversions.toLocaleString(), note: "Attributed to your links" },
    { label: "Pending commission", value: money(commissionByStatus("PENDING")), note: "Awaiting confirmation" },
    { label: "Total earnings", value: money(liveCommissions.reduce((total, item) => total + Number(item.commissionAmount || 0), 0)), note: "Returned by your account" },
  ] : resellerDashboard.metrics
  const campaigns = backendMode ? liveApplications.map((application) => ({ title: application.title, status: displayStatus(application.applicationStatus) })) : resellerDashboard.campaigns.map((title) => ({ title, status: "Active" }))
  const trackingLinks = backendMode ? liveTrackingLinks.map((item) => ({ campaign: item.campaign, url: item.trackingUrl, clicks: String(item.clicks), conversions: String(item.conversions) })) : resellerDashboard.trackingLinks
  const commissions = backendMode ? liveCommissions.map((item) => ({ campaign: item.campaign, order: "Commission record", amount: money(item.commissionAmount), date: item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "-", status: displayStatus(item.status) === "Active" ? "Approved" : displayStatus(item.status) })) : resellerDashboard.commissions
  const [copied, setCopied] = useState("")
  async function copyLink(url: string) {
    try {
      await navigator.clipboard.writeText(url.startsWith("http") ? url : `https://${url}`)
      setCopied(url)
      window.setTimeout(() => setCopied(""), 1800)
    } catch {
      setCopied("unavailable")
    }
  }

  return <>
    <div><p className="text-sm font-medium text-teal-800">Reseller Portal</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">Overview</h1><p className="mt-1 text-sm text-slate-500">Your partner performance and earnings at a glance.</p></div>
    {loadingAccountData&&<p role="status" className="rounded-lg border border-sky-100 bg-sky-50 px-4 py-3 text-sm text-sky-900">Loading account data...</p>}{accountDataError&&<p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-900">{accountDataError}</p>}<div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric) => <Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-3xl">{metric.value}</CardTitle></CardHeader><CardContent className="text-xs text-emerald-700">{metric.note}</CardContent></Card>)}
      {!backendMode&&resellerDashboard.socialViews.map((metric, index) => {
        const isTikTok = metric.platform === "TikTok"
        const Icon = isTikTok ? Music2 : FacebookBrandIcon
        return <Card key={metric.platform} style={{ animationDelay: `${index * 140}ms` }} className={`social-view-card group relative isolate overflow-hidden border-0 shadow-md ring-1 ring-inset ring-white/15 xl:col-span-2 ${isTikTok ? "bg-gradient-to-br from-slate-950 via-[#171528] to-slate-900 text-white" : "bg-gradient-to-br from-[#0757d5] via-[#1877f2] to-[#56a0ff] text-white"}`}>
          <div aria-hidden="true" className={`pointer-events-none absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${isTikTok ? "from-cyan-300 via-fuchsia-400 to-cyan-300" : "from-white/35 via-white to-white/35"}`} />
          <div aria-hidden="true" className="social-view-sheen pointer-events-none absolute -inset-y-8 -left-1/2 w-1/3 skew-x-[-18deg] bg-gradient-to-r from-transparent via-white/15 to-transparent" />
          <div aria-hidden="true" className={`social-view-glow pointer-events-none absolute -right-12 -top-16 size-56 rounded-full blur-3xl transition-opacity duration-300 group-hover:opacity-90 ${isTikTok ? "bg-fuchsia-500/25" : "bg-sky-200/35"}`} />
          {isTikTok && <div aria-hidden="true" className="social-view-glow social-view-glow-delayed pointer-events-none absolute -bottom-24 left-1/3 size-48 rounded-full bg-cyan-400/15 blur-3xl" />}
          <CardContent className="relative flex min-h-40 items-center justify-between gap-4 p-5 sm:p-6">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2.5">
                <span className={`flex size-11 shrink-0 items-center justify-center rounded-2xl shadow-lg ring-1 ring-white/15 ${isTikTok ? "bg-white/10" : "bg-white"}`}>
                  {isTikTok ? <span className="relative flex"><Music2 aria-hidden="true" className="absolute -left-0.5 top-0.5 size-6 text-cyan-300"/><Music2 aria-hidden="true" className="absolute left-0.5 -top-0.5 size-6 text-pink-400"/><Music2 aria-hidden="true" className="relative size-6 text-white"/></span> : <Icon aria-hidden={true} className="size-8"/>}
                </span>
                <CardDescription className="font-semibold tracking-wide text-white/85">{metric.platform} Views</CardDescription>
                <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-white/75"><Sparkles aria-hidden="true" className="size-3"/>Social reach</span>
              </div>
              <CardTitle className="mt-4 text-4xl font-semibold tracking-tight text-white sm:text-5xl">{metric.views.toLocaleString("en-US")}</CardTitle>
              <div className="mt-3 flex items-center gap-1.5 text-xs text-white/70"><Eye aria-hidden="true" className="size-3.5"/>Sample total views <span className="text-white/40">·</span> No comparison data</div>
            </div>
            <div aria-hidden="true" className={`hidden shrink-0 items-center justify-center rounded-full p-5 ring-1 ring-white/15 sm:flex ${isTikTok ? "bg-white/[0.06]" : "bg-white/10"}`}>
              {isTikTok ? <Music2 className="size-12 text-white/20"/> : <Icon className="size-12 opacity-25"/>}
            </div>
          </CardContent>
        </Card>
      })}
    </div>
    <div className="rounded-xl border border-teal-100 bg-teal-50 px-4 py-3 text-sm text-slate-700">Conversion rate <strong className="text-teal-900">{clicks ? (conversions * 100 / clicks).toFixed(2) : "0.00"}%</strong><span className="text-slate-500"> · Calculated from your tracking-link clicks and conversions.</span></div>
    <div className="grid gap-4 sm:grid-cols-3">{commissionSummary.map((item)=><Card key={item.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{item.label}</CardDescription><CardTitle className="text-2xl">{item.value}</CardTitle></CardHeader><CardContent className="text-xs text-slate-500">{item.note}</CardContent></Card>)}</div>
    <div className="grid gap-4 xl:grid-cols-[1.4fr_0.8fr]"><Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Your performance</CardTitle><CardDescription>Last 30 days · through 14 Oct 2026</CardDescription></CardHeader><CardContent><div className="flex h-48 items-end gap-2 border-b border-slate-100 px-1 sm:gap-4">{resellerDashboard.chart.map((height,index)=><div key={index} className="flex h-full flex-1 items-end justify-center gap-1"><span className="w-2/5 rounded-t bg-teal-700" style={{height:`${height}%`}} title={`${600+index*18} clicks`}/><span className="w-2/5 rounded-t bg-amber-400" style={{height:`${resellerDashboard.linkConversions[index]}%`}} title={`${9+Math.floor(index/2)} conversions`}/></div>)}</div><div className="mt-3 flex justify-between text-[10px] text-slate-400"><span>Sep 15</span><span>Sep 22</span><span>Sep 29</span><span>Oct 6</span><span>Oct 13</span></div><div className="mt-4 flex gap-5 text-xs text-slate-500"><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-teal-700"/>Clicks</span><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-amber-400"/>Conversions</span></div><p className="mt-3 text-xs text-slate-400">Daily sample counts · each series indexed to its own peak</p></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Campaigns you joined</CardTitle><CardDescription>Active partner campaigns</CardDescription></CardHeader><CardContent className="space-y-3">{campaigns.map((campaign)=><div key={campaign.title} className="flex items-center justify-between gap-2 rounded-lg border border-slate-100 px-3 py-3"><span className="text-sm font-medium">{campaign.title}</span><Badge className={campaign.status==="Pending"?"bg-amber-50 text-amber-900":"bg-emerald-50 text-emerald-800"}>{campaign.status}</Badge></div>)}</CardContent></Card></div>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Recent tracking links</CardTitle><CardDescription>Referral links and attributed results</CardDescription></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[700px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Campaign</th><th className="pb-3 font-medium">Tracking URL</th><th className="pb-3 font-medium">Clicks</th><th className="pb-3 font-medium">Conversions</th><th className="pb-3 text-right font-medium">Action</th></tr></thead><tbody>{trackingLinks.map((link)=><tr key={link.url} className="border-b last:border-0"><td className="py-3.5 font-medium">{link.campaign}</td><td className="py-3.5"><span className="inline-flex items-center gap-1.5 rounded bg-slate-50 px-2 py-1 font-mono text-xs text-slate-600"><Link2 className="size-3"/>{link.url}</span></td><td className="py-3.5 text-slate-600">{link.clicks}</td><td className="py-3.5 text-slate-600">{link.conversions}</td><td className="py-3.5 text-right"><Button size="sm" variant="outline" onClick={()=>copyLink(link.url)}>{copied===link.url?<Check className="size-3.5"/>:<Copy className="size-3.5"/>}{copied===link.url?"Copied":"Copy link"}</Button></td></tr>)}</tbody></table>{copied==="unavailable"&&<p role="status" className="mt-3 text-sm text-amber-800">Clipboard access is unavailable in this browser context.</p>}</div></CardContent></Card>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><div><CardTitle>Recent commissions</CardTitle><CardDescription>Illustrative attributed bookings</CardDescription></div><Link href="/reseller/commissions" className="inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-teal-800 hover:bg-slate-100">View all <ArrowUpRight className="size-4"/></Link></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[700px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Campaign</th><th className="pb-3 font-medium">Order</th><th className="pb-3 font-medium">Commission</th><th className="pb-3 font-medium">Date</th><th className="pb-3 font-medium">Status</th></tr></thead><tbody>{commissions.map((item)=><tr key={item.order} className="border-b last:border-0"><td className="py-3.5 font-medium">{item.campaign}</td><td className="py-3.5 text-slate-600">{item.order}</td><td className="py-3.5 font-medium">{item.amount}</td><td className="py-3.5 text-slate-600">{item.date}</td><td className="py-3.5"><Badge className={item.status==="Paid"?"bg-emerald-50 text-emerald-800":item.status==="Approved"?"bg-sky-50 text-sky-800":"bg-amber-50 text-amber-900"}>{item.status}</Badge></td></tr>)}</tbody></table></div></CardContent></Card>
  </>
}
