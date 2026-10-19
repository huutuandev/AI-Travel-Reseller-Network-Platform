"use client"

import Link from "next/link"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { Compass, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { authenticate, MOCK_USERS, ROLE_HOME, saveDemoSession } from "@/lib/mock-auth"

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [message, setMessage] = useState("")

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const user = authenticate(email, password)
    if (!user) {
      setMessage("Email or password is incorrect. Choose a demo account below or check your details.")
      return
    }
    saveDemoSession(user)
    router.push(ROLE_HOME[user.role])
  }

  function fillAccount(accountEmail: string, accountPassword: string) {
    setEmail(accountEmail)
    setPassword(accountPassword)
    setMessage("")
  }
  return <main className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_0.95fr]">
    <section className="relative hidden overflow-hidden bg-[#eaf2ed] p-12 lg:flex lg:flex-col lg:justify-between xl:p-16">
      <Link href="/" className="relative z-10 flex w-fit items-center gap-2.5 font-semibold"><span className="flex size-9 items-center justify-center rounded-xl bg-teal-800 text-white"><Compass className="size-5"/></span>Wayfarer Network</Link>
      <div className="relative z-10 max-w-xl"><p className="text-xs font-semibold uppercase tracking-[.17em] text-teal-800">Travel further, together</p><h1 className="mt-5 text-5xl font-semibold leading-[1.08] tracking-tight">Good journeys grow through good partners.</h1><p className="mt-5 max-w-md text-lg leading-8 text-slate-600">Bring thoughtful travel experiences to the people who will love them.</p><div className="mt-9 flex gap-2"><span className="h-1.5 w-8 rounded-full bg-teal-800"/><span className="h-1.5 w-4 rounded-full bg-teal-800/25"/><span className="h-1.5 w-4 rounded-full bg-teal-800/25"/></div></div>
      <div className="relative z-10 rounded-2xl border border-white/80 bg-white/75 p-5 shadow-sm backdrop-blur"><div className="flex items-center gap-3"><span className="flex size-10 items-center justify-center rounded-xl bg-amber-100 text-amber-900"><Compass className="size-5"/></span><span><strong className="block text-sm">A network built for discovery</strong><small className="text-slate-500">Campaigns, referrals, and partner growth in one place</small></span></div></div>
    </section>
    <section className="flex min-h-screen flex-col px-6 py-7 sm:px-12 lg:px-16"><Link href="/" className="flex w-fit items-center gap-2 font-semibold lg:hidden"><span className="flex size-9 items-center justify-center rounded-xl bg-teal-800 text-white"><Compass className="size-5"/></span>Wayfarer Network</Link><div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-12"><p className="text-sm font-medium text-teal-800">Welcome back</p><h2 className="mt-2 text-3xl font-semibold tracking-tight">Sign in to your workspace</h2><p className="mt-2 text-sm text-slate-500">Pick up where you left off with your travel network.</p>
      <form className="mt-8 space-y-5" onSubmit={handleSubmit}><div className="space-y-2"><Label htmlFor="email">Email address</Label><Input id="email" type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={(event)=>{setEmail(event.target.value);setMessage("")}} required/></div><div className="space-y-2"><div className="flex items-center justify-between"><Label htmlFor="password">Password</Label><Link href="/forgot-password" className="text-sm font-medium text-teal-800 hover:underline">Forgot password?</Link></div><div className="relative"><Input id="password" type={showPassword?"text":"password"} autoComplete="current-password" placeholder="Enter your password" value={password} onChange={(event)=>{setPassword(event.target.value);setMessage("")}} className="pr-11" required/><button type="button" onClick={()=>setShowPassword(!showPassword)} aria-label={showPassword?"Hide password":"Show password"} className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-slate-400 hover:text-slate-700">{showPassword?<EyeOff className="size-4"/>:<Eye className="size-4"/>}</button></div></div><div className="flex items-center gap-2 text-sm text-slate-600"><input id="remember" type="checkbox" className="size-4 rounded border-slate-300 accent-teal-800"/><Label htmlFor="remember" className="font-normal">Remember me</Label></div><Button type="submit" className="h-11 w-full bg-teal-800 hover:bg-teal-900">Sign in</Button>{message&&<p role="alert" className="rounded-lg bg-red-50 p-3 text-sm leading-5 text-red-800">{message}</p>}</form>
      <div className="mt-7 rounded-xl border border-slate-200 bg-slate-50 p-4"><div className="flex items-start justify-between gap-3"><div><p className="text-sm font-semibold">Development demo accounts</p><p className="mt-1 text-xs leading-5 text-slate-500">Choose a role to fill its local-only credentials.</p></div><span className="rounded-full bg-amber-100 px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-amber-900">Demo only</span></div><div className="mt-3 grid grid-cols-2 gap-2">{MOCK_USERS.map((user)=><button key={user.id} type="button" onClick={()=>fillAccount(user.email,user.password)} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-left text-xs font-medium text-slate-700 hover:border-teal-300 hover:text-teal-900">{user.role==="platform-admin"?"Platform admin":user.role==="tenant"?"Tenant":user.role==="reseller"?"Reseller":"Supplier"}</button>)}</div></div>
      <p className="mt-5 text-center text-sm text-slate-500">New to Wayfarer? <Link href="/signup" className="font-medium text-teal-800 hover:underline">Explore the preview</Link></p>
      </div><p className="text-center text-xs text-slate-400">Product preview · Sample data only</p></section>
  </main>
}
