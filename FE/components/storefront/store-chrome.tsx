"use client"

import { useEffect, useState, useSyncExternalStore } from "react"
import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Menu, Search, X } from "lucide-react"

function subscribeToLocationHash(onChange: () => void) {
  window.addEventListener("hashchange", onChange)
  return () => window.removeEventListener("hashchange", onChange)
}

function getLocationHash() {
  return window.location.hash
}

export function StoreNav({ current }: { current: string }) {
  const router = useRouter()
  const [search, setSearch] = useState("")
  const [menuOpen, setMenuOpen] = useState(false)
  const [clickedItem, setClickedItem] = useState<string | null>(null)
  const currentHash = useSyncExternalStore(subscribeToLocationHash, getLocationHash, () => "")
  const activeItem = clickedItem ?? ({ "#destinations": "Destinations", "#why-lantern": "Why us" } as Record<string, string>)[currentHash] ?? current
  useEffect(() => {
    const clearClickedItem = () => setClickedItem(null)
    window.addEventListener("hashchange", clearClickedItem)
    return () => window.removeEventListener("hashchange", clearClickedItem)
  }, [])
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClickedItem("Destinations")
    router.push(`/storefront?search=${encodeURIComponent(search.trim())}#destinations`)
    setMenuOpen(false)
  }

  return <header className="site-nav customer-site-header">
    <Link className="store-brand" href="/storefront" aria-label="Lantern and Tide Travel home" onClick={() => setClickedItem("Home")}><span className="brand-seal"><span className="brand-seal-orbit"><svg viewBox="0 0 40 40" aria-hidden="true"><circle cx="20" cy="20" r="17"/><circle cx="20" cy="20" r="12"/><path className="brand-rose" d="M20 5.5 23 17l11.5 3L23 23l-3 11.5L17 23 5.5 20 17 17 20 5.5Z"/><path className="brand-rose-gold" d="m20 5.5 3 11.5-3 3-3-3 3-11.5Zm14.5 14.5L23 23l-3-3 3-3 11.5 3Z"/><circle className="brand-rose-center" cx="20" cy="20" r="2.4"/></svg><i aria-hidden="true"/></span></span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link>
    <nav className={menuOpen ? "is-open" : ""} aria-label="Main navigation"><Link className={activeItem === "Home" ? "nav-current" : ""} href="/storefront" onClick={() => { setClickedItem("Home"); setMenuOpen(false) }}>Home</Link><a className={activeItem === "Destinations" ? "nav-current" : ""} href="/storefront#destinations" onClick={() => { setClickedItem("Destinations"); setMenuOpen(false) }}>Destinations</a><a className={activeItem === "Why us" ? "nav-current" : ""} href="/storefront#why-lantern" onClick={() => { setClickedItem("Why us"); setMenuOpen(false) }}>Why us</a><form className="store-nav-search mobile-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Where would you like to go?" aria-label="Search journeys"/><button type="submit">Search</button></form></nav>
    <form className="store-nav-search desktop-search" role="search" onSubmit={submitSearch}><Search aria-hidden="true"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search journeys" aria-label="Search journeys"/><button type="submit">Search</button></form>
    <div className="store-nav-actions"><a className="nav-cta" href="/storefront#destinations" onClick={() => setClickedItem("Destinations")}>Explore tours <b aria-hidden="true">↗</b></a><button className="store-menu-toggle" type="button" onClick={() => setMenuOpen((open) => !open)} aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen}>{menuOpen ? <X aria-hidden="true"/> : <Menu aria-hidden="true"/>}</button></div>
  </header>
}

export function StoreFooter() {
  return <footer className="store-footer"><Link className="store-brand" href="/storefront"><span className="brand-seal">L<span>&amp;</span>T</span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link><div className="footer-links"><Link href="/storefront#journeys">Journeys</Link><Link href="/storefront#approach">Our approach</Link><Link href="/">Platform preview</Link></div><div className="footer-legal"><span>TRAVEL THOUGHTFULLY. LEAVE LIGHTLY.</span><small>Sample tenant storefront · Prototype only</small></div></footer>
}
