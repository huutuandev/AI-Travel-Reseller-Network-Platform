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
