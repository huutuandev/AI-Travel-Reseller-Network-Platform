const BACKEND_API_URL = (process.env.BACKEND_API_URL || "http://localhost:8080").replace(/\/+$/, "")

export async function proxyBackend(path: string, init: RequestInit = {}): Promise<Response> {
  try {
    const upstream = await fetch(BACKEND_API_URL + path, { ...init, cache: "no-store" })
    const headers = new Headers()
    const contentType = upstream.headers.get("content-type")
    if (contentType) headers.set("content-type", contentType)
    return new Response(await upstream.arrayBuffer(), { status: upstream.status, headers })
  } catch {
    return Response.json(
      { success: false, message: "Backend API is unavailable. Check that the backend service is running." },
      { status: 502 },
    )
  }
}
