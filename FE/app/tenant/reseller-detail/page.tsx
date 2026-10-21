import Link from "next/link"
import { notFound } from "next/navigation"
import { ArrowLeft, ArrowUpRight, MousePointerClick, ShoppingBag, UserRound, Users } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { tenantTables } from "@/lib/wireframe-mocks"

const resellerPerformance: Record<string, { clicks: string; conversions: string; campaigns: string[] }> = {
  "Mia Nguyen": { clicks: "2,840", conversions: "74", campaigns: ["Ha Long Bay Weekend Escape", "Hoi An Lantern Season", "Phu Quoc Island Escape"] },
  "Coastline Journeys": { clicks: "1,920", conversions: "31", campaigns: ["Ha Long Bay Weekend Escape", "Hoi An Lantern Season"] },
  "Sam Rivera": { clicks: "486", conversions: "8", campaigns: ["Ha Long Bay Weekend Escape"] },
}

const statusStyles: Record<string, string> = {
  Active: "bg-emerald-50 text-emerald-800",
  Pending: "bg-amber-50 text-amber-900",
  Blocked: "bg-slate-100 text-slate-700",
}

export default async function TenantResellerDetailPage({ searchParams }: { searchParams: Promise<{ reseller?: string }> }) {
  const { reseller: resellerName } = await searchParams
  const resellerRow = tenantTables.resellers.rows.find((row) => row[0].split(" Â· ")[0] === resellerName)
  if (!resellerRow) notFound()

  const [name, email] = resellerRow[0].split(" Â· ")
  const [campaignCount, status, joined] = [resellerRow[1], resellerRow[2], resellerRow[3]]
  const performance = resellerPerformance[name] ?? { clicks: "0", conversions: "0", campaigns: [] }

  return <div className="space-y-6">
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <Link href="/tenant/resellers" className="inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline"><ArrowLeft className="size-4"/>Back to resellers</Link>
        <p className="mt-4 text-xs font-semibold uppercase tracking-[.15em] text-teal-800">Tenant workspace · Partner profile</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight text-slate-900">{name}</h1>
        <p className="mt-1 text-sm text-slate-500">Reseller account details and campaign activity.</p>
      </div>
      <Badge className={statusStyles[status] ?? "bg-slate-100 text-slate-700"}>{status}</Badge>
    </div>

    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {[
        { label: "Campaigns joined", value: campaignCount, note: "Active partner campaigns", icon: ShoppingBag },
        { label: "Tracked clicks", value: performance.clicks, note: "Illustrative total", icon: MousePointerClick },
        { label: "Attributed bookings", value: performance.conversions, note: "Illustrative total", icon: ArrowUpRight },
        { label: "Partner since", value: joined, note: "Joined this workspace", icon: Users },
      ].map(({ label, value, note, icon: Icon }) => <Card key={label} className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-start justify-between pb-2"><div><CardDescription>{label}</CardDescription><CardTitle className="mt-2 text-2xl">{value}</CardTitle></div><span className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-800"><Icon className="size-[18px]"/></span></CardHeader><CardContent className="text-xs text-slate-500">{note}</CardContent></Card>)}
    </div>

    <div className="grid gap-4 lg:grid-cols-[0.85fr_1.15fr]">
      <Card className="border-slate-200/80 shadow-sm">
        <CardHeader><div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-xl bg-emerald-50 text-emerald-800"><UserRound className="size-5"/></span><div><CardTitle>Account information</CardTitle><CardDescription>Reseller contact and membership</CardDescription></div></div></CardHeader>
        <CardContent><dl className="divide-y divide-slate-100">{[{ label: "Reseller", value: name }, { label: "Email", value: email ?? "Not provided" }, { label: "Status", value: status }, { label: "Joined", value: joined }].map((item) => <div key={item.label} className="grid grid-cols-[100px_1fr] gap-3 py-3 text-sm"><dt className="text-slate-500">{item.label}</dt><dd className="break-all font-medium text-slate-800">{item.value}</dd></div>)}</dl></CardContent>
      </Card>

      <Card className="border-slate-200/80 shadow-sm">
        <CardHeader><CardTitle>Campaigns joined</CardTitle><CardDescription>Travel campaigns this partner is associated with.</CardDescription></CardHeader>
        <CardContent className="space-y-3">{performance.campaigns.map((campaign) => <div key={campaign} className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 bg-white px-4 py-3"><div className="min-w-0"><p className="truncate text-sm font-medium text-slate-800">{campaign}</p><p className="mt-1 text-xs text-slate-500">Partner campaign</p></div><ArrowUpRight className="size-4 shrink-0 text-slate-400"/></div>)}{performance.campaigns.length === 0 && <p className="py-6 text-center text-sm text-slate-500">No campaign activity is available for this reseller.</p>}</CardContent>
      </Card>
    </div>

    <div className="flex justify-end"><Link href="/tenant/resellers" className={buttonVariants({ variant: "outline" })}>Return to reseller list</Link></div>
  </div>
}
