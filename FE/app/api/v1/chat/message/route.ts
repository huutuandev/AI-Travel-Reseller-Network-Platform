type ChatMessageRequest = {
  sessionId?: string | null
  message?: string
}

const configuredBackendUrl = process.env.CHAT_API_BASE_URL?.trim()
const backendBaseUrls = configuredBackendUrl
  ? [configuredBackendUrl.replace(/\/$/, "")]
  : ["http://localhost:8080", "http://backend:8080"]

export async function POST(request: Request) {
  let payload: ChatMessageRequest

  try {
    payload = (await request.json()) as ChatMessageRequest
  } catch {
    return Response.json({ success: false, message: "Invalid JSON request." }, { status: 400 })
  }

  if (typeof payload.message !== "string" || !payload.message.trim() || payload.message.length > 2000) {
    return Response.json({ success: false, message: "Message must contain between 1 and 2000 characters." }, { status: 400 })
  }

  try {
    let backendResponse: Response | undefined
    for (const baseUrl of backendBaseUrls) {
      try {
        backendResponse = await fetch(`${baseUrl}/api/v1/chat`, {
          method: "POST",
          headers: { "Content-Type": "application/json", Accept: "application/json" },
          body: JSON.stringify({
            ...(typeof payload.sessionId === "string" && payload.sessionId.trim() ? { sessionId: payload.sessionId.trim() } : {}),
            message: payload.message.trim(),
          }),
          cache: "no-store",
        })
        break
      } catch {
        // A local Next.js process uses localhost; the Compose frontend uses the backend service hostname.
      }
    }
    if (!backendResponse) throw new Error("Could not reach the chat backend.")

    const responseBody = await backendResponse.text()
    return new Response(responseBody || null, {
      status: backendResponse.status,
      headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
    })
  } catch {
    return Response.json(
      { success: false, message: "Không kết nối được chatbot. Hãy kiểm tra BE đang chạy rồi thử lại nhé." },
      { status: 502 },
    )
  }
}
