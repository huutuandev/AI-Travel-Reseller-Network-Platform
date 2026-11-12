export const tenantDashboard = {
  metrics: [
    { label: "Total campaigns", value: "12", note: "+2 this month" },
    { label: "Active resellers", value: "84", note: "+12% this month" },
    { label: "Partner clicks", value: "18,420", note: "+8.4% vs last month" },
    { label: "Conversions", value: "326", note: "Attributed bookings" },
    { label: "Commission", value: "$12,840", note: "Across 326 conversions" },
  ],
  activity: [
    { type: "New conversion", partner: "Mia Nguyen", campaign: "Ha Long Bay Weekend Escape", time: "12 min ago", status: "Approved" },
    { type: "Reseller joined", partner: "Coastline Journeys", campaign: "Hoi An Lantern Season", time: "1 hour ago", status: "New partner" },
    { type: "Campaign published", partner: "Avery Morgan", campaign: "Phu Quoc Island Escape", time: "Yesterday", status: "Live" },
    { type: "New conversion", partner: "The Wandering Lens", campaign: "Ha Long Bay Weekend Escape", time: "Yesterday", status: "In review" },
  ],
}
export const platformAdminDashboard = {
  metrics: [
    { label: "Total tenants", value: "248", note: "12 added this month" },
    { label: "Active tenants", value: "229", note: "92.3% of all tenants" },
    { label: "Pending review", value: "12", note: "Requires onboarding follow-up" },
    { label: "Suspended", value: "7", note: "Platform-level status" },
  ],
  tenantEvents: [
    { tenant: "Lantern & Tide Travel", domain: "lantern-tide.aitravel.example", activity: "Tenant workspace activated", status: "Active", date: "15 Oct 2026" },
    { tenant: "SunWorld Travel", domain: "sunworld.aitravel.example", activity: "Brand configuration updated", status: "Active", date: "14 Oct 2026" },
    { tenant: "Coastal Routes Co.", domain: "coastal-routes.aitravel.example", activity: "Tenant application submitted", status: "Pending", date: "14 Oct 2026" },
  ],
  usage: [
    { label: "Tracked clicks", value: "1.28M", width: 82 },
    { label: "Attributed bookings", value: "8,462", width: 61 },
    { label: "Campaigns running", value: "1,936", width: 73 },
  ],
}

export const platformAdminTenants = [
  { business: "SunWorld Travel", description: "Travel experiences - Vietnam", id: "tn_sunworld", domain: "sunworld.aitravel.example", status: "Active", created: "08 Mar 2026" },
  { business: "Lantern & Tide Travel", description: "Boutique stays - Vietnam", id: "tn_lantern_tide", domain: "lantern-tide.aitravel.example", status: "Active", created: "15 Oct 2026" },
  { business: "Coastal Routes Co.", description: "Regional tour operator", id: "tn_coastal_routes", domain: "coastal-routes.aitravel.example", status: "Pending", created: "14 Oct 2026" },
  { business: "Highland Pathways", description: "Guided journeys - Vietnam", id: "tn_highland_pathways", domain: "highland-pathways.aitravel.example", status: "Suspended", created: "19 Jun 2026" },
]

export const platformTenantDetail = {
  name: "SunWorld Travel",
  id: "tn_sunworld",
  domain: "sunworld.aitravel.example",
  created: "08 March 2026",
  contact: "Mai Tran - admin@sunworld.example",
  metrics: [
    { label: "Active campaigns", value: "18", note: "Tenant-scoped sample" },
    { label: "Resellers", value: "426", note: "398 active" },
    { label: "Tracked clicks - 30 days", value: "48,290", note: "Across tenant campaigns" },
    { label: "Attributed bookings", value: "1,204", note: "Illustrative usage data" },
  ],
  events: [
    { event: "Campaign updated: Ha Long Bay Weekend Escape", area: "Campaign management", when: "14 Oct 2026 - 10:42" },
    { event: "Brand colors saved", area: "White label", when: "13 Oct 2026 - 16:08" },
    { event: "Reseller approved", area: "Partner network", when: "13 Oct 2026 - 09:27" },
  ],
}

export const platformSystemLogs = [
  { time: "15 Oct 2026 - 09:14", event: "Tenant configuration loaded", scope: "tn_lantern_tide", result: "Success" },
  { time: "15 Oct 2026 - 08:42", event: "AI content template preview", scope: "Platform template", result: "Demo only" },
  { time: "14 Oct 2026 - 18:06", event: "Tenant status review", scope: "tn_coastal_routes", result: "Pending" },
]

export const resellerDashboard = {
  socialViews: [
    { platform: "TikTok", views: 24580 },
    { platform: "Facebook", views: 18320 },
  ],
  metrics: [
    { label: "Total clicks", value: "8,492", note: "↑ 11.4%" },
    { label: "Conversions", value: "142", note: "↑ 6.8%" },
    { label: "Pending commission", value: "$684.20", note: "Across 7 bookings" },
    { label: "Total earnings", value: "$4,218.60", note: "Illustrative total" },
  ],
  commissionSummary: [
    { label: "Commission pending", value: "$684.20", note: "7 bookings awaiting confirmation" },
    { label: "Commission approved", value: "$1,122.40", note: "Ready for payout" },
    { label: "Commission paid", value: "$2,412.00", note: "Illustrative lifetime total" },
  ],
  campaigns: ["Ha Long Bay Weekend Escape", "Hoi An Lantern Season", "Phu Quoc Island Escape"],
  chart: [74, 76, 80, 83, 84, 85, 87, 90, 92, 96, 99, 100],
  linkConversions: [56, 63, 63, 69, 69, 75, 69, 81, 75, 88, 81, 100],
  trackingLinks: [
    { campaign: "Ha Long Bay Weekend Escape", url: "wayfarer.example/r/jordan/halong", clicks: "2,840", conversions: "74" },
    { campaign: "Hoi An Lantern Season", url: "wayfarer.example/r/jordan/hoian", clicks: "1,920", conversions: "31" },
    { campaign: "Phu Quoc Island Escape", url: "wayfarer.example/r/jordan/coast", clicks: "1,104", conversions: "19" },
  ],
  commissions: [
    { campaign: "Ha Long Bay Weekend Escape", order: "ORD-20418 · Cruise booking", amount: "$86.70", date: "14 Oct 2026", status: "Pending" },
    { campaign: "Hoi An Lantern Season", order: "ORD-20392 · Hotel stay", amount: "$42.00", date: "13 Oct 2026", status: "Approved" },
    { campaign: "Phu Quoc Island Escape", order: "ORD-20118 · Island stay", amount: "$38.00", date: "09 Oct 2026", status: "Paid" },
  ],
}
