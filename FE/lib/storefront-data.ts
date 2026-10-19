export type Journey = {
  key: string
  title: string
  product: string
  location: string
  short: string
  region: string
  type: string
  nights: string
  description: string
  price: number
  image: string
  thumbnail: string
  duration: string
  feature: string
  feeling: string
  number: string
}

// Frontend demonstration data. Replace this source with the storefront API adapter when available.
export const storefrontJourneys: Journey[] = [
  {
    key: "halong",
    title: "Ha Long Bay Weekend Escape",
    product: "Ha Long Bay cruise",
    location: "HA LONG BAY · VIETNAM",
    short: "Ha Long Bay",
    region: "THE NORTHEAST COAST",
    type: "SEA · SLOW TRAVEL",
    nights: "2 NIGHTS",
    description: "A slow sailing journey through emerald waters and limestone karsts.",
    price: 420,
    image: "/images/halong-bay-photo.jpg",
    thumbnail: "/images/halong-bay.svg",
    duration: "Two unhurried nights",
    feature: "Ha Long Bay, unhurried",
    feeling: "Quiet discovery",
    number: "01",
  },
  {
    key: "hoian",
    title: "Hoi An Lantern Season",
    product: "Hoi An riverside stay",
    location: "HOI AN · VIETNAM",
    short: "Hoi An",
    region: "THE OLD TOWN",
    type: "CULTURE · BOUTIQUE STAY",
    nights: "3 NIGHTS",
    description: "A riverside hideaway and evenings that belong to the old town.",
    price: 280,
    image: "/images/hoi-an-photo.jpg",
    thumbnail: "/images/hoi-an.svg",
    duration: "Three nights",
    feature: "Hoi An after the lanterns",
    feeling: "Old town evenings",
    number: "02",
  },
  {
    key: "phuquoc",
    title: "Phu Quoc Island Escape",
    product: "Phu Quoc beach stay",
    location: "PHU QUOC · VIETNAM",
    short: "Phu Quoc",
    region: "THE SOUTHERN ISLANDS",
    type: "ISLAND · COASTAL STAY",
    nights: "4 NIGHTS",
    description: "Clear water, long afternoons and nowhere else you need to be.",
    price: 380,
    image: "/images/phu-quoc-photo.jpg",
    thumbnail: "/images/phu-quoc.svg",
    duration: "Four island nights",
    feature: "Phu Quoc, all to yourself",
    feeling: "Coastal reset",
    number: "03",
  },
]

export const getJourney = (key: string | null | undefined) =>
  storefrontJourneys.find((journey) => journey.key === key) ?? storefrontJourneys[0]
