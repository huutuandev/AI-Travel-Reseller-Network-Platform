const configuredBackendUrl = process.env.BACKEND_API_BASE_URL?.trim()
const backendBaseUrls = configuredBackendUrl
  ? [configuredBackendUrl.replace(/\/$/, "")]
  : ["http://localhost:8080", "http://backend:8080"]
const allowedPrefixes = ["api/v1/admin", "api/v1/auth"]

async function proxy(request: Request, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params
  const apiPath = path.join("/")
  if (!allowedPrefixes.some((prefix) => apiPath === prefix || apiPath.startsWith(`${prefix}/`))) {
    return Response.json({ success: false, message: "Unsupported API route." }, { status: 404 })
  }
  const incomingUrl = new URL(request.url)
  const headers = new Headers()
  for (const name of ["accept", "authorization", "content-type"]) {
    const value = request.headers.get(name)
    if (value) headers.set(name, value)
  }
  const body = request.method === "GET" || request.method === "HEAD" ? undefined : await request.arrayBuffer()
  for (const backendBaseUrl of backendBaseUrls) {
    try {
      const upstream = await fetch(`${backendBaseUrl}/${apiPath}${incomingUrl.search}`, { method: request.method, headers, body, cache: "no-store" })
      return new Response(upstream.body, { status: upstream.status, headers: { "Content-Type": upstream.headers.get("Content-Type") ?? "application/json" } })
    } catch {
      // Local Next.js uses localhost; the Docker frontend can reach the backend service by its Compose name.
    }
  }
  return Response.json({ success: false, message: "Backend service is unavailable." }, { status: 502 })
}

export const GET = proxy
export const POST = proxy
export const PUT = proxy
export const PATCH = proxy
