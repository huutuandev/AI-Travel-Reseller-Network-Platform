"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import { ArrowUpRight, Check, Copy, Search, Video } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import type { WireframeTable } from "@/lib/wireframe-mocks"

const badgeStyles: Record<string, string> = {
  Active:"bg-emerald-50 text-emerald-800", Approved:"bg-emerald-50 text-emerald-800", Paid:"bg-sky-50 text-sky-800",
  Pending:"bg-amber-50 text-amber-900", "Pending review":"bg-amber-50 text-amber-900", Review:"bg-amber-50 text-amber-900",
  Rejected:"bg-red-50 text-red-800", Blocked:"bg-slate-100 text-slate-700", Draft:"bg-slate-100 text-slate-700",
}

export function WireframeTablePage({ config, hrefPrefix }: { config: WireframeTable; hrefPrefix: string }) {
  const [rows, setRows] = useState(config.rows)
  const [query, setQuery] = useState("")
  const [filters, setFilters] = useState<string[]>(config.filters?.map((filter)=>filter.values[0]) ?? [])
  const [feedback, setFeedback] = useState("")
  const [copied, setCopied] = useState("")
  const filteredRows = useMemo(() => rows.filter((row) => {
    const matchesQuery = row.join(" ").toLowerCase().includes(query.toLowerCase())
    const matchesFilters = (config.filters ?? []).every((filter,index) => {
      const value = filters[index]
      return !value || value === filter.values[0] || filter.column < 0 || (row[filter.column] ?? "").toLowerCase().includes(value.toLowerCase())
    })
    return matchesQuery && matchesFilters
  }), [config.filters, filters, query, rows])

  function reviewRow(rowIndex: number, accepted: boolean) {
    const target = filteredRows[rowIndex]
    if (!target || config.statusColumn === undefined) return
    const currentIndex = rows.indexOf(target)
    const status = accepted ? "Approved" : config.title === "AI content review" ? "Changes requested" : "Rejected"
    setRows((current)=>current.map((row,index)=>index===currentIndex?row.map((cell,column)=>column===config.statusColumn?status:column===row.length-1?"Reviewed":cell):row))
    setFeedback(`${accepted?"Approved":"Review decision saved"}: ${target[0]}. This changed this page preview only.`)
  }

  async function copyValue(value: string) {
    try {
      await navigator.clipboard.writeText(value.startsWith("http")?value:`https://${value}`)
      setCopied(value)
      setFeedback("")
      window.setTimeout(()=>setCopied(""),1600)
    } catch {
      setFeedback("Clipboard access is unavailable in this browser context.")
    }
  }

  function performDetailAction(action: string, row: string[]) {
    setFeedback(`${action} selected for ${row[0]}. This preview does not save changes.`)
  }

  return <>
    <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[.15em] text-teal-800">{hrefPrefix==="/tenant"?"Tenant workspace":"Reseller Portal"}</p><h1 className="mt-1 text-3xl font-semibold tracking-tight">{config.title}</h1><p className="mt-1 max-w-3xl text-sm text-slate-500">{config.description}</p></div>{config.title==="Campaign management"&&<Link href="/tenant/campaign-create" className="inline-flex items-center gap-2 rounded-md bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900">Create campaign <ArrowUpRight className="size-4"/></Link>}{config.title==="My campaigns"&&<Link href="/reseller/campaign-marketplace" className="inline-flex items-center gap-2 rounded-md bg-teal-800 px-4 py-2 text-sm font-medium text-white hover:bg-teal-900">Browse campaigns <ArrowUpRight className="size-4"/></Link>}</div>
    {config.metrics&&<div className={`grid gap-4 sm:grid-cols-2 ${config.metrics.length>3?"xl:grid-cols-4":"xl:grid-cols-3"}`}>{config.metrics.map((metric)=><Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-2xl">{metric.value}</CardTitle></CardHeader>{metric.note&&<CardContent className="text-xs text-slate-500">{metric.note}</CardContent>}</Card>)}</div>}
    {config.title==="Performance analytics"&&<Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><CardTitle>Clicks &amp; conversions</CardTitle><select aria-label="Analytics period" value={filters[0]} onChange={event=>setFilters(current=>current.map((value,index)=>index===0?event.target.value:value))} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option>Last 30 days</option></select></CardHeader><CardContent><div className="flex h-56 items-end gap-2 border-b border-slate-100 px-1 sm:gap-4">{[[40,20],[55,35],[47,27],[72,52],[64,44],[83,63],[67,47],[94,74],[76,56],[86,66],[71,51],[100,80]].map(([clicks,conversions],index)=><div key={index} className="flex h-full flex-1 items-end justify-center gap-1"><span className="w-2/5 rounded-t bg-teal-700" style={{height:`${clicks}%`}}/><span className="w-2/5 rounded-t bg-amber-400" style={{height:`${conversions}%`}}/></div>)}</div><div className="mt-3 flex justify-between text-[10px] text-slate-400"><span>Sep 15</span><span>Sep 22</span><span>Sep 29</span><span>Oct 6</span><span>Oct 13</span></div><div className="mt-3 flex gap-5 text-xs text-slate-500"><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-teal-700"/>Clicks</span><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-amber-400"/>Conversions</span></div></CardContent></Card>}
    <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>{config.title}</CardTitle><CardDescription>{config.description}</CardDescription></CardHeader><CardContent className="space-y-4"><div className="flex flex-col gap-3 lg:flex-row"><div className="relative min-w-[220px] flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"/><Input type="search" aria-label={config.searchPlaceholder} placeholder={config.searchPlaceholder} value={query} onChange={(event)=>setQuery(event.target.value)} className="pl-9"/></div>{config.filters?.filter((filter)=>config.title!=="Performance analytics"||filter.column>=0).map((filter,index)=><select key={filter.label} aria-label={filter.label} value={filters[index]} onChange={(event)=>setFilters((current)=>current.map((value,i)=>i===index?event.target.value:value))} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option disabled>{filter.label}</option>{filter.values.map((value)=><option key={value}>{value}</option>)}</select>)}</div>
    <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500">{config.columns.map((column)=><th key={column} className="pb-3 pr-3 font-medium">{column}</th>)}</tr></thead><tbody>{filteredRows.map((row,rowIndex)=><tr key={`${row[0]}-${rowIndex}`} className="border-b align-top last:border-0">{row.map((cell,columnIndex)=><td key={`${columnIndex}-${cell}`} className="py-3.5 pr-3 text-slate-700">{columnIndex===config.statusColumn||badgeStyles[cell]?<Badge className={badgeStyles[cell]??"bg-slate-100 text-slate-700"}>{cell}</Badge>:columnIndex===row.length-1&&config.action==="review"?cell.split(" · ").map((action)=><Button key={action} size="sm" variant={action==="Approve"?"default":"outline"} className="mr-1 mb-1" disabled={cell==="Reviewed"} onClick={()=>reviewRow(rowIndex,action==="Approve")}>{cell==="Reviewed"?<Check className="size-3.5"/>:null}{cell==="Reviewed"?"Reviewed":action}</Button>):columnIndex===row.length-1&&config.action==="copy"?<Button size="sm" variant="outline" onClick={()=>copyValue(row[1])}>{copied===row[1]?<Check className="size-3.5"/>:<Copy className="size-3.5"/>}{copied===row[1]?"Copied":"Copy link"}</Button>:columnIndex===row.length-1&&config.action==="details"?config.title==="Resellers"?cell.split(" · ").map((action)=><Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`/tenant/reseller-detail?reseller=${encodeURIComponent(row[0].split(" Â· ")[0])}`}>{action}<ArrowUpRight className="size-3.5"/></Link>):(config.title.toLowerCase().includes("campaign")||config.title==="My campaigns")?cell.split(" · ").map((action)=>action==="View"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`${hrefPrefix}/campaign-detail?campaign=${encodeURIComponent(row[0])}`}>{action}<ArrowUpRight className="size-3.5"/></Link>:config.title==="My campaigns"&&action==="Tracking link"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href="/reseller/tracking-links">{action}<ArrowUpRight className="size-3.5"/></Link>:config.title==="My campaigns"&&action==="Get content"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`/reseller/campaign-content?campaign=${encodeURIComponent(row[0])}`}><Video className="size-3.5"/>{action}</Link>:<Button key={action} size="sm" variant="outline" className="mr-1" onClick={()=>performDetailAction(action,row)}>{action}</Button>):cell.split(" · ").map((action)=><Button key={action} size="sm" variant="outline" className="mr-1" onClick={()=>performDetailAction(action,row)}>{action}</Button>):cell}</td>)}</tr>)}</tbody></table>{filteredRows.length===0&&<p className="py-10 text-center text-sm text-slate-500">No records match the current search and filters.</p>}</div><div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500"><span>Showing {filteredRows.length} of {rows.length} sample records</span>{feedback&&<span role="status" className="text-emerald-800">{feedback}</span>}</div></CardContent></Card>
    {config.note&&<p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-900">{config.note}</p>}
  </>
}

