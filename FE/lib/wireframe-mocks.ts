export type WireframeMetric = { label: string; value: string; note?: string }
export type WireframeTable = {
  title: string
  description: string
  columns: string[]
  rows: string[][]
  searchPlaceholder: string
  filters?: { label: string; values: string[]; column: number }[]
  metrics?: WireframeMetric[]
  note?: string
  action?: "review" | "copy" | "details"
  statusColumn?: number
}

export const tenantTables: Record<string, WireframeTable> = {
  products: {
    title: "Travel products", description: "Products and experiences available to campaigns.", searchPlaceholder: "Search products",
    filters: [{label:"Category",values:["All categories","Tours & cruises","Hotels","Packages"],column:1},{label:"Status",values:["All statuses","Active","Draft","Paused"],column:3}],
    columns: ["Product / service","Category","Retail price","Status","Actions"],
    rows: [["Ha Long Bay · 2 night cruise","Tours & cruises","$289","Active","View · Edit"],["Hoi An · Lantern Town Stay","Hotels","$146","Active","View · Edit"],["Da Nang · Coast & Cuisine","Packages","$420","Active","View · Edit"]], action:"details",
  },
  campaigns: {
    title: "Campaign management", description: "Manage travel offers shared with your reseller network.", searchPlaceholder: "Search campaigns",
    filters: [{label:"Status",values:["All statuses","Active","Draft","Paused"],column:5},{label:"Product",values:["All products","Ha Long Bay cruise","Hoi An stay","Phu Quoc beach stay"],column:1}],
    columns: ["Campaign name","Product","Commission","Start date","End date","Status","Actions"],
    rows: [["Ha Long Bay Weekend Escape","Ha Long Bay cruise","12%","01 Oct 2026","31 Dec 2026","Active","View"],["Hoi An Lantern Season","Hoi An stay","10%","15 Oct 2026","31 Jan 2027","Active","View"],["Phu Quoc Island Escape","Phu Quoc beach stay","$38 / booking","14 Oct 2026","28 Feb 2027","Active","View"]], action:"details",
  },
  resellers: {
    title: "Resellers", description: "Manage partner applications and active network members.", searchPlaceholder: "Search partners",
    filters: [{label:"Status",values:["All statuses","Active","Pending","Blocked"],column:2}],
    columns: ["Reseller","Campaigns joined","Status","Joined","Actions"],
    rows: [["Mia Nguyen · mia@roamwithmia.com","3","Active","13 Oct 2026","View details"],["Coastline Journeys · hello@coastline.example","2","Pending","14 Oct 2026","View details"],["Sam Rivera","1","Blocked","09 Oct 2026","View details"]], action:"details",
  },
  commissions: {
    title: "Commissions", description: "Review reseller commissions attributed to confirmed bookings.", searchPlaceholder: "Search reseller, campaign or order",
    filters: [{label:"Status",values:["All statuses","Pending","Approved","Paid"],column:5}],
    metrics: [{label:"Pending","value":"$2,840.50"},{label:"Approved","value":"$5,210.00"},{label:"Paid","value":"$4,789.50"}],
    columns: ["Reseller","Campaign","Order / conversion","Commission","Date","Status"],
    rows: [["Mia Nguyen","Ha Long Bay Weekend Escape","ORD-20418 · Cruise","$86.70","14 Oct 2026","Pending"],["Sam Rivera","Hoi An Lantern Season","ORD-20392 · Hotel","$42.00","13 Oct 2026","Approved"],["Mia Nguyen","Phu Quoc Island Escape","ORD-20118 · Tour","$38.00","09 Oct 2026","Paid"]],
  },
  "tracking-attribution": {
    title: "Tracking & attribution", description: "Referral activity and attributed bookings across partner links.", searchPlaceholder: "Search this page",
    filters: [{label:"Period",values:["Last 30 days"],column:-1},{label:"Campaign",values:["All campaigns"],column:-1}],
    metrics: [{label:"Total clicks",value:"18,420"},{label:"Conversions",value:"326"},{label:"Conversion rate",value:"1.77%"}],
    columns: ["Tracking link / referrer","Reseller","Campaign","Clicks","Conversions"],
    rows: [["wayfarer.example/r/0","Mia Nguyen","Ha Long Bay Weekend Escape","2,840","74"],["wayfarer.example/r/1","Coastline Journeys","Hoi An Lantern Season","1,920","31"],["wayfarer.example/r/2","Sam Rivera","Phu Quoc Island Escape","1,104","19"]],
  },
  "conversion-review": {
    title: "Conversion review", description: "Review sample bookings before commission approval.", searchPlaceholder: "Search order, campaign or reseller",
    filters: [{label:"Status",values:["All statuses","Pending","Approved","Rejected"],column:5}],
    metrics: [{label:"Pending review",value:"3"},{label:"Approved this month",value:"128"},{label:"Flagged",value:"1"}],
    columns: ["Booking","Campaign / product","Reseller","Amount","Commission","Status","Review"],
    rows: [["BK-20418 · 14 Oct 2026","Ha Long Bay Weekend Escape · Ha Long Bay cruise","Mia Nguyen","$722.50","$86.70","Pending","Approve · Reject"],["BK-20392 · 13 Oct 2026","Hoi An Lantern Season · Hoi An stay","Coastline Journeys","$420.00","$42.00","Pending","Approve · Reject"],["BK-20310 · 12 Oct 2026","Phu Quoc Island Escape · Beach stay","Northstar Travel","$380.00","$38.00","Pending","Approve · Reject"]], action:"review", statusColumn:5,
  },
  "ai-content-review": {
    title: "AI content review", description: "AI-assisted marketing content requires tenant review before publication.", searchPlaceholder: "Search this page",
    filters: [{label:"Status",values:["All statuses","Pending review","Approved","Changes requested"],column:4}],
    columns: ["Content","Campaign","Submitted by","Submitted","Status","Decision"],
    rows: [["Social caption · Drift through emerald waters and wake up beside the limestone peaks of Ha Long Bay.","Ha Long Bay Weekend Escape","Mai Tran · Tenant Admin","Today, 10:24","Pending review","Approve · Request changes"],["15-second video script · Your Hoi An evening starts where the lanterns meet the river.","Hoi An Lantern Season","Tenant Marketing Team","Yesterday","Approved","Reviewed"]], action:"review", statusColumn:4,
    note:"Sample content only. Approving or requesting changes updates this page preview only.",
  },
}

