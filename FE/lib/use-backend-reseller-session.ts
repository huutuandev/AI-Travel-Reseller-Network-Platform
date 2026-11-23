"use client"

import { useSyncExternalStore } from "react"
import { getBackendAccessToken } from "@/lib/backend-api"

function subscribe(callback: () => void) {
  if (typeof window === "undefined") return () => undefined
  window.addEventListener("wayfarer:session-change", callback)
  window.addEventListener("storage", callback)
  return () => {
    window.removeEventListener("wayfarer:session-change", callback)
    window.removeEventListener("storage", callback)
  }
}

function getSnapshot() {
  return Boolean(getBackendAccessToken())
}

export function useBackendResellerSession() {
  return useSyncExternalStore(subscribe, getSnapshot, () => false)
}