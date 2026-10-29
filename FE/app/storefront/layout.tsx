import "./storefront.css"
import type { Metadata } from "next"
import { CustomerChatWidget } from "@/components/storefront/customer-chat-widget"

export const metadata: Metadata = {
  title: "Lantern & Tide Travel | Considered journeys in Vietnam",
  description: "Explore thoughtful stays and locally hosted experiences with Lantern & Tide Travel.",
}

export default function StorefrontLayout({ children }: { children: React.ReactNode }) {
  return <div className="store-page">{children}<CustomerChatWidget /></div>
}