export const resellerTables: Record<string, WireframeTable> = {
  "my-campaigns": {
    title:"My campaigns", description:"Joined and pending offers", searchPlaceholder:"Search this page",
    filters:[{label:"Status",values:["All statuses","Active","Pending"],column:2}],
    columns:["Campaign","Commission","Status","Joined","Actions"],
    rows:[["Ha Long Bay Weekend Escape","12%","Active","10 Oct 2026","View · Tracking link"],["Hoi An Lantern Season","10%","Active","10 Oct 2026","View · Tracking link"],["Phu Quoc Island Escape","$38 / booking","Pending","10 Oct 2026","View · Tracking link"]],action:"details",
  },
  commissions: {
    title:"Commissions", description:"Illustrative attributed bookings and earnings.", searchPlaceholder:"Search this page",
    filters:[{label:"Status",values:["All statuses","Pending","Approved","Paid"],column:4}],
    metrics:[{label:"Total commission",value:"$4,218.60"},{label:"Pending",value:"$684.20"},{label:"Approved",value:"$1,122.40"},{label:"Paid",value:"$2,412.00"}],
    columns:["Campaign","Conversion / order","Amount","Date","Status"],
    rows:[["Ha Long Bay Weekend Escape","ORD-20418 · Cruise","$86.70","14 Oct 2026","Pending"],["Hoi An Lantern Season","ORD-20392 · Hotel","$42.00","14 Oct 2026","Approved"],["Phu Quoc Island Escape","ORD-20118 · Tour","$38.00","14 Oct 2026","Paid"]],
  },
  "tracking-links": {
    title:"Tracking links", description:"Referral links for campaigns you have joined.", searchPlaceholder:"Search this page",
    columns:["Campaign","Tracking URL","Clicks","Conversions","Created","Action"],
    rows:[["Ha Long Bay Weekend Escape","wayfarer.example/r/jordan/halong","2,840","74","10 Oct 2026","Copy link"],["Hoi An Lantern Season","wayfarer.example/r/jordan/hoian","1,920","31","10 Oct 2026","Copy link"],["Phu Quoc Island Escape","wayfarer.example/r/jordan/coast","1,104","19","10 Oct 2026","Copy link"]],action:"copy",
  },
  analytics: {
    title:"Performance analytics", description:"Your referral performance across campaigns.", searchPlaceholder:"Search this page",
    filters:[{label:"Period",values:["Last 30 days","Last 7 days","This month"],column:-1}],
    metrics:[{label:"Conversion rate",value:"2.4%"},{label:"Clicks",value:"8,492"},{label:"Conversions",value:"142"}],
    columns:["Campaign","Clicks","Conversions","Rate"],
    rows:[["Ha Long Bay Weekend Escape","2,840","74","2.61%"],["Hoi An Lantern Season","2,840","74","2.61%"],["Phu Quoc Island Escape","2,840","74","2.61%"]],
  },
}

