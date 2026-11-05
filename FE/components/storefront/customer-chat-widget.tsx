"use client"

import { useEffect, useRef, useState, type FormEvent } from "react"
import { Bot, MessageCircle, Send, Sparkles, X } from "lucide-react"

type ChatMessage = {
  id: string
  role: "assistant" | "user"
  content: string
}

type ChatApiResponse = {
  success?: boolean
  message?: string
  data?: {
    sessionId?: string
    reply?: string
  }
  sessionId?: string
  reply?: string
}

const SESSION_STORAGE_KEY = "lantern-tide-chat-session"
const greeting: ChatMessage = {
  id: "greeting",
  role: "assistant",
  content: "Xin chào! Mình có thể giúp bạn tìm hiểu các tour và điểm đến. Bạn đang muốn đi đâu?",
}
const suggestions = ["Gợi ý tour phù hợp cho tôi", "Tôi muốn khám phá Việt Nam"]

export function CustomerChatWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState("")
  const [messages, setMessages] = useState<ChatMessage[]>([greeting])
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState("")
  const [sessionId, setSessionId] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" })
  }, [messages, isSending, isOpen])

  async function sendMessage(event?: FormEvent<HTMLFormElement>, suggestedMessage?: string) {
    event?.preventDefault()
    const content = (suggestedMessage ?? message).trim()
    if (!content || isSending) return

    setError("")
    setMessage("")
    setMessages((current) => [...current, { id: crypto.randomUUID(), role: "user", content }])
    setIsSending(true)

    try {
      const activeSessionId = sessionId ?? window.localStorage.getItem(SESSION_STORAGE_KEY)
      const response = await fetch("/api/v1/chat/message", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ ...(activeSessionId ? { sessionId: activeSessionId } : {}), message: content }),
      })
      const payload = (await response.json().catch(() => null)) as ChatApiResponse | null
      const reply = payload?.data?.reply ?? payload?.reply
      const nextSessionId = payload?.data?.sessionId ?? payload?.sessionId

      if (!response.ok || payload?.success === false || !reply?.trim()) {
        throw new Error(payload?.message || "Trợ lý chưa thể trả lời lúc này. Bạn thử lại sau nhé.")
      }

      if (nextSessionId) {
        setSessionId(nextSessionId)
        window.localStorage.setItem(SESSION_STORAGE_KEY, nextSessionId)
      }
      setMessages((current) => [...current, { id: crypto.randomUUID(), role: "assistant", content: reply.trim() }])
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không kết nối được chatbot. Bạn thử lại sau nhé.")
    } finally {
      setIsSending(false)
    }
  }

  function startNewConversation() {
    window.localStorage.removeItem(SESSION_STORAGE_KEY)
    setSessionId(null)
    setMessages([greeting])
    setError("")
    setMessage("")
  }

  return (
    <div className="customer-chat-widget">
      {isOpen && (
        <section className="customer-chat-panel" aria-label="Chat với Lantern & Tide" aria-live="polite">
          <header className="customer-chat-header">
            <div className="customer-chat-brand-icon" aria-hidden="true"><Sparkles /></div>
            <div className="customer-chat-heading">
              <strong>Lantern &amp; Tide</strong>
              <span><i /> Travel concierge</span>
            </div>
            <button className="customer-chat-close" type="button" onClick={() => setIsOpen(false)} aria-label="Đóng chat">
              <X />
            </button>
            <div className="customer-chat-intro">
              <h2>Plan a trip that feels like yours.</h2>
              <p>Hỏi trợ lý về tour và điểm đến tại Việt Nam.</p>
            </div>
          </header>

          <div className="customer-chat-messages" role="log" aria-label="Tin nhắn trong cuộc trò chuyện">
            {messages.map((item) => (
              <div className={`customer-chat-row is-${item.role}`} key={item.id}>
                {item.role === "assistant" && <span className="customer-chat-avatar" aria-hidden="true"><Bot /></span>}
                <p className="customer-chat-bubble">{item.content}</p>
              </div>
            ))}
            {messages.length === 1 && !isSending && (
              <div className="customer-chat-suggestions">
                {suggestions.map((suggestion) => (
                  <button key={suggestion} type="button" onClick={() => void sendMessage(undefined, suggestion)}>
                    {suggestion}
                  </button>
                ))}
              </div>
            )}
            {isSending && (
              <div className="customer-chat-row is-assistant">
                <span className="customer-chat-avatar" aria-hidden="true"><Bot /></span>
                <p className="customer-chat-bubble customer-chat-typing"><i /><i /><i /><span className="sr-only">Đang trả lời</span></p>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="customer-chat-compose-area">
            {error && <p className="customer-chat-error" role="alert">{error}</p>}
            <form className="customer-chat-compose" onSubmit={(event) => void sendMessage(event)}>
              <input
                aria-label="Tin nhắn cho trợ lý"
                autoComplete="off"
                maxLength={2000}
                placeholder="Nhắn tin cho trợ lý..."
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                disabled={isSending}
              />
              <button type="submit" aria-label="Gửi tin nhắn" disabled={isSending || !message.trim()}>
                <Send />
              </button>
            </form>
            <div className="customer-chat-footer">
              <span>AI assistant · Không thay thế xác nhận từ đối tác</span>
              <button type="button" onClick={startNewConversation}>Cuộc trò chuyện mới</button>
            </div>
          </div>
        </section>
      )}

      <button
        className="customer-chat-launcher"
        type="button"
        aria-label="Mở chat với Lantern & Tide"
        aria-expanded={isOpen}
        onClick={() => setIsOpen(true)}
      >
        <MessageCircle />
        <i aria-hidden="true"><Sparkles /></i>
      </button>
    </div>
  )
}
