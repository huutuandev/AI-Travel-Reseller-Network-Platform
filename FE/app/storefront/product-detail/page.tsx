"use client"

import Link from "next/link"
import Image from "next/image"
import { Suspense } from "react"
import { useSearchParams } from "next/navigation"
import { ArrowLeft, ArrowUpRight, CalendarDays, Check, Clock3, MapPin, Tag } from "lucide-react"
import { StoreFooter, StoreNav } from "@/components/storefront/store-chrome"
import { getJourney } from "@/lib/storefront-data"

export default function ProductDetailPage() {
  return <Suspense fallback={<main className="product-detail">Loading journey…</main>}><ProductDetailContent /></Suspense>
}

function ProductDetailContent() {
  const searchParams = useSearchParams()
  const item = getJourney(searchParams.get("product"))
  const checkout = `/storefront/checkout?product=${item.key}`

  return <div className="store-detail-page">
    <StoreNav current="The collection" returnTo={`/storefront/product-detail?product=${item.key}`} detail />
    <main className="product-detail">
      <Link className="detail-back-link" href="/storefront#destinations"><ArrowLeft aria-hidden="true" /> Back to journeys</Link>
      <header className="detail-page-heading">
        <div><span className="detail-location">{item.location}</span><h1>{item.title}</h1><p>{item.description}</p></div>
        <span className="detail-number">JOURNEY <b>{item.number}</b></span>
      </header>
      <div className="detail-meta-row"><span><Clock3 />{item.nights.toLowerCase()}</span><span><MapPin />{item.short}, Vietnam</span><span><Tag />{item.type.toLowerCase()}</span></div>

      <section className="detail-purchase-grid" aria-label="Journey overview and booking">
        <div className="detail-gallery" aria-label={`${item.short} destination gallery`}>
          <div className="detail-gallery-main"><Image className={`destination-photo destination-photo-${item.key}`} src={item.image} alt={`${item.short} landscape, Vietnam`} fill priority sizes="(max-width: 700px) 100vw, 44vw" /><span className="gallery-location"><MapPin aria-hidden="true" />{item.region}</span><div className="visual-caption"><span className="visual-pin">LT</span><span>A JOURNEY THROUGH {item.short.toUpperCase()}</span></div></div>
          <div className="detail-gallery-aside"><div className="detail-gallery-thumb"><Image src={item.thumbnail} alt={`Illustration inspired by ${item.short}`} fill sizes="(max-width: 700px) 44vw, 20vw" /><span>THE PLACE, AT YOUR PACE</span></div><div className="detail-gallery-note"><span className="overline">A LITTLE MORE OF VIETNAM</span><p>{item.feeling}, with time left for the unplanned parts.</p><Link href="#overview">Explore the journey <ArrowUpRight /></Link></div></div>
        </div>

        <aside className="detail-booking-card">
          <span className="detail-booking-kicker">A LANTERN &amp; TIDE JOURNEY</span>
          <h2>{item.product}</h2>
          <div className="detail-booking-location"><MapPin />{item.short}, Vietnam</div>
          <div className="detail-rate"><span>From</span><strong>${item.price.toLocaleString("en-US")}</strong><small>/ guest</small></div>
          <span className="detail-rate-note">Illustrative sample rate</span>
          <Link className="detail-book-button" href={checkout}>Plan this journey <ArrowUpRight /></Link>
          <div className="detail-booking-foot"><CalendarDays /><span>Choose your preferred date and party size in the next step.</span></div>
          <p>Availability and final arrangements are confirmed by the travel partner.</p>
        </aside>
      </section>

      <nav className="detail-tabs" aria-label="Journey details"><a className="is-current" href="#overview">Overview</a><a href="#included">What&apos;s included</a><a href="#tour-details">Tour details</a></nav>

      <section className="detail-overview-section" id="overview"><div className="detail-section-heading"><span className="overline">A THOUGHTFUL WAY TO EXPLORE</span><h2>Make the most of {item.short}.</h2><p>{item.description} The pace is yours, with local insight to help you get closer to the character of the place.</p></div><div className="detail-overview-facts"><article><span><MapPin /></span><div><small>DESTINATION</small><strong>{item.short}, Vietnam</strong></div></article><article><span><Clock3 /></span><div><small>PACE &amp; DURATION</small><strong>{item.duration}</strong></div></article><article><span><Tag /></span><div><small>JOURNEY STYLE</small><strong>{item.type.toLowerCase()}</strong></div></article><article><span><Check /></span><div><small>THE FEELING</small><strong>{item.feeling}</strong></div></article></div></section>

      <section className="detail-included-section" id="included"><div><span className="overline">THE GOOD DETAILS</span><h2>Considered from the start.</h2></div><ul><li><Check />A locally hosted experience</li><li><Check />Thoughtfully selected stays</li><li><Check />A flexible pace with room to make it your own</li></ul></section>

      <section className="detail-itinerary-section" id="tour-details"><div className="detail-section-heading"><span className="overline">A FEEL FOR THE JOURNEY</span><h2>Let the days unfold.</h2><p>A sample rhythm to help you imagine the trip. Your travel partner can confirm the final itinerary with you.</p></div><div className="detail-itinerary-list"><article><span>01</span><div><h3>Arrive and settle in</h3><p>Meet your local host, get comfortable, and enjoy an easy first evening.</p></div></article><article><span>02</span><div><h3>Find the local rhythm</h3><p>Explore the character of the place, with time for food, people, and the moments in between.</p></div></article><article><span>03</span><div><h3>Take the day as it comes</h3><p>Leave space for a slower morning and decide what you would like to discover next.</p></div></article></div></section>
      <section className="detail-end-cta"><div><span className="overline">READY WHEN YOU ARE</span><h2>Let&apos;s plan {item.short}.</h2></div><Link className="detail-book-button" href={checkout}>Continue to booking <ArrowUpRight /></Link></section>
    </main>
    <StoreFooter />
  </div>
}
