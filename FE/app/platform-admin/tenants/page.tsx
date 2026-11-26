"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { adminApi, formatAdminDate, toDisplayStatus, type AdminPage, type Tenant } from "@/lib/admin/api"

const statusStyles: Record<Tenant["status"], string> = { ACTIVE: "bg-emerald-50 text-emerald-800", PENDING: "bg-amber-50 text-amber-900", SUSPENDED: "bg-slate-100 text-slate-700" }

export default function TenantManagementPage() {
  const [pageData, setPageData] = useState<AdminPage<Tenant> | null>(null)
  const [query, setQuery] = useState("")
  const [status, setStatus] = useState("")
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [busyId, setBusyId] = useState("")

  const load = useCallback(async () => {
    setLoading(true); setError("")
    try { setPageData(await adminApi.tenants({ page, size: 10, search: query, status })) }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to load tenant records.") }
    finally { setLoading(false) }
  }, [page, query, status])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, query ? 300 : 0); return () => window.clearTimeout(timer) }, [load, query])

  async function changeStatus(tenant: Tenant) {
    const nextStatus = tenant.status === "ACTIVE" ? "SUSPENDED" : tenant.status === "SUSPENDED" ? "ACTIVE" : null
    if (!nextStatus) return
    const reason = nextStatus === "SUSPENDED" ? window.prompt(`Reason for suspending ${tenant.businessName}:`)?.trim() : undefined
    if (nextStatus === "SUSPENDED" && !reason) return
    setBusyId(tenant.id); setError("")
    try { await adminApi.changeTenantStatus(tenant.id, { status: nextStatus, ...(reason ? { reason } : {}), version: tenant.version }); await load() }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Unable to change tenant status.") }
    finally { setBusyId("") }
  }

  const tenants = pageData?.content ?? []
  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Platform operations</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">Tenant management</h1><p className="mt-1 text-sm text-slate-500">Review businesses using the shared platform and manage account status.</p></div><Badge variant="outline" className="bg-white px-3 py-1.5">{pageData?.totalElements ?? "—"} tenants</Badge></div>
    {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error}</div>}
    <Card className="border-slate-200/80 shadow-sm"><CardContent className="space-y-4"><div className="flex flex-col gap-3 sm:flex-row"><Input type="search" aria-label="Search tenants" placeholder="Search business, tenant ID or subdomain" value={query} onChange={(event) => { setPage(0); setQuery(event.target.value) }} className="sm:max-w-md"/><select aria-label="Filter by tenant status" value={status} onChange={(event) => { setPage(0); setStatus(event.target.value) }} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="PENDING">Pending</option><option value="SUSPENDED">Suspended</option></select></div>
      <div className="overflow-x-auto"><table className="w-full min-w-[800px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500"><th className="pb-3 font-medium">Business</th><th className="pb-3 font-medium">Tenant ID / subdomain</th><th className="pb-3 font-medium">Status</th><th className="pb-3 font-medium">Created</th><th className="pb-3 font-medium">Actions</th></tr></thead><tbody>{tenants.map((tenant) => <tr key={tenant.id} className="border-b last:border-0"><td className="py-3.5"><strong className="block font-medium">{tenant.businessName}</strong><span className="text-xs text-slate-500">{tenant.description || tenant.code}</span></td><td className="py-3.5"><span className="block font-mono text-xs">{tenant.id}</span><span className="text-xs text-slate-500">{tenant.subdomain}{tenant.customDomain ? ` · ${tenant.customDomain}` : ""}</span></td><td className="py-3.5"><Badge className={statusStyles[tenant.status]}>{toDisplayStatus(tenant.status)}</Badge></td><td className="py-3.5 text-slate-600">{formatAdminDate(tenant.createdAt)}</td><td className="py-3.5"><div className="flex items-center gap-2"><Link href={`/platform-admin/tenant-detail?tenant=${encodeURIComponent(tenant.id)}`} className="text-sm font-medium text-teal-800 hover:underline">View details</Link>{tenant.status !== "PENDING" && <Button size="sm" variant="outline" disabled={busyId === tenant.id} onClick={() => void changeStatus(tenant)}>{busyId === tenant.id ? "Saving…" : tenant.status === "ACTIVE" ? "Suspend" : "Activate"}</Button>}</div></td></tr>)}</tbody></table>{!loading && tenants.length === 0 && <p className="py-8 text-center text-sm text-slate-500">No tenants match these filters.</p>}{loading && <p className="py-8 text-center text-sm text-slate-500">Loading tenants…</p>}</div>
      <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500"><span>Showing {tenants.length} of {pageData?.totalElements ?? 0} tenants</span><div className="flex gap-2"><Button variant="outline" size="sm" disabled={page <= 0 || loading} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</Button><span className="flex items-center px-2">Page {page + 1} of {Math.max(1, pageData?.totalPages ?? 0)}</span><Button variant="outline" size="sm" disabled={loading || page + 1 >= (pageData?.totalPages ?? 0)} onClick={() => setPage((current) => current + 1)}>Next</Button></div></div>
    </CardContent></Card>
  </>
}
