"use client"

import Link from "next/link"
import { Suspense, useState } from "react"
import { useSearchParams } from "next/navigation"
import { CustomerAuthLayout } from "@/components/storefront/customer-auth-layout"

export default function CustomerForgotPasswordPage() {
  return <Suspense fallback={<main className="customer-auth-loading">Opening password help…</main>}><CustomerForgotPasswordForm/></Suspense>
}

function CustomerForgotPasswordForm() {
  const searchParams = useSearchParams()
  const [email, setEmail] = useState("")
  const [submitted, setSubmitted] = useState(false)
  const requestedNext = searchParams.get("next")
  const next = requestedNext?.startsWith("/storefront") ? requestedNext : "/storefront"
  return <CustomerAuthLayout eyebrow="ACCOUNT HELP" title="Let’s find your way back." description="Enter the email you use for Lantern & Tide customer sign in.">
    <form className="customer-auth-form" onSubmit={(event) => { event.preventDefault(); setSubmitted(true) }}>
      <label htmlFor="customer-email">Email address</label><input id="customer-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" required/>
      <button className="store-button customer-auth-submit" type="submit">Continue <b aria-hidden="true">→</b></button>
      {submitted && <p className="customer-auth-message is-demo" role="status">This preview does not send email or change passwords. No reset request was sent.</p>}
    </form>
    <p className="customer-auth-switch"><Link href={`/login?audience=customer&next=${encodeURIComponent(next)}`}>← Return to customer sign in</Link></p>
  </CustomerAuthLayout>
}
