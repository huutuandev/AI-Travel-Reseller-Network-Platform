import { proxyBackend } from "@/lib/backend-proxy"

export async function POST(request: Request) {
  const authorization = request.headers.get("authorization")
  if (!authorization) return Response.json({ success: false, message: "Sign in first." }, { status: 401 })
  return proxyBackend("/api/v1/auth/logout", {
    method: "POST",
    headers: { "Content-Type": "application/json", authorization },
    body: await request.text(),
  })
}
