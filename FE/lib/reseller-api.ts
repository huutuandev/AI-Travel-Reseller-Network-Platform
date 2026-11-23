import { getBackendAccessToken, refreshBackendAccessToken } from "@/lib/backend-api"

export type ApiResponse<T> = { success: boolean; message?: string; data: T }
export type BackendCampaign = {
  id: string
  title: string
  description: string | null
  status: string
  commissionType: string
  commissionRate: number
  commissionCurrency: string | null
  startDate: string | null
  endDate: string | null
}
export type BackendApplication = {
  applicationId: string
  campaignId: string
  title: string
  description: string | null
  campaignStatus: string
  applicationStatus: string
  commissionType: string
  commissionRate: number
  appliedAt: string
}
export type BackendTrackingLink = { campaign: string; trackingUrl: string; clicks: number; conversions: number; created: string }
export type BackendCommission = { campaign: string; orderAmount: number; commissionAmount: number; currency: string; status: string; createdAt: string }
export type BackendTikTokVideo = {
  id: string
  title: string | null
  description: string | null
  createTime: string | null
  coverImageUrl: string | null
  shareUrl: string | null
  embedLink: string | null
  viewCount: number | null
  likeCount: number | null
  commentCount: number | null
  shareCount: number | null
}
export type BackendTikTokVideos = { socialAccountId: string; videos: BackendTikTokVideo[]; cursor: number | null; hasMore: boolean }
export type BackendSocialVideo = {
  id: string
  platform: string
  externalVideoId: string
  postUrl: string
  views: number
  likes: number
  metricsStatus: string
  metricsFetchedAt: string | null
  createdAt: string
}

export class ResellerApiError extends Error {
  status: number
  constructor(message: string, status = 0) {
    super(message)
    this.name = "ResellerApiError"
    this.status = status
  }
}

export async function resellerApi<T>(path: string, options: { method?: "GET" | "POST"; body?: unknown } = {}): Promise<T> {
  let accessToken = getBackendAccessToken()
  if (!accessToken) throw new ResellerApiError("Sign in with a real reseller account to load this data.", 401)
  const method = options.method ?? "GET"
  const request = (token: string) => fetch(`/api/backend/reseller/${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(options.body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    ...(options.body === undefined ? {} : { body: JSON.stringify(options.body) }),
    cache: "no-store",
  })
  let response = await request(accessToken)
  if (response.status === 401) {
    const refreshedToken = await refreshBackendAccessToken()
    if (refreshedToken) {
      accessToken = refreshedToken
      response = await request(accessToken)
    }
  }
  const result = await response.json().catch(() => null) as ApiResponse<T> | null
  if (!response.ok || !result?.success) {
    throw new ResellerApiError(result?.message || `Reseller API request failed (${response.status}).`, response.status)
  }
  return result.data
}

export function formatCommission(type: string, rate: number, currency?: string | null) {
  if (type.toUpperCase().includes("PERCENT")) return `${rate}%`
  return new Intl.NumberFormat("en-US", { style: "currency", currency: currency || "USD" }).format(rate)
}

export function displayStatus(value: string) {
  const normalized = value.toUpperCase()
  if (normalized === "APPROVED" || normalized === "ACTIVE") return "Active"
  if (normalized === "PENDING" || normalized === "PENDING_REVIEW") return "Pending"
  if (normalized === "REJECTED") return "Rejected"
  if (normalized === "PAUSED") return "Paused"
  return value.charAt(0) + value.slice(1).toLowerCase()
}