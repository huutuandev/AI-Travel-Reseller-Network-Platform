import { proxyBackend } from "@/lib/backend-proxy"

export async function GET(request: Request) {
  const authorization = request.headers.get("authorization")
  if (!authorization) {
    return Response.json({ success: false, message: "Sign in with a reseller account first." }, { status: 401 })
  }
  return proxyBackend("/api/v1/reseller/social-accounts/tiktok/oauth-url", {
    headers: { authorization },
  })
}
