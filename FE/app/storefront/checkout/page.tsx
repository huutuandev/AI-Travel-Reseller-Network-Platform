"use client"

import Link from "next/link"
import Image from "next/image"
import { Suspense, useState } from "react"
import { useSearchParams } from "next/navigation"
import type { FormEvent } from "react"
import { CalendarDays, Check, ClipboardCheck, CreditCard, Mail, MapPin, Phone, Users } from "lucide-react"
import { StoreFooter, StoreNav } from "@/components/storefront/store-chrome"
import { getJourney } from "@/lib/storefront-data"
import { createMockBooking, type MockBooking } from "@/lib/storefront-bookings"

export default function StorefrontCheckout() {
  return <Suspense fallback={<main className="checkout-wrap">Loading your journey…</main>}><CheckoutContent /></Suspense>
}

function CheckoutContent() {
  const searchParams = useSearchParams()
  const item = getJourney(searchParams.get("product"))
  const [guests, setGuests] = useState(2)
  const [confirmation, setConfirmation] = useState<MockBooking | null>(null)

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    if (!form.reportValidity()) return
    const data = new FormData(form)
    const guestCount = Number(data.get("guests"))
    setConfirmation(createMockBooking({
      productKey: item.key,
      productName: item.title,
      firstName: String(data.get("firstName")),
      lastName: String(data.get("lastName")),
      email: String(data.get("email")),
      phone: String(data.get("phone")),
      travelDate: String(data.get("travelDate")),
      guests: guestCount,
      notes: String(data.get("notes") ?? ""),
      total: item.price * guestCount,
    }))
  }

  return <div className="store-checkout-page">
    <StoreNav current="Home" returnTo={`/storefront/checkout?product=${item.key}`} home />
    <main className="checkout-shell">
      <div className="checkout-progress" aria-label="Booking progress"><span className="is-current"><i>1</i> TRAVEL DETAILS</span><b /><span><i>2</i> SAMPLE CONFIRMATION</span></div>
      <div className="checkout-title"><span className="overline">A GOOD JOURNEY STARTS HERE</span><h1>Secure checkout</h1><p>Share a few details so your travel partner can prepare your request.</p></div>

      {confirmation ? <section className="checkout-confirmation"><span className="confirmation-mark"><Check /></span><span className="overline">SAMPLE REQUEST PREPARED</span><h2>Thank you, {confirmation.firstName}.</h2><p>Your request for <strong>{item.title}</strong> is recorded in this prototype. No payment was taken and no travel reservation was created.</p><div className="checkout-confirmation-ref">Sample reference <strong>{confirmation.reference}</strong></div><Link className="checkout-primary-button" href="/storefront">Return to the collection <span>↗</span></Link></section> : <div className="checkout-layout">
        <form className="checkout-form checkout-form-reference" onSubmit={submit}>
          <section className="checkout-form-section">
            <div className="checkout-form-heading"><span className="checkout-section-icon"><Mail /></span><div><h2>Contact detail</h2><p>How can the travel partner reach you?</p></div></div>
            <div className="checkout-fields"><label><span>Email address</span><input type="email" name="email" autoComplete="email" placeholder="you@example.com" required /></label><label><span>Phone number</span><input type="tel" name="phone" autoComplete="tel" placeholder="Include country code" required /></label></div>
          </section>

          <section className="checkout-form-section">
            <div className="checkout-form-heading"><span className="checkout-section-icon"><Users /></span><div><h2>Traveler detail</h2><p>Who will be joining this journey?</p></div></div>
            <div className="checkout-fields"><label><span>First name</span><input name="firstName" autoComplete="given-name" placeholder="e.g. Alex" required /></label><label><span>Last name</span><input name="lastName" autoComplete="family-name" placeholder="e.g. Morgan" required /></label><label className="checkout-field-wide"><span>Notes for your travel partner <small>OPTIONAL</small></span><textarea name="notes" rows={3} placeholder="A special occasion, accessibility needs, or something you would love to experience" /></label></div>
          </section>

          <section className="checkout-form-section checkout-trip-section">
            <div className="checkout-form-heading"><span className="checkout-section-icon"><CalendarDays /></span><div><h2>Your trip</h2><p>Choose a preferred date and party size.</p></div></div>
            <div className="checkout-fields"><label><span>Preferred travel date</span><input type="date" name="travelDate" min="2026-10-19" required /></label><label><span>Number of guests</span><select name="guests" value={guests} onChange={(event) => setGuests(Number(event.target.value))}>{[1, 2, 3, 4].map((count) => <option key={count} value={count}>{count} {count === 1 ? "guest" : "guests"}</option>)}</select></label></div>
          </section>

          <div className="checkout-inline-note"><CreditCard /><p><strong>No payment details required</strong><span>This prototype does not process payments or send a real reservation request.</span></p></div>
          <button className="checkout-primary-button checkout-submit" type="submit">Prepare sample request <span>↗</span></button>
        </form>

        <aside className="checkout-aside">
          <section className="checkout-order-card"><h2>Review order details</h2><div className="checkout-order-image"><Image src={item.image} alt={`${item.short} landscape`} fill sizes="(max-width: 700px) 100vw, 390px" /><span>{item.region}</span></div><div className="checkout-order-content"><h3>{item.title}</h3><p className="checkout-order-location"><MapPin />{item.short}, Vietnam</p><div className="checkout-order-tags"><span>{item.duration}</span><span>{guests} {guests === 1 ? "guest" : "guests"}</span></div><div className="checkout-price-line"><span>Sample rate per guest</span><strong>${item.price.toLocaleString("en-US")}</strong></div><div className="checkout-price-line"><span>Guests</span><strong>× {guests}</strong></div><div className="checkout-price-total"><span>Illustrative total</span><strong>${(item.price * guests).toLocaleString("en-US")}</strong></div><small>Sample pricing only. Final details and availability are confirmed by the travel partner.</small></div></section>
          <section className="checkout-confidence"><h2>Booking information</h2><article><span><CreditCard /></span><div><strong>No payment now</strong><p>This request preview does not collect payment.</p></div></article><article><span><ClipboardCheck /></span><div><strong>Review your trip</strong><p>Check your selected date and guest count before submitting.</p></div></article><article><span><Phone /></span><div><strong>Partner confirmation</strong><p>Your request is a local prototype preview, not a confirmed booking.</p></div></article></section>
        </aside>
      </div>}
    </main>
    <StoreFooter />
  </div>
}
