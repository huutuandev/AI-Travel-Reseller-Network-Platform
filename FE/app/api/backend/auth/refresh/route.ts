import { proxyBackend } from "@/lib/backend-proxy"

export async function POST(request: Request) {
  return proxyBackend("/api/v1/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text(),
  })
}
