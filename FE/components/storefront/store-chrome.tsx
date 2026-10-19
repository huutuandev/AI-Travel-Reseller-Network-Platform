"use client"

import { useMemo, useState, useSyncExternalStore } from "react"
import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { LogOut, Menu, Search, UserRound, X } from "lucide-react"
import { clearCustomerSession, getCustomerSession, getCustomerSessionSnapshot, subscribeCustomerSession } from "@/lib/customer-auth"

function StoreAccountActions({ returnTo, compact = false }: { returnTo: string; compact?: boolean }) {
  const snapshot = useSyncExternalStore(subscribeCustomerSession, getCustomerSessionSnapshot, () => "__hydrating__")
  const customer = useMemo(() => snapshot && !snapshot.startsWith("__") ? getCustomerSession() : null, [snapshot])
  const loginHref = `/login?audience=customer&next=${encodeURIComponent(returnTo)}`

  if (snapshot === "__hydrating__") return <div className="store-account-placeholder" aria-hidden="true" />
  if (customer) return <div className={`store-account-actions ${compact ? "is-compact" : ""}`}>
    <span className="store-customer-name"><UserRound aria-hidden="true"/><span>{customer.name}</span></span>
    <button className="store-account-logout" type="button" onClick={clearCustomerSession} aria-label="Sign out"><LogOut aria-hidden="true"/><span>Sign out</span></button>
  </div>

  return <div className={`store-account-actions ${compact ? "is-compact" : ""}`}>
    <Link className="store-login-link" href={loginHref}>Log in</Link>
    <Link className="store-signup-link" href="/storefront/signup">Sign up</Link>
  </div>
}

export function StoreNav({ current, returnTo = "/storefront", home = false, detail = false }: { current: string; returnTo?: string; home?: boolean; detail?: boolean }) {
  const router = useRouter()
  const [search, setSearch] = useState("")
  const [menuOpen, setMenuOpen] = useState(false)
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    router.push(`/storefront?search=${encodeURIComponent(search.trim())}#journeys`)
    setMenuOpen(false)
  }

  return <header className="site-nav">
    <Link className="store-brand" href="/storefront" aria-label="Lantern and Tide Travel home"><span className="brand-seal">L<span>&amp;</span>T</span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link>
    <nav className={menuOpen ? "is-open" : ""} aria-label="Main navigation">{home ? <><Link className={current === "Home" ? "nav-current" : ""} href="/storefront" onClick={() => setMenuOpen(false)}>Home</Link><Link href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Destinations</Link><Link href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Tours</Link><Link href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>Why us</Link></> : detail ? <><Link href="/storefront" onClick={() => setMenuOpen(false)}>Home</Link><Link href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>About</Link><Link href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Destination</Link><Link className={current === "The collection" ? "nav-current" : ""} href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Tour</Link><Link href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>Journal</Link></> : <><Link className={current === "The collection" ? "nav-current" : ""} href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Explore journeys</Link><Link href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>Our approach</Link><Link href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>Field notes</Link></>}<form className="store-nav-search mobile-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Where would you like to go?" aria-label="Search journeys"/><button type="submit">Search</button></form><div className="store-mobile-account"><StoreAccountActions returnTo={returnTo}/></div></nav>
    <form className="store-nav-search desktop-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search journeys" aria-label="Search journeys"/><button type="submit">Search</button></form>
    <div className="store-nav-actions"><StoreAccountActions returnTo={returnTo}/><Link className="nav-cta" href="/storefront#destinations">{home ? "Explore tours" : "Plan your escape"} <b aria-hidden="true">↗</b></Link><button className="store-menu-toggle" type="button" onClick={() => setMenuOpen((open) => !open)} aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen}>{menuOpen ? <X aria-hidden="true"/> : <Menu aria-hidden="true"/>}</button></div>
  </header>
}

export function StoreFooter() {
  return <footer className="store-footer"><Link className="store-brand" href="/storefront"><span className="brand-seal">L<span>&amp;</span>T</span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link><div className="footer-links"><Link href="/storefront#journeys">Journeys</Link><Link href="/storefront#approach">Our approach</Link><Link href="/login?audience=customer&amp;next=%2Fstorefront">Customer sign in</Link><Link href="/">Platform preview</Link></div><div className="footer-legal"><span>TRAVEL THOUGHTFULLY. LEAVE LIGHTLY.</span><small>Sample tenant storefront · Prototype only</small></div></footer>
}

export function CheckoutAccountActions({ returnTo }: { returnTo: string }) {
  return <StoreAccountActions returnTo={returnTo} compact />
}
