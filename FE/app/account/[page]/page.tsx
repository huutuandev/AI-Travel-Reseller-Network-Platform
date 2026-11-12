import { notFound } from "next/navigation"
import { AccountSpecialPage } from "@/components/wireframe-special-pages"

export function generateStaticParams() {
  return [{ page: "profile" }, { page: "notifications" }]
}

export default async function AccountPage({ params }: { params: Promise<{ page: string }> }) {
  const { page } = await params
  if (page !== "profile" && page !== "notifications") notFound()
  return <AccountSpecialPage page={page} />
}
