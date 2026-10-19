import { redirect } from "next/navigation"

export default async function CustomerLoginRedirect({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next: requestedNext } = await searchParams
  const next = requestedNext?.startsWith("/storefront") && !requestedNext.startsWith("//") ? requestedNext : "/storefront"
  redirect(`/login?audience=customer&next=${encodeURIComponent(next)}`)
}
