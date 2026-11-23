import { proxyBackend } from "@/lib/backend-proxy"

const allowedRoutes: Record<string, string[]> = {
  "GET": [
    "campaigns",
    "campaigns/applications",
    "tracking-links",
    "commissions",
    "campaigns/:id",
    "social-accounts/tiktok/videos",
    "social-accounts/tiktok/oauth-url",
    "social-accounts/tiktok/:id/videos",
  ],
  "POST": [
    "campaigns/:id/applications",
    "social-videos",
  ],
}

function isAllowed(method: string, path: string) {
  return (allowedRoutes[method] ?? []).some((pattern) => {
    const expression = new RegExp(`^${pattern.split("/").map((part) => part === ":id" ? "[0-9a-f-]{36}" : part).join("/")}$`, "i")
    return expression.test(path)
  })
}

async function handle(request: Request) {
  const authorization = request.headers.get("authorization")
  if (!authorization) return Response.json({ success: false, message: "Sign in with a reseller account first." }, { status: 401 })

  const url = new URL(request.url)
  const path = url.pathname.split("/").slice(4).join("/")
  if (!isAllowed(request.method, path)) {
    return Response.json({ success: false, message: "This reseller API route is not available through the frontend proxy." }, { status: 404 })
  }

  const headers = new Headers({ authorization })
  const init: RequestInit = { method: request.method, headers }
  if (request.method === "POST") {
    headers.set("content-type", request.headers.get("content-type") ?? "application/json")
    init.body = await request.text()
  }
  return proxyBackend(`/api/v1/reseller/${path}${url.search}`, init)
}

export const GET = handle
export const POST = handle