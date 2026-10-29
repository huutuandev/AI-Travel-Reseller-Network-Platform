"use client"

import { useEffect, useRef, useState, type FormEvent } from "react"
import { Bot, LoaderCircle, MessageCircle, Send, Sparkles, X } from "lucide-react"

type ChatMessage = { id: string; role: "user" | "assistant"; content: string }
type StoredChat = { sessionId?: string; messages: ChatMessage[]; updatedAt: number }
type ChatApiResponse = {
  success?: boolean
  message?: string
  data?: { sessionId?: string; reply?: string }
}

const storageKey = "lantern-tide-customer-chat-v1"
const sessionLifetime = 30 * 60 * 1000
const starterPrompts = ["Gợi ý tour phù hợp cho tôi", "Tôi muốn tìm hiểu về các điểm đến"]

export function CustomerChatWidget() {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState("")
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [sessionId, setSessionId] = useState("")
  const [ready, setReady] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState("")
  const endOfMessagesRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      try {
        const raw = window.sessionStorage.getItem(storageKey)
        if (raw) {
          const stored = JSON.parse(raw) as StoredChat
          if (Date.now() - stored.updatedAt < sessionLifetime) {
            setSessionId(stored.sessionId ?? "")
            setMessages(Array.isArray(stored.messages) ? stored.messages : [])
          } else {
            window.sessionStorage.removeItem(storageKey)
          }
        }
      } catch {
        try { window.sessionStorage.removeItem(storageKey) } catch { /* Storage can be unavailable in private contexts. */ }
      }
      setReady(true)
    })
    return () => window.cancelAnimationFrame(frame)
  }, [])

  useEffect(() => {
    if (!ready) return
    if (messages.length === 0 && !sessionId) {
      window.sessionStorage.removeItem(storageKey)
      return
    }
    const stored: StoredChat = { sessionId, messages, updatedAt: Date.now() }
    window.sessionStorage.setItem(storageKey, JSON.stringify(stored))
  }, [messages, ready, sessionId])

  useEffect(() => {
    if (open) endOfMessagesRef.current?.scrollIntoView({ behavior: "smooth", block: "end" })
  }, [messages, open, sending])

  useEffect(() => {
    if (open) inputRef.current?.focus()
  }, [open])

  async function sendMessage(event: FormEvent<HTMLFormElement>, messageOverride?: string) {
    event.preventDefault()
    const message = (messageOverride ?? draft).trim()
    if (!message || sending || message.length > 2000) return

    const userMessage: ChatMessage = { id: crypto.randomUUID(), role: "user", content: message }
    setMessages((current) => [...current, userMessage])
    setDraft("")
    setError("")
    setSending(true)

    try {
      const response = await fetch("/api/v1/chat/message", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ ...(sessionId ? { sessionId } : {}), message }),
      })
      const result = (await response.json()) as ChatApiResponse
      if (!response.ok || result.success === false) {
        throw new Error(result.message || "Mình chưa gửi được tin nhắn. Bạn thử lại nhé.")
      }
      const reply = result.data?.reply?.trim()
      if (!reply) throw new Error("Chatbot chưa trả về nội dung. Bạn thử lại sau nhé.")
      if (result.data?.sessionId) setSessionId(result.data.sessionId)
      setMessages((current) => [...current, { id: crypto.randomUUID(), role: "assistant", content: reply }])
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể kết nối chatbot lúc này. Vui lòng thử lại.")
    } finally {
      setSending(false)
      inputRef.current?.focus()
    }
  }

  function resetChat() {
    setMessages([])
    setSessionId("")
    setDraft("")
    setError("")
    window.sessionStorage.removeItem(storageKey)
    inputRef.current?.focus()
  }

  return (
    <div className="fixed bottom-4 right-4 z-[80] flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      {open && (
        <section
          role="dialog"
          aria-label="Chat"
          className="flex h-[min(620px,calc(100dvh-6.5rem))] w-[min(400px,calc(100vw-2rem))] flex-col overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-[0_24px_80px_-20px_rgba(23,55,78,.38)] animate-in fade-in slide-in-from-bottom-3 duration-200"
        >
          <header className="relative overflow-hidden bg-gradient-to-br from-[#159da8] via-[#347fca] to-[#6175de] px-5 pb-5 pt-5 text-white">
            <div aria-hidden="true" className="absolute -right-8 -top-12 size-40 rounded-full border border-white/10" />
            <div aria-hidden="true" className="absolute -right-2 -top-7 size-28 rounded-full border border-white/10" />
            <div className="relative flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="grid size-11 shrink-0 place-items-center rounded-2xl border border-white/20 bg-white/10 shadow-inner"><Sparkles className="size-5 text-[#f4c98c]" /></span>
                <div><p className="text-sm font-semibold tracking-wide">Lantern &amp; Tide</p><p className="mt-0.5 flex items-center gap-1.5 text-[11px] text-white/75"><i className="size-1.5 rounded-full bg-emerald-300" /> Travel concierge</p></div>
              </div>
              <button type="button" onClick={() => setOpen(false)} aria-label="Đóng cửa sổ chat" className="grid size-9 place-items-center rounded-full text-white/80 transition hover:bg-white/15 hover:text-white"><X className="size-4" /></button>
            </div>
            <div className="relative mt-5"><p className="text-lg font-semibold">Plan a trip that feels like yours.</p><p className="mt-1 text-xs leading-5 text-white/75">Hỏi trợ lý du lịch về điểm đến và hành trình tại Việt Nam.</p></div>
          </header>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto bg-[#f7f9f8] px-4 py-4 sm:px-5" aria-live="polite">
            <div className="flex items-start gap-2.5">
              <span className="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full bg-[#e3f6f7] text-[#27849a]"><Bot className="size-4" /></span>
              <div className="max-w-[85%] rounded-2xl rounded-tl-md border border-slate-100 bg-white px-3.5 py-3 text-sm leading-5 text-slate-700 shadow-sm">Xin chào! Mình có thể giúp bạn tìm hiểu các tour và điểm đến. Bạn đang muốn đi đâu?</div>
            </div>
            {messages.map((message) => (
              <div key={message.id} className={`flex items-start gap-2.5 ${message.role === "user" ? "justify-end" : ""}`}>
                {message.role === "assistant" && <span className="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full bg-[#e3f6f7] text-[#27849a]"><Bot className="size-4" /></span>}
                <p className={`max-w-[85%] whitespace-pre-wrap break-words rounded-2xl px-3.5 py-3 text-sm leading-5 ${message.role === "user" ? "rounded-tr-md bg-gradient-to-br from-[#25b9c2] to-[#4e8fe5] text-white shadow-sm" : "rounded-tl-md border border-slate-100 bg-white text-slate-700 shadow-sm"}`}>{message.content}</p>
              </div>
            ))}
            {messages.length === 0 && !sending && <div className="ml-9 flex flex-wrap gap-2">{starterPrompts.map((prompt) => <button key={prompt} type="button" onClick={() => setDraft(prompt)} className="rounded-full border border-[#d2e8ef] bg-white px-3 py-2 text-left text-[11px] font-medium text-[#337f9e] transition hover:border-[#7fc4d2] hover:bg-[#eff9fb]">{prompt}</button>)}</div>}
            {sending && <div className="flex items-center gap-2.5"><span className="grid size-7 place-items-center rounded-full bg-[#e3f6f7] text-[#27849a]"><Bot className="size-4" /></span><span className="flex items-center gap-2 rounded-2xl rounded-tl-md border border-slate-100 bg-white px-3.5 py-3 text-xs text-slate-500 shadow-sm"><LoaderCircle className="size-3.5 animate-spin" />Đang soạn câu trả lời…</span></div>}
            <div ref={endOfMessagesRef} />
          </div>

          <div className="border-t border-slate-100 bg-white px-3.5 pb-3.5 pt-3 sm:px-4">
            {error && <p role="alert" className="mb-2 rounded-lg bg-rose-50 px-3 py-2 text-xs leading-5 text-rose-800">{error}</p>}
            <form onSubmit={(event) => void sendMessage(event)} className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 transition focus-within:border-[#69bfce] focus-within:ring-2 focus-within:ring-[#d7f2f3]">
              <textarea ref={inputRef} rows={1} maxLength={2000} value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit() } }} placeholder="Nhắn tin cho trợ lý…" aria-label="Tin nhắn của bạn" className="max-h-24 min-h-10 flex-1 resize-none bg-transparent px-2 py-2.5 text-sm leading-5 text-slate-800 outline-none placeholder:text-slate-400" />
              <button type="submit" disabled={sending || !draft.trim()} aria-label="Gửi tin nhắn" className="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-[#25b9c2] to-[#4e8fe5] text-white shadow-sm transition hover:brightness-95 disabled:cursor-not-allowed disabled:opacity-45"><Send className="size-4" /></button>
            </form>
            <div className="mt-2 flex items-center justify-between gap-2 px-1"><span className="text-[10px] text-slate-400">AI assistant · Không thay thế xác nhận từ đối tác</span>{messages.length > 0 && <button type="button" onClick={resetChat} className="text-[10px] font-medium text-slate-500 underline-offset-2 hover:text-slate-800 hover:underline">Cuộc trò chuyện mới</button>}</div>
          </div>
        </section>
      )}

      <button type="button" onClick={() => setOpen((current) => !current)} aria-label={open ? "Đóng chat" : "Mở chat"} aria-expanded={open} className="group relative grid size-[62px] place-items-center overflow-hidden rounded-full bg-gradient-to-br from-[#65d8e8] via-[#55a9f5] to-[#8d78f5] text-white shadow-[0_12px_30px_rgba(76,157,224,.36)] transition duration-200 hover:-translate-y-1 hover:shadow-[0_16px_36px_rgba(76,157,224,.46)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#bdefff]">
        <span aria-hidden="true" className="absolute -right-3 -top-4 size-11 rounded-full bg-white/25 blur-[1px]" />
        <MessageCircle className="relative size-[27px] drop-shadow-sm transition-transform duration-200 group-hover:scale-110" strokeWidth={2} />
        <span className="absolute right-[12px] top-[11px] grid size-[15px] place-items-center rounded-full bg-white text-[#4f9fe9] shadow-sm"><Sparkles className="size-2.5" strokeWidth={2.5} /></span>
      </button>
    </div>
  )
}