export const supplierProducts: WireframeTable = {
  title:"My Products", description:"Products and availability shared with tenant partners.", searchPlaceholder:"Search products",
  filters:[{label:"Status",values:["All statuses","Active","Review","Paused"],column:4}],
  columns:["Product","Destination","Retail price","Availability","Status","Action"],
  rows:[["Ha Long Bay cruise · Cruise - 2 nights","Ha Long Bay","$420 / guest","12 slots","Active","Edit"],["Hoi An riverside stay · Boutique hotel - 3 nights","Hoi An","$280 / stay","8 rooms","Review","Edit"],["Phu Quoc beach stay · Beach resort - 4 nights","Phu Quoc","$380 / stay","16 rooms","Active","Edit"]], action:"details",
}

export const resellerCampaignCards = [
  {name:"Ha Long Bay Weekend Escape", product:"Ha Long Bay cruise", category:"Tours & cruises", destination:"Ha Long Bay", commission:"12%", dates:"01 Oct 2026 — 31 Dec 2026", status:"Open", image:"/images/halong-bay.svg"},
  {name:"Hoi An Lantern Season", product:"Hoi An stay", category:"Hotels", destination:"Hoi An", commission:"10%", dates:"15 Oct 2026 — 31 Jan 2027", status:"Open", image:"/images/hoi-an.svg"},
  {name:"Phu Quoc Island Escape", product:"Phu Quoc beach stay", category:"Hotels", destination:"Phu Quoc", commission:"$38 / booking", dates:"14 Oct 2026 — 28 Feb 2027", status:"Open", image:"/images/phu-quoc.svg"},
]

export const publishedAiAssets = [
  {type:"Social caption",campaign:"Ha Long Bay Weekend Escape",product:"Ha Long Bay cruise",language:"English",tone:"Warm and inspiring",copy:"Drift through emerald waters and wake up beside the limestone peaks of Ha Long Bay."},
  {type:"Promotional description",campaign:"Hoi An Lantern Season",product:"Hoi An stay",language:"English",tone:"Premium and refined",copy:"As evening settles over Hoi An, lantern light traces the river and the old town invites you to slow down."},
  {type:"Hashtag suggestions",campaign:"Phu Quoc Island Escape",product:"Phu Quoc beach stay",language:"Vietnamese",tone:"Playful and adventurous",copy:"#PhuQuoc #IslandDays #VietnamBySea"},
]
