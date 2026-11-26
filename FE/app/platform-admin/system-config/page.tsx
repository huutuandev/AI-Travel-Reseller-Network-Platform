"use client"

import { useCallback, useEffect, useState } from "react"
import { RefreshCw, Save } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { adminApi, formatAdminDate, type AdminPage, type PlatformEvent, type PlatformSettings } from "@/lib/admin/api"

const initialSettings: PlatformSettings = { routeMode: "SUBDOMAIN_PREVIEW", baseDomain: "", captionTemplate: "", videoTemplate: "", version: 0 }

export default function SystemConfigurationPage() {
  const [settings, setSettings] = useState<PlatformSettings>(initialSettings)
  const [logs, setLogs] = useState<AdminPage<PlatformEvent> | null>(null)
  const [logPage, setLogPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")
  const [feedback, setFeedback] = useState("")

  const load = useCallback(async () => {
    setLoading(true); setError("")
    try { const [config, logResult] = await Promise.all([adminApi.settings(), adminApi.logs(logPage, 20)]); setSettings(config); setLogs(logResult) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load platform settings.") }
    finally { setLoading(false) }
  }, [logPage])
  useEffect(() => { void load() }, [load])

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(""); setFeedback("")
    try {
      const updated = await adminApi.saveSettings({ routeMode: settings.routeMode, baseDomain: settings.baseDomain.trim(), captionTemplate: settings.captionTemplate, videoTemplate: settings.videoTemplate, version: settings.version })
      setSettings(updated); setFeedback("Platform settings saved.")
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to save platform settings.") }
    finally { setSaving(false) }
  }

  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Platform operations</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">System configuration</h1><p className="mt-1 text-sm text-slate-500">Platform routing defaults, AI templates and operational logs.</p></div><Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`}/>Refresh</Button></div>
    {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error}</div>}
    <form className="space-y-5" onSubmit={(event) => void save(event)}>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-start gap-3"><span className="flex size-8 items-center justify-center rounded-full bg-teal-50 text-sm font-semibold text-teal-900">01</span><div><CardTitle>Tenant routing</CardTitle><CardDescription className="mt-1">Configuration currently supported by the platform service.</CardDescription></div></div></CardHeader><CardContent className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="route-mode">Tenant resolution mode</Label><select id="route-mode" value={settings.routeMode} disabled className="h-9 w-full rounded-md border border-input bg-muted px-3 text-sm"><option value="SUBDOMAIN_PREVIEW">Subdomain preview</option></select><p className="text-xs text-slate-500">The API currently accepts SUBDOMAIN_PREVIEW only.</p></div><div className="space-y-2"><Label htmlFor="base-domain">Base domain</Label><Input id="base-domain" value={settings.baseDomain} onChange={(event) => setSettings((current) => ({...current, baseDomain:event.target.value}))} disabled={loading || saving} required maxLength={253} pattern="(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}"/><p className="text-xs text-slate-500">Use a valid domain, for example `travel.example`.</p></div></CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-start gap-3"><span className="flex size-8 items-center justify-center rounded-full bg-teal-50 text-sm font-semibold text-teal-900">02</span><div><CardTitle>AI content templates</CardTitle><CardDescription className="mt-1">Template text stored in the platform settings record.</CardDescription></div></div></CardHeader><CardContent className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="caption-template">Social caption template</Label><Textarea id="caption-template" rows={5} value={settings.captionTemplate} onChange={(event) => setSettings((current) => ({...current, captionTemplate:event.target.value}))} disabled={loading || saving} required maxLength={10000}/></div><div className="space-y-2"><Label htmlFor="video-template">Video script template</Label><Textarea id="video-template" rows={5} value={settings.videoTemplate} onChange={(event) => setSettings((current) => ({...current, videoTemplate:event.target.value}))} disabled={loading || saving} required maxLength={10000}/></div></CardContent></Card>
      <div className="flex flex-wrap items-center gap-3"><Button type="submit" className="bg-teal-800 hover:bg-teal-900" disabled={loading || saving}><Save className="size-4"/>{saving ? "Saving…" : "Save settings"}</Button>{feedback && <p role="status" className="text-sm text-emerald-800">{feedback}</p>}{settings.updatedAt && <span className="text-xs text-slate-500">Last updated {formatAdminDate(settings.updatedAt)}</span>}</div>
    </form>
    <Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-wrap flex-row items-center justify-between gap-3"><div><CardTitle>Platform logs</CardTitle><CardDescription>Audit events recorded by the Admin API</CardDescription></div><Badge variant="outline">{logs?.totalElements ?? "—"} events</Badge></CardHeader><CardContent><div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Time</th><th className="pb-3 font-medium">Event</th><th className="pb-3 font-medium">Scope</th><th className="pb-3 font-medium">Result</th><th className="pb-3 font-medium">Details</th></tr></thead><tbody>{(logs?.content ?? []).map((log) => <tr key={log.id} className="border-b last:border-0"><td className="py-3.5 text-slate-600">{formatAdminDate(log.createdAt)}</td><td className="py-3.5 font-medium">{log.eventType.replaceAll("_", " ")}</td><td className="py-3.5 text-slate-600">{log.businessName || log.tenantCode || log.area}</td><td className="py-3.5"><Badge className={log.result === "SUCCESS" ? "bg-emerald-50 text-emerald-800" : "bg-slate-100 text-slate-700"}>{log.result}</Badge></td><td className="py-3.5 text-slate-600">{log.reason || (log.previousStatus || log.newStatus ? `${log.previousStatus || "—"} → ${log.newStatus || "—"}` : log.tenantId || "—")}</td></tr>)}</tbody></table>{!loading && logs?.content.length === 0 && <p className="py-8 text-center text-sm text-slate-500">No platform events recorded.</p>}</div><div className="mt-4 flex items-center justify-between text-sm text-slate-500"><span>Page {logPage + 1} of {Math.max(1, logs?.totalPages ?? 0)}</span><div className="flex gap-2"><Button variant="outline" size="sm" disabled={logPage <= 0 || loading} onClick={() => setLogPage((current) => Math.max(0, current - 1))}>Previous</Button><Button variant="outline" size="sm" disabled={loading || logPage + 1 >= (logs?.totalPages ?? 0)} onClick={() => setLogPage((current) => current + 1)}>Next</Button></div></div></CardContent></Card>
  </>
}
