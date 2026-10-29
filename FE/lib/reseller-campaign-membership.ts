"use client"

import { createContext, createElement, useContext, useState, type ReactNode } from "react"

export type CampaignMembership = {
  status: "Pending" | "Active"
  joinedAt: string
}

export type CampaignMembershipMap = Record<string, CampaignMembership>

const defaultMemberships: CampaignMembershipMap = {
  "Ha Long Bay Weekend Escape": { status: "Active", joinedAt: "10 Oct 2026" },
  "Phu Quoc Island Escape": { status: "Pending", joinedAt: "10 Oct 2026" },
}

type MembershipContextValue = {
  memberships: CampaignMembershipMap
  requestToJoin: (campaignName: string) => void
}

const MembershipContext = createContext<MembershipContextValue | null>(null)

export function ResellerCampaignMembershipProvider({ children }: { children: ReactNode }) {
  const [memberships, setMemberships] = useState<CampaignMembershipMap>(defaultMemberships)

  function requestToJoin(campaignName: string) {
    setMemberships((current) => {
      if (current[campaignName]) return current
      const joinedAt = new Intl.DateTimeFormat("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }).format(new Date())
      return { ...current, [campaignName]: { status: "Pending", joinedAt } }
    })
  }

  return createElement(MembershipContext.Provider, { value: { memberships, requestToJoin } }, children)
}

export function useResellerCampaignMembership(enabled = true) {
  const context = useContext(MembershipContext)
  return {
    memberships: enabled ? context?.memberships ?? defaultMemberships : defaultMemberships,
    requestToJoin: enabled && context ? context.requestToJoin : () => {},
  }
}
