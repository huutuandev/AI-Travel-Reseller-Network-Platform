import { proxyBackend } from "@/lib/backend-proxy"

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization")
  if (!authorization) {
    return Response.json({ success: false, message: "Sign in with a reseller account first." }, { status: 401 })
  }

  const incoming = new URL(request.url)
  const query = new URLSearchParams()
  for (const key of ["videoId", "socialAccountId", "maxCount", "cursor"]) {
    const value = incoming.searchParams.get(key)
    if (value) query.set(key, value)
  }
  const suffix = query.size ? "?" + query.toString() : ""
  return proxyBackend("/api/v1/reseller/social-accounts/tiktok/videos" + suffix, {
    headers: { authorization },
  })
}
