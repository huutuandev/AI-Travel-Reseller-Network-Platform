"use client"

import Link from "next/link"
import { useState } from "react"
import { CheckCircle2, Compass } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function SignupPage() {
  const [submitted, setSubmitted] = useState(false)
  return <main className="flex min-h-screen flex-col bg-[#fbfcfa] px-5 py-7"><Link href="/" className="mx-auto flex w-full max-w-6xl items-center gap-2.5 font-semibold"><span className="flex size-9 items-center justify-center rounded-xl bg-teal-800 text-white"><Compass className="size-5"/></span>Wayfarer Network</Link><section className="mx-auto my-auto w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-7 shadow-sm sm:p-9"><p className="text-sm font-medium text-teal-800">Start growing together</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Explore the Wayfarer preview</h1><p className="mt-3 text-sm leading-6 text-slate-500">Tell us a little about your travel business. This demo form keeps your details in this browser session only.</p>{submitted?<div role="status" className="mt-7 rounded-xl bg-emerald-50 p-5 text-emerald-900"><CheckCircle2 className="size-6"/><p className="mt-3 font-semibold">Thanks for your interest.</p><p className="mt-1 text-sm">This preview does not submit information to a server.</p><Button variant="outline" className="mt-4 bg-white" onClick={()=>setSubmitted(false)}>Edit details</Button></div>:<form className="mt-7 space-y-4" onSubmit={(event)=>{event.preventDefault();setSubmitted(true)}}><div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="name">Your name</Label><Input id="name" autoComplete="name" placeholder="Avery Morgan" required/></div><div className="space-y-2"><Label htmlFor="business">Business name</Label><Input id="business" autoComplete="organization" placeholder="Your travel brand" required/></div></div><div className="space-y-2"><Label htmlFor="email">Work email</Label><Input id="email" type="email" autoComplete="email" placeholder="you@yourtravelbrand.com" required/></div><Button type="submit" className="h-11 w-full bg-teal-800 hover:bg-teal-900">Request a product preview</Button><p className="text-center text-xs text-slate-400">Sample interface · No account will be created</p></form>}<p className="mt-6 text-center text-sm text-slate-500">Already have access? <Link href="/login" className="font-medium text-teal-800 hover:underline">Sign in</Link></p></section></main>
}
