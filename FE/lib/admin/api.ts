import { getDemoSession, saveDemoSession, type DemoSession } from "@/lib/mock-auth"

export type ApiEnvelope<T> = { success: boolean; message: string; data: T }
export type AdminPage<T> = { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export type Tenant = {
  id: string; code: string; businessName: string; description?: string | null; subdomain: string
  customDomain?: string | null; status: "ACTIVE" | "PENDING" | "SUSPENDED"; createdAt: string; version: number
  contact?: { name?: string; ownerEmail?: string; email?: string; phone?: string; address?: string }
  branding?: { logoUrl?: string; primaryColor?: string; secondaryColor?: string } | null
  stats?: { activeCampaigns?: number; totalResellers?: number; attributedBookings?: number; clicks30Days?: number | null; clicksAvailability?: string; bookingsPeriod?: string }
}
export type PlatformOverview = {
  metrics: { totalTenants: number; newTenantsThisMonth: number; activeTenants: number; pendingTenants: number; suspendedTenants: number; activePercentage: number }
  usage: { attributedBookings: number; runningCampaigns: number; trackedClicks: number | null; clicksAvailability: string; runningCampaignsPeriod: string }
  from: string; to: string; timezone: string
}
export type PlatformEvent = {
  id: string; tenantId?: string | null; tenantCode?: string | null; businessName?: string | null; subdomain?: string | null
  actorUserId?: string | null; eventType: string; area: string; result: string; previousStatus?: string | null
  newStatus?: string | null; reason?: string | null; createdAt: string
}
export type PlatformSettings = { routeMode: "SUBDOMAIN_PREVIEW"; baseDomain: string; captionTemplate: string; videoTemplate: string; version: number; updatedAt?: string; updatedBy?: string }

async function request<T>(path: string, init: RequestInit = {}, retryRefresh = true): Promise<T> {
  let session = getDemoSession()
  if (!session?.accessToken) throw new Error("Sign in with a platform administrator account to load this page.")
  const headers = new Headers(init.headers)
  headers.set("Accept", "application/json")
  headers.set("Content-Type", "application/json")
  headers.set("Authorization", `Bearer ${session.accessToken}`)
  const response = await fetch(`/api/backend/api/v1/admin${path}`, {
    ...init,
    headers,
    cache: "no-store",
  })
  if (response.status === 401 && retryRefresh && session.refreshToken) {
    const refreshed = await fetch("/api/backend/api/v1/auth/refresh", {
      method: "POST", headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ refreshToken: session.refreshToken }), cache: "no-store",
    })
    const refreshEnvelope = await refreshed.json().catch(() => null) as ApiEnvelope<{ accessToken: string; refreshToken: string }> | null
    if (refreshed.ok && refreshEnvelope?.data?.accessToken) {
      session = { ...session, accessToken: refreshEnvelope.data.accessToken, refreshToken: refreshEnvelope.data.refreshToken }
      saveDemoSession(session)
      return request<T>(path, init, false)
    }
  }
  const envelope = await response.json().catch(() => null) as ApiEnvelope<T> | null
  if (!response.ok || !envelope?.success) throw new Error(envelope?.message || `Admin API request failed (${response.status}).`)
  return envelope.data
}

export const adminApi = {
  overview: (days = 30) => request<PlatformOverview>(`/dashboard/overview?days=${days}`),
  recentActivities: (limit = 10) => request<AdminPage<PlatformEvent>>(`/dashboard/recent-activities?limit=${limit}`),
  tenants: (params: { page?: number; size?: number; search?: string; status?: string }) => {
    const query = new URLSearchParams({ page: String(params.page ?? 0), size: String(params.size ?? 10), search: params.search ?? "", status: params.status ?? "" })
    return request<AdminPage<Tenant>>(`/tenants?${query}`)
  },
  tenant: (id: string) => request<Tenant>(`/tenants/${encodeURIComponent(id)}`),
  changeTenantStatus: (id: string, body: { status: "ACTIVE" | "SUSPENDED"; reason?: string; version: number }) => request<Tenant>(`/tenants/${encodeURIComponent(id)}/status`, { method: "PATCH", body: JSON.stringify(body) }),
  tenantLogs: (id: string, page = 0, size = 20) => request<AdminPage<PlatformEvent>>(`/tenants/${encodeURIComponent(id)}/logs?page=${page}&size=${size}`),
  settings: () => request<PlatformSettings>("/system/config"),
  saveSettings: (settings: Omit<PlatformSettings, "updatedAt" | "updatedBy">) => request<PlatformSettings>("/system/config", { method: "PUT", body: JSON.stringify(settings) }),
  logs: (page = 0, size = 20) => request<AdminPage<PlatformEvent>>(`/system/logs?page=${page}&size=${size}`),
}

export function formatAdminDate(value?: string | null) {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date)
}

export function toDisplayStatus(status: Tenant["status"]) {
  return status === "ACTIVE" ? "Active" : status === "PENDING" ? "Pending" : "Suspended"
}

export function sessionFromAccessToken(accessToken: string, refreshToken: string): DemoSession {
  const payload = accessToken.split(".")[1]
  if (!payload) throw new Error("Backend returned an invalid access token.")
  const claims = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/"))) as { id?: string; sub?: string; roles?: string }
  const roles = (claims.roles ?? "").split(",")
  const role = roles.some((item) => item.endsWith("ADMIN")) ? "platform-admin" : roles.some((item) => item.endsWith("RESELLER")) ? "reseller" : roles.some((item) => item.endsWith("MERCHANT") || item.endsWith("TENANT")) ? "tenant" : null
  if (!role) throw new Error("This account does not have a supported workspace role.")
  return { id: claims.id ?? claims.sub ?? crypto.randomUUID(), email: claims.sub ?? "", name: (claims.sub ?? "Account").split("@")[0], role, title: role === "platform-admin" ? "Platform administrator" : role === "tenant" ? "Tenant administrator" : "Travel partner", status: "active", accessToken, refreshToken }
}

export async function signOutBackend(session: DemoSession | null) {
  if (!session?.refreshToken) return
  await fetch("/api/backend/api/v1/auth/logout", { method: "POST", headers: { "Content-Type": "application/json", ...(session.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {}) }, body: JSON.stringify({ refreshToken: session.refreshToken }), cache: "no-store" }).catch(() => undefined)
}
