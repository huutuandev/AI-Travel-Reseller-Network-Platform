"use client"

import { useState } from "react"
import { Save } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { platformSystemLogs } from "@/lib/mock-data"

export default function SystemConfigurationPage() {
  const [feedback, setFeedback] = useState("")
  return <>
    <div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Platform operations</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">System configuration</h1><p className="mt-1 text-sm text-slate-500">Review platform-wide routing concepts, AI templates and operational logs.</p></div>
    <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">All controls below are local UI examples. Changes are not saved to a platform service.</p>
    <form className="space-y-5" onSubmit={(event)=>{event.preventDefault();setFeedback("Preview settings applied in this session only. No information was sent or saved.")}}>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-start gap-3"><span className="flex size-8 items-center justify-center rounded-full bg-teal-50 text-sm font-semibold text-teal-900">01</span><div><CardTitle>Tenant routing</CardTitle><CardDescription className="mt-1">Conceptual defaults for branded tenant experiences.</CardDescription></div></div></CardHeader><CardContent className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="route-mode">Tenant resolution mode</Label><select id="route-mode" className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"><option>Subdomain preview</option><option>Custom domain (planned)</option></select></div><div className="space-y-2"><Label htmlFor="base-domain">Sample base domain</Label><Input id="base-domain" defaultValue="aitravel.example"/><p className="text-xs text-slate-500">Example only; no DNS or domain lookup is active.</p></div></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-start gap-3"><span className="flex size-8 items-center justify-center rounded-full bg-teal-50 text-sm font-semibold text-teal-900">02</span><div><CardTitle>AI content templates</CardTitle><CardDescription className="mt-1">Sample prompt guidance for caption and video-script generation.</CardDescription></div></div></CardHeader><CardContent className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="caption-template">Social caption template</Label><Textarea id="caption-template" rows={5} defaultValue="Use product highlights, destination context and the selected tone. Avoid unverified claims."/></div><div className="space-y-2"><Label htmlFor="video-template">Video script template</Label><Textarea id="video-template" rows={5} defaultValue="Create a short opening hook, three itinerary moments and a clear call to explore the campaign."/></div></CardContent></Card>
      <div className="flex flex-wrap items-center gap-3"><Button type="submit" className="bg-teal-800 hover:bg-teal-900"><Save className="size-4"/>Save preview settings</Button>{feedback&&<p role="status" className="text-sm text-emerald-800">{feedback}</p>}</div>
    </form>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><div><CardTitle>Recent platform logs</CardTitle><CardDescription>Illustrative operational events</CardDescription></div><Badge variant="outline">Sample log</Badge></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Time</th><th className="pb-3 font-medium">Event</th><th className="pb-3 font-medium">Scope</th><th className="pb-3 font-medium">Result</th></tr></thead><tbody>{platformSystemLogs.map((log)=><tr key={log.time} className="border-b last:border-0"><td className="py-3.5 text-slate-600">{log.time}</td><td className="py-3.5 font-medium">{log.event}</td><td className="py-3.5 text-slate-600">{log.scope}</td><td className="py-3.5"><Badge className={log.result==="Success"?"bg-emerald-50 text-emerald-800":log.result==="Pending"?"bg-amber-50 text-amber-900":"bg-slate-100 text-slate-700"}>{log.result}</Badge></td></tr>)}</tbody></table></div></CardContent></Card>
  </>
}
