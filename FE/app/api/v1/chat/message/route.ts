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
    return Response.json({ success: false, message: "Yêu cầu chat không hợp lệ." }, { status: 400 })
  }

  if (typeof payload.message !== "string" || !payload.message.trim() || payload.message.length > 2000) {
    return Response.json({ success: false, message: "Tin nhắn cần có từ 1 đến 2000 ký tự." }, { status: 400 })
  }

  for (const baseUrl of backendBaseUrls) {
    try {
      const backendResponse = await fetch(`${baseUrl}/api/v1/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({
          ...(typeof payload.sessionId === "string" && payload.sessionId.trim()
            ? { sessionId: payload.sessionId.trim() }
            : {}),
          message: payload.message.trim(),
        }),
        cache: "no-store",
      })
      const responseBody = await backendResponse.text()
      return new Response(responseBody || null, {
        status: backendResponse.status,
        headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
      })
    } catch {
      // Local Next.js uses localhost; the Docker frontend can use the backend service hostname.
    }
  }

  return Response.json(
    { success: false, message: "Không kết nối được chatbot. Hãy kiểm tra BE đang chạy rồi thử lại nhé." },
    { status: 502 },
  )
}
