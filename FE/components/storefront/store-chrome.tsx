"use client"

import { useMemo, useState, useSyncExternalStore } from "react"
import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { ChevronDown, LogOut, Menu, Search, UserRound, X } from "lucide-react"
import { clearCustomerSession, getCustomerSession, getCustomerSessionSnapshot, subscribeCustomerSession } from "@/lib/customer-auth"

function StoreAccountActions({ returnTo, compact = false }: { returnTo: string; compact?: boolean }) {
  const snapshot = useSyncExternalStore(subscribeCustomerSession, getCustomerSessionSnapshot, () => "__hydrating__")
  const customer = useMemo(() => snapshot && !snapshot.startsWith("__") ? getCustomerSession() : null, [snapshot])
  const loginHref = `/login?audience=customer&next=${encodeURIComponent(returnTo)}`

  if (snapshot === "__hydrating__") return <div className="store-account-placeholder" aria-hidden="true" />
  if (customer) return <div className={`store-account-actions ${compact ? "is-compact" : ""}`}>
    <details className="store-account-menu">
      <summary className="store-customer-name"><span className="store-customer-avatar"><UserRound aria-hidden="true"/></span><span className="store-customer-label">{customer.name}</span><ChevronDown className="store-account-chevron" aria-hidden="true"/></summary>
      <div className="store-account-dropdown"><span className="store-account-dropdown-label">Signed in as</span><strong>{customer.name}</strong><button className="store-account-logout" type="button" onClick={clearCustomerSession}><LogOut aria-hidden="true"/><span>Sign out</span></button></div>
    </details>
  </div>

  return <div className={`store-account-actions ${compact ? "is-compact" : ""}`}>
    <Link className="store-login-link" href={loginHref}>Log in</Link>
    <Link className="store-signup-link" href="/storefront/signup">Sign up</Link>
  </div>
}

export function StoreNav({ current, returnTo = "/storefront" }: { current: string; returnTo?: string }) {
  const router = useRouter()
  const [search, setSearch] = useState("")
  const [menuOpen, setMenuOpen] = useState(false)
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    router.push(`/storefront?search=${encodeURIComponent(search.trim())}#destinations`)
    setMenuOpen(false)
  }

  return <header className="site-nav customer-site-header">
    <Link className="store-brand" href="/storefront" aria-label="Lantern and Tide Travel home"><span className="brand-seal"><span className="brand-seal-orbit"><svg viewBox="0 0 40 40" aria-hidden="true"><circle cx="20" cy="20" r="17"/><circle cx="20" cy="20" r="12"/><path className="brand-rose" d="M20 5.5 23 17l11.5 3L23 23l-3 11.5L17 23 5.5 20 17 17 20 5.5Z"/><path className="brand-rose-gold" d="m20 5.5 3 11.5-3 3-3-3 3-11.5Zm14.5 14.5L23 23l-3-3 3-3 11.5 3Z"/><circle className="brand-rose-center" cx="20" cy="20" r="2.4"/></svg><i aria-hidden="true"/></span></span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link>
    <nav className={menuOpen ? "is-open" : ""} aria-label="Main navigation"><Link className={current === "Home" ? "nav-current" : ""} href="/storefront" onClick={() => setMenuOpen(false)}>Home</Link><Link className={current === "Destinations" ? "nav-current" : ""} href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Destinations</Link><Link className={current === "Tours" ? "nav-current" : ""} href="/storefront#destinations" onClick={() => setMenuOpen(false)}>Tours</Link><Link className={current === "Why us" ? "nav-current" : ""} href="/storefront#why-lantern" onClick={() => setMenuOpen(false)}>Why us</Link><form className="store-nav-search mobile-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Where would you like to go?" aria-label="Search journeys"/><button type="submit">Search</button></form><div className="store-mobile-account"><StoreAccountActions returnTo={returnTo}/></div></nav>
    <form className="store-nav-search desktop-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search journeys" aria-label="Search journeys"/><button type="submit">Search</button></form>
    <div className="store-nav-actions"><StoreAccountActions returnTo={returnTo}/><Link className="nav-cta" href="/storefront#destinations">Explore tours <b aria-hidden="true">↗</b></Link><button className="store-menu-toggle" type="button" onClick={() => setMenuOpen((open) => !open)} aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen}>{menuOpen ? <X aria-hidden="true"/> : <Menu aria-hidden="true"/>}</button></div>
  </header>
}

export function StoreFooter() {
  return <footer className="store-footer"><Link className="store-brand" href="/storefront"><span className="brand-seal">L<span>&amp;</span>T</span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link><div className="footer-links"><Link href="/storefront#journeys">Journeys</Link><Link href="/storefront#approach">Our approach</Link><Link href="/login?audience=customer&amp;next=%2Fstorefront">Customer sign in</Link><Link href="/">Platform preview</Link></div><div className="footer-legal"><span>TRAVEL THOUGHTFULLY. LEAVE LIGHTLY.</span><small>Sample tenant storefront · Prototype only</small></div></footer>
}

export function CheckoutAccountActions({ returnTo }: { returnTo: string }) {
  return <StoreAccountActions returnTo={returnTo} compact />
}
