"use client"

import { useState } from "react"
import { LockKeyhole, LockOpen } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"

type ResellerStatus = "Active" | "Pending" | "Blocked"

const statusStyles: Record<ResellerStatus, string> = {
  Active: "border-emerald-200 bg-emerald-50 text-emerald-800",
  Pending: "border-amber-200 bg-amber-50 text-amber-900",
  Blocked: "border-slate-200 bg-slate-100 text-slate-700",
}

export function ResellerStatusManager({ initialStatus }: { initialStatus: string }) {
  const [status, setStatus] = useState<ResellerStatus>(initialStatus as ResellerStatus)
  const [feedback, setFeedback] = useState("")

  function updateStatus(nextStatus: ResellerStatus, message: string) {
    setStatus(nextStatus)
    setFeedback(message)
  }

  return (
    <div className="flex flex-col items-end gap-2">
      <Badge variant="outline" className={statusStyles[status] ?? "border-slate-200 bg-slate-100 text-slate-700"}>{status}</Badge>
      {status === "Active" && (
        <Button type="button" variant="destructive" size="sm" onClick={() => updateStatus("Blocked", "Reseller blocked.")}>
          <LockKeyhole className="size-4" />Block reseller
        </Button>
      )}

      {status === "Blocked" && (
        <Button type="button" variant="outline" size="sm" className="border-emerald-200 text-emerald-800 hover:bg-emerald-50" onClick={() => updateStatus("Active", "Reseller unblocked.")}>
          <LockOpen className="size-4" />Unblock reseller
        </Button>
      )}
      {feedback && <span role="status" className="text-right text-xs text-slate-500">{feedback}</span>}
    </div>
  )
}
