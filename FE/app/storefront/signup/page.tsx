"use client"

import Link from "next/link"
import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Eye, EyeOff } from "lucide-react"
import { CustomerAuthLayout } from "@/components/storefront/customer-auth-layout"
import { registerCustomer } from "@/lib/customer-auth"
import { clearDemoSession } from "@/lib/mock-auth"

export default function CustomerSignupPage() {
  return <Suspense fallback={<main className="customer-auth-loading">Opening account registration…</main>}><CustomerSignupForm/></Suspense>
}

function CustomerSignupForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const requestedNext = searchParams.get("next")
  const next = requestedNext?.startsWith("/storefront") ? requestedNext : "/storefront"
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError("")
    if (password !== confirmPassword) {
      setError("The passwords don’t match yet. Please check both fields.")
      return
    }
    setLoading(true)
    await new Promise((resolve) => window.setTimeout(resolve, 350))
    const result = registerCustomer(name, email, password)
    if (!result.session) {
      setLoading(false)
      setError(result.error ?? "We couldn’t create this demo account. Please try again.")
      return
    }
    clearDemoSession()
    setSuccess(true)
    window.setTimeout(() => router.replace(next), 600)
  }

  return <CustomerAuthLayout eyebrow="A NEW CHAPTER" title="Create your customer account." description="Save a little time next time you find somewhere you love.">
    <form className="customer-auth-form customer-signup-form" onSubmit={submit}>
      <label htmlFor="customer-name">Your name</label>
      <input id="customer-name" autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} placeholder="Alex Morgan" minLength={2} required/>
      <label htmlFor="customer-email">Email address</label>
      <input id="customer-email" type="email" autoComplete="email" value={email} onChange={(event) => { setEmail(event.target.value); setError("") }} placeholder="you@example.com" required/>
      <label htmlFor="customer-password">Password</label>
      <div className="customer-auth-password"><input id="customer-password" type={showPassword ? "text" : "password"} autoComplete="new-password" value={password} onChange={(event) => { setPassword(event.target.value); setError("") }} placeholder="At least 8 characters" minLength={8} required/><button type="button" aria-label={showPassword ? "Hide password" : "Show password"} onClick={() => setShowPassword((shown) => !shown)}>{showPassword ? <EyeOff/> : <Eye/>}</button></div>
      <label htmlFor="customer-confirm-password">Confirm password</label>
      <input id="customer-confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => { setConfirmPassword(event.target.value); setError("") }} placeholder="Enter your password again" minLength={8} required/>
      <button className="store-button customer-auth-submit" type="submit" disabled={loading || success}>{loading ? "Creating your account…" : success ? "Account ready · Opening the collection…" : "Create customer account"}<b aria-hidden="true">→</b></button>
      {error && <p className="customer-auth-message is-error" role="alert">{error}</p>}
      {success && <p className="customer-auth-message is-success" role="status">Your demo customer account is ready.</p>}
      <p className="customer-auth-note">This prototype stores demo account details in this browser only. No server account is created.</p>
    </form>
    <p className="customer-auth-switch">Already have an account? <Link href={`/login?audience=customer&next=${encodeURIComponent(next)}`}>Sign in</Link></p>
  </CustomerAuthLayout>
}
