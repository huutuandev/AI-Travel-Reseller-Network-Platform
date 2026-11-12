import Image from "next/image"
import Link from "next/link"
import { ArrowUpRight } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import type { Journey } from "@/lib/storefront-data"

export function TravelProductCard({ journey }: { journey: Journey }) {
  return <article className={`trip-card ${journey.key === "halong" ? "trip-card-featured" : ""}`}>
    <Card className="trip-card-panel">
      <Link href={`/storefront/product-detail?product=${journey.key}`} aria-label={`View ${journey.title}`}>
        <div className="trip-image"><Image className={`destination-photo destination-photo-${journey.key}`} src={journey.image} alt={`${journey.short}, Vietnam`} fill sizes="(max-width: 700px) 100vw, (max-width: 1000px) 50vw, 33vw"/><span className="trip-number">{journey.number}</span><span className="trip-location">{journey.region}</span></div>
        <CardContent className="trip-copy"><div className="trip-card-meta"><span>{journey.type}</span><span>{journey.nights}</span></div><h3>{journey.title}</h3><p>{journey.description}</p><div className="trip-card-bottom"><span>From <strong>${journey.price.toLocaleString("en-US")}</strong> <small>/ guest</small></span><b aria-hidden="true"><ArrowUpRight/></b></div></CardContent>
      </Link>
    </Card>
  </article>
}
