"use client"

import Image from "next/image"
import Link from "next/link"
import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { CalendarDays, MapPin, Search, Sparkles } from "lucide-react"
import { StoreFooter, StoreNav } from "@/components/storefront/store-chrome"
import { TravelProductCard } from "@/components/storefront/travel-product-card"
import { storefrontJourneys as journeys } from "@/lib/storefront-data"

export default function StorefrontHome() {
  return <Suspense fallback={<main className="storefront-loading">Opening the collection…</main>}><StorefrontHomeContent /></Suspense>
}

function StorefrontHomeContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [slide, setSlide] = useState(0)
  const [category, setCategory] = useState("all")
  const [destination, setDestination] = useState("all")
  const [duration, setDuration] = useState("all")
  const [budget, setBudget] = useState("all")
  const query = (searchParams.get("search") ?? "").trim().toLowerCase()
  const featured = journeys[slide]
  const visibleJourneys = journeys.filter((journey) => {
    const matchesSearch = !query || `${journey.title} ${journey.short} ${journey.type}`.toLowerCase().includes(query)
    const matchesCategory = category === "all" || (category === "coast" ? /sea|island/i.test(journey.type) : /culture|boutique/i.test(journey.type))
    const matchesDestination = destination === "all" || journey.key === destination
    const matchesDuration = duration === "all" || (duration === "weekend" ? journey.key === "halong" : journey.key !== "halong")
    const matchesBudget = budget === "all" || (budget === "under-350" ? journey.price < 350 : journey.price >= 350)
    return matchesSearch && matchesCategory && matchesDestination && matchesDuration && matchesBudget
  })
  const moveSlide = (direction: number) => setSlide((current) => (current + direction + journeys.length) % journeys.length)

  return <div className="store-home">
    <StoreNav current="Home" returnTo="/storefront" home />
    <main>
      <section className="home-hero" aria-label="Discover Vietnam">
        <Image className={`home-hero-image destination-photo destination-photo-${featured.key}`} src={featured.image} alt={`${featured.short}, Vietnam`} fill priority sizes="100vw" />
        <div className="home-hero-overlay" />
        <div className="home-hero-content">
          <span className="home-eyebrow"><i /> LANTERN &amp; TIDE · VIETNAM</span>
          <h1>Find your kind<br />of <em>somewhere.</em></h1>
          <p>Remarkable places, thoughtful local stays, and journeys made to feel like your own.</p>
          <div className="home-hero-controls">
            <button type="button" onClick={() => moveSlide(-1)} aria-label="Previous destination">←</button>
            <div>{journeys.map((journey, index) => <button key={journey.key} type="button" onClick={() => setSlide(index)} className={slide === index ? "is-active" : ""} aria-label={`Show ${journey.short}`} aria-current={slide === index ? "true" : undefined} />)}</div>
            <button type="button" onClick={() => moveSlide(1)} aria-label="Next destination">→</button>
            <span>{String(slide + 1).padStart(2, "0")} <i>/</i> {String(journeys.length).padStart(2, "0")}</span>
          </div>
        </div>
        <Link className="home-feature-link" href={`/storefront/product-detail?product=${featured.key}`}>
          <span>FEATURED JOURNEY</span><strong>{featured.feature}</strong><small>{featured.duration} · From <b>${featured.price.toLocaleString("en-US")}</b> / guest</small><i aria-hidden="true">↗</i>
        </Link>
        <form className="home-search" onSubmit={(event) => { event.preventDefault(); document.getElementById("destinations")?.scrollIntoView({ behavior: "smooth" }) }}>
          <label><MapPin /><span>DESTINATION<select value={destination} onChange={(event) => setDestination(event.target.value)}><option value="all">Anywhere in Vietnam</option>{journeys.map((journey) => <option key={journey.key} value={journey.key}>{journey.short}</option>)}</select></span></label>
          <label><Sparkles /><span>TRIP TYPE<select value={category} onChange={(event) => setCategory(event.target.value)}><option value="all">Any experience</option><option value="coast">Coast &amp; islands</option><option value="culture">Culture &amp; stays</option></select></span></label>
          <label><CalendarDays /><span>DURATION<select value={duration} onChange={(event) => setDuration(event.target.value)}><option value="all">Any duration</option><option value="weekend">A weekend</option><option value="long">3+ nights</option></select></span></label>
          <label><span className="home-currency">$</span><span>YOUR BUDGET<select value={budget} onChange={(event) => setBudget(event.target.value)}><option value="all">Any budget</option><option value="under-350">Under $350</option><option value="over-350">$350 and above</option></select></span></label>
          <button type="submit"><Search /> Search</button>
        </form>
      </section>

      <section className="home-destinations" id="destinations">
        <div className="home-section-heading"><div><span className="home-section-kicker">A FEW PLACES TO BEGIN</span><h2>Popular destinations</h2><p>Small, considered journeys across Vietnam.</p></div><a href="#why-lantern">Why travel with us <span>↗</span></a></div>
        {query && <p className="home-search-result">Showing journeys matching “{searchParams.get("search") ?? ""}”</p>}
        <div className="home-destination-grid">{visibleJourneys.map((journey) => <TravelProductCard key={journey.key} journey={journey} />)}</div>
        {visibleJourneys.length === 0 && <div className="home-no-results"><span>No journeys match these filters yet.</span><button type="button" onClick={() => { setCategory("all"); setDestination("all"); setDuration("all"); setBudget("all"); router.push("/storefront") }}>Clear filters</button></div>}
        <p className="home-sample-note">Illustrative sample journeys and prices. Availability and final details are confirmed by the travel partner.</p>
      </section>

      <section className="home-why" id="why-lantern"><span className="home-section-kicker">THE LANTERN &amp; TIDE DIFFERENCE</span><h2>Travel well.<br /><em>Feel more.</em></h2><div className="home-why-items"><article><span>01</span><div><h3>Chosen with care</h3><p>A focused collection of stays and experiences, curated around place and character.</p></div></article><article><span>02</span><div><h3>Rooted in Vietnam</h3><p>Local knowledge shapes the small details that make a trip feel personal.</p></div></article><article><span>03</span><div><h3>Room to make it yours</h3><p>Start with a thoughtful plan, then leave space for the moments you discover.</p></div></article></div></section>
    </main>
    <StoreFooter />
  </div>
}
