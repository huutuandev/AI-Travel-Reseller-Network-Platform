"use client"

import Link from "next/link"
import { useState } from "react"
import { ArrowLeft, Compass, Mail } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  return <main className="flex min-h-screen flex-col bg-[#fbfcfa] px-5 py-7"><Link href="/" className="mx-auto flex w-full max-w-6xl items-center gap-2.5 font-semibold"><span className="flex size-9 items-center justify-center rounded-xl bg-teal-800 text-white"><Compass className="size-5"/></span>Wayfarer Network</Link><section className="mx-auto my-auto w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-sm sm:p-9"><span className="flex size-12 items-center justify-center rounded-xl bg-teal-50 text-teal-800"><Mail className="size-5"/></span><p className="mt-6 text-sm font-medium text-teal-800">Account access</p><h1 className="mt-2 text-2xl font-semibold tracking-tight">Forgot your password?</h1><p className="mt-2 text-sm leading-6 text-slate-500">Enter your email address and we’ll show you how a reset request works in this preview.</p><form className="mt-6 space-y-4" onSubmit={(event)=>{event.preventDefault();setSubmitted(true)}}><div className="space-y-2"><Label htmlFor="email">Email address</Label><Input id="email" type="email" autoComplete="email" placeholder="you@yourtravelbrand.com" required/></div><Button type="submit" className="h-11 w-full bg-teal-800 hover:bg-teal-900">Send reset instructions</Button>{submitted&&<p role="status" className="rounded-lg bg-amber-50 p-3 text-sm leading-5 text-amber-900">This preview does not send email. No reset request was sent.</p>}</form><Link href="/login" className="mt-6 inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-teal-800"><ArrowLeft className="size-4"/>Back to sign in</Link></section></main>
}
