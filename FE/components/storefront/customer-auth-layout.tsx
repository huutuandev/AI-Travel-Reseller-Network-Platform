import Link from "next/link"

export function CustomerAuthLayout({ eyebrow, title, description, children }: { eyebrow: string; title: string; description: string; children: React.ReactNode }) {
  return <main className="customer-auth-page">
    <section className="customer-auth-visual">
      <Link className="store-brand customer-auth-brand" href="/storefront" aria-label="Lantern and Tide Travel home"><span className="brand-seal">L<span>&amp;</span>T</span><span className="brand-name">LANTERN <i>&amp;</i> TIDE<small>TRAVEL, THOUGHTFULLY</small></span></Link>
      <div className="customer-auth-visual-copy"><span className="overline">A LITTLE CLOSER TO SOMEWHERE</span><h1>Make room<br/>for <em>the unexpected.</em></h1><p>Keep your favorite journeys close and take the next step when it feels right.</p></div>
      <span className="customer-auth-photo-caption">A QUIETER MORNING IN VIETNAM <i/> LANTERN &amp; TIDE TRAVEL</span>
    </section>
    <section className="customer-auth-panel">
      <Link className="customer-auth-back" href="/storefront">← Back to the collection</Link>
      <div className="customer-auth-content"><span className="overline">{eyebrow}</span><h2>{title}</h2><p className="customer-auth-description">{description}</p>{children}</div>
      <p className="customer-auth-legal">A Lantern &amp; Tide customer account · Prototype experience</p>
    </section>
  </main>
}
