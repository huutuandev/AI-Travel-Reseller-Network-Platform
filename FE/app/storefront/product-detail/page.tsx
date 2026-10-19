"use client"

import Link from "next/link"
import { Suspense } from "react"
import { useSearchParams } from "next/navigation"
import { StoreFooter, StoreNav } from "../page"

const products = {
  halong: { title:"Ha Long Bay Weekend Escape", product:"Ha Long Bay cruise", location:"HA LONG BAY · VIETNAM", short:"Ha Long Bay", price:420, image:"/images/halong-bay.svg", description:"A slow sailing journey through emerald waters and limestone karsts.", duration:"Two unhurried nights", feeling:"Quiet discovery", number:"01" },
  hoian: { title:"Hoi An Lantern Season", product:"Hoi An riverside stay", location:"HOI AN · VIETNAM", short:"Hoi An", price:280, image:"/images/hoi-an.svg", description:"Stay by the river and discover lantern-lit streets after dusk.", duration:"Three nights", feeling:"Old town evenings", number:"02" },
  phuquoc: { title:"Phu Quoc Island Escape", product:"Phu Quoc beach stay", location:"PHU QUOC · VIETNAM", short:"Phu Quoc", price:380, image:"/images/phu-quoc.svg", description:"A sunlit island hideaway with soft sand and clear blue water.", duration:"Four island nights", feeling:"Coastal reset", number:"03" },
}

export default function ProductDetailPage() {
  return <Suspense fallback={<main className="product-detail">Loading journey…</main>}><ProductDetailContent/></Suspense>
}

function ProductDetailContent() {
  const searchParams=useSearchParams()
  const requested=searchParams.get("product")
  const key=requested&&requested in products?requested as keyof typeof products:"halong"
  const item=products[key]
  const checkout=`/storefront/checkout?product=${key}`
  return <><div className="announcement"><span>THE LANTERN &amp; TIDE COLLECTION</span><i/><span>A thoughtful way to discover Vietnam</span><Link href="/storefront#journeys">Explore the collection <b>→</b></Link></div><StoreNav current="The collection"/><main className="product-detail"><div className="detail-breadcrumb"><Link href="/storefront">Home</Link><span>/</span><Link href="/storefront#journeys">The collection</Link><span>/</span><span>{item.short}</span></div><section className="detail-grid"><div className="detail-visual"><img src={item.image} alt={`${item.short}, Vietnam`}/><div className="visual-caption"><span className="visual-pin">LT</span><span>CURATED IN VIETNAM</span><b>{item.number} — 03</b></div><div className="visual-vertical">A SLOWER WAY TO SEE THE WORLD</div></div><div className="detail-copy"><div className="detail-eyebrow"><span className="overline">THE LANTERN &amp; TIDE COLLECTION</span><span className="detail-id">NO. <b>{item.number}</b></span></div><span className="detail-location">{item.location}</span><h1>{item.title}</h1><p className="detail-product">{item.product}</p><p className="detail-lead">{item.description}</p><div className="detail-rule"/><div className="detail-facts"><div><span>THE PACE</span><strong>{item.duration}</strong></div><div><span>THE FEELING</span><strong>{item.feeling}</strong></div></div><p className="detail-body">Wake to a quiet horizon, spend the day drifting between limestone islands, then return to a private deck as the bay turns gold. Your local host can help tailor the details to your pace.</p><div className="detail-includes"><span><i>01</i> A locally hosted experience</span><span><i>02</i> Thoughtfully selected stays</span><span><i>03</i> Room to make it your own</span></div><div className="detail-booking"><div><span>YOUR JOURNEY, FROM</span><strong>${item.price} <small>/ guest</small></strong><small>Sample rate · Final details confirmed by the travel partner</small></div><Link className="store-button" href={checkout}>Plan this journey <b>→</b></Link></div><p className="detail-prototype-note">Illustrative experience and pricing for this prototype.</p></div></section><section className="itinerary"><div className="itinerary-heading"><span className="overline">A FEEL FOR THE JOURNEY</span><h2>Every day has room<br/>to become its own.</h2></div><div className="itinerary-days"><article><span>DAY 01</span><div><h3>Arrive, then exhale</h3><p>Meet your local host and settle in. The first evening is intentionally yours.</p></div></article><article><span>DAY 02</span><div><h3>Follow the water</h3><p>A day shaped around the place: its people, its rhythms and the moments in between.</p></div></article><article><span>DAY 03</span><div><h3>Leave a little later</h3><p>A final slow morning before you decide where the road goes next.</p></div></article></div></section><section className="detail-bottom-cta"><div><span className="overline">A GOOD PLACE TO BEGIN</span><h2>Make this journey<br/><em>your own.</em></h2></div><Link className="store-button" href={checkout}>Check your journey <b>→</b></Link></section></main><StoreFooter/></>
}
