export const BACKEND_ACCESS_TOKEN_KEY = "wayfarer-backend-access-token"

export function getBackendAccessToken(): string | null {
  if (typeof window === "undefined") return null
  return window.sessionStorage.getItem(BACKEND_ACCESS_TOKEN_KEY)
}

export function decodeBackendAccessToken(token: string): Record<string, unknown> | null {
  try {
    const encoded = token.split(".")[1]
    if (!encoded) return null
    const base64 = encoded.replace(/-/g, "+").replace(/_/g, "/")
    const binary = window.atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "="))
    const json = decodeURIComponent(Array.from(binary, (character) =>
      "%" + character.charCodeAt(0).toString(16).padStart(2, "0"),
    ).join(""))
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return null
  }
}
export const BACKEND_REFRESH_TOKEN_KEY = "wayfarer-backend-refresh-token"

export function getBackendRefreshToken(): string | null {
  if (typeof window === "undefined") return null
  return window.sessionStorage.getItem(BACKEND_REFRESH_TOKEN_KEY)
}

let refreshRequest: Promise<string | null> | null = null

export function refreshBackendAccessToken(): Promise<string | null> {
  if (typeof window === "undefined") return Promise.resolve(null)
  if (refreshRequest) return refreshRequest
  const refreshToken = getBackendRefreshToken()
  if (!refreshToken) return Promise.resolve(null)

  refreshRequest = (async () => {
    try {
      const response = await fetch("/api/backend/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
        cache: "no-store",
      })
      const result = await response.json().catch(() => null) as { success?: boolean; data?: { accessToken?: string; refreshToken?: string } } | null
      const accessToken = result?.data?.accessToken
      const nextRefreshToken = result?.data?.refreshToken
      if (!response.ok || !result?.success || !accessToken || !nextRefreshToken) return null
      window.sessionStorage.setItem(BACKEND_ACCESS_TOKEN_KEY, accessToken)
      window.sessionStorage.setItem(BACKEND_REFRESH_TOKEN_KEY, nextRefreshToken)
      return accessToken
    } catch {
      return null
    } finally {
      refreshRequest = null
    }
  })()
  return refreshRequest
}

export async function logoutBackendSession(): Promise<void> {
  if (typeof window === "undefined") return
  const refreshToken = getBackendRefreshToken()
  if (!refreshToken) return
  const accessToken = getBackendAccessToken()
  try {
    await fetch("/api/backend/auth/logout", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    })
  } catch {
    // Local sign-out should still complete if the BE is offline.
  }
}