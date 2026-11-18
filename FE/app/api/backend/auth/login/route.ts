import { proxyBackend } from "@/lib/backend-proxy"

export async function POST(request: Request) {
  return proxyBackend("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text(),
  })
}
