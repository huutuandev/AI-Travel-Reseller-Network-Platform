import { AlertTriangle, Eye, PackageCheck } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { supplierDashboard } from "@/lib/mock-data"

export default function SupplierDashboard() {
  return <>
    <div><p className="text-sm font-medium text-teal-800">Supplier Portal</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">Overview</h1><p className="mt-1 text-sm text-slate-500">Illustrative workspace · Sample data · No backend connected</p></div>
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{supplierDashboard.metrics.map((metric)=><Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-3xl">{metric.value}</CardTitle></CardHeader><CardContent className="text-xs text-slate-500">{metric.note}</CardContent></Card>)}</div>
    <div className="grid gap-4 lg:grid-cols-2"><Card className="border-slate-200/80 shadow-sm"><CardHeader><div className="flex items-start justify-between"><div><CardTitle>Availability to review</CardTitle><CardDescription>Upcoming inventory windows</CardDescription></div><AlertTriangle className="size-5 text-amber-600"/></div></CardHeader><CardContent className="space-y-3">{supplierDashboard.availability.map((item)=><div key={item.product} className="flex items-center justify-between gap-3 rounded-lg border border-slate-100 px-3 py-3"><span className="font-medium text-sm">{item.product}</span><Badge className={item.pending?"bg-amber-50 text-amber-900":"bg-emerald-50 text-emerald-800"}>{item.status}</Badge></div>)}</CardContent></Card>
      <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>Supplier workspace</CardTitle><CardDescription>Product information shared with tenant partners</CardDescription></CardHeader><CardContent><div className="flex size-12 items-center justify-center rounded-xl bg-teal-50 text-teal-800"><PackageCheck className="size-6"/></div><p className="mt-4 text-sm leading-6 text-slate-600">Maintain product details and availability shared with tenant partners. Campaigns, reseller relationships, and commission rules remain managed by the tenant.</p><div className="mt-5 flex items-center gap-2 text-xs text-slate-500"><Eye className="size-4"/>Product views are sample counts for the last 30 days.</div></CardContent></Card></div>
  </>
}
