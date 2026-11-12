"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import { ArrowUpRight, Check, ChevronDown, ChevronUp, Copy, Search, Video } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { resellerCampaignCards, type WireframeTable } from "@/lib/wireframe-mocks"
import { useResellerCampaignMembership } from "@/lib/reseller-campaign-membership"

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
  const [showAllResellers, setShowAllResellers] = useState(false)
  const { memberships } = useResellerCampaignMembership(config.title === "My campaigns")
  const sourceRows = useMemo(() => config.title === "My campaigns"
    ? resellerCampaignCards.flatMap((campaign) => {
      const participation = memberships[campaign.name]
      return participation ? [[campaign.name, campaign.commission, participation.status, participation.joinedAt, participation.status === "Active" ? "View · Tracking link · Get content" : "View"]] : []
    })
    : rows, [config.title, memberships, rows])
  const filteredRows = useMemo(() => sourceRows.filter((row) => {
    const matchesQuery = row.join(" ").toLowerCase().includes(query.toLowerCase())
    const matchesFilters = (config.filters ?? []).every((filter,index) => {
      const value = filters[index]
      return !value || value === filter.values[0] || filter.column < 0 || (row[filter.column] ?? "").toLowerCase().includes(value.toLowerCase())
    })
    return matchesQuery && matchesFilters
  }), [config.filters, filters, query, sourceRows])
  const resellerLeaderboard = [
    { name: "Mia Nguyen", likes: 142, shares: 36, comments: 24 },
    { name: "Coastline Journeys", likes: 118, shares: 42, comments: 19 },
    { name: "Sam Rivera", likes: 90, shares: 30, comments: 22 },
  ].map((reseller) => ({
    ...reseller,
    score: reseller.likes + reseller.shares * 2 + reseller.comments * 3,
  })).sort((a, b) => b.score - a.score)
  const visibleResellers = showAllResellers ? resellerLeaderboard : resellerLeaderboard.slice(0, 3)

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
    {config.title === "Resellers" && (
      <Card className="overflow-hidden gap-0 border-amber-200/70 bg-gradient-to-br from-white via-white to-amber-50/40 p-0 shadow-[0_12px_36px_-22px_rgba(15,118,110,0.35)]">
        <CardHeader className="border-b border-amber-100 bg-gradient-to-r from-amber-50 via-teal-50/70 to-cyan-50/70 px-5 py-5 sm:px-6">
          <div className="flex items-center gap-4">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-yellow-300 via-amber-400 to-orange-500 text-white shadow-[0_8px_20px_-8px_rgba(217,119,6,0.8)] ring-1 ring-amber-300">
              <svg aria-hidden="true" viewBox="0 0 64 64" className="size-11" shapeRendering="geometricPrecision">
                <defs>
                  <linearGradient id="leaderboard-cup-gold" x1="10" y1="5" x2="54" y2="60" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#FFF18A" />
                    <stop offset=".42" stopColor="#FFC928" />
                    <stop offset=".76" stopColor="#F2A900" />
                    <stop offset="1" stopColor="#D97706" />
                  </linearGradient>
                  <linearGradient id="leaderboard-cup-handle" x1="5" y1="10" x2="59" y2="38" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#FFF18A" />
                    <stop offset="1" stopColor="#E89300" />
                  </linearGradient>
                </defs>
                <path d="M16 12H7v7c0 8 5 13 13 15m28-22h9v7c0 8-5 13-13 15" fill="none" stroke="url(#leaderboard-cup-handle)" strokeWidth="4.5" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M16 8h32l-3 19c-1.2 8.1-5.7 13.2-13 15-7.3-1.8-11.8-6.9-13-15L16 8Z" fill="url(#leaderboard-cup-gold)" stroke="#D99000" strokeWidth="1.5" strokeLinejoin="round" />
                <path d="M21 13h22l-1.7 12c-.7 5-3.6 8.6-9.3 10.3-5.7-1.7-8.6-5.3-9.3-10.3L21 13Z" fill="#FFF8CC" fillOpacity=".42" />
                <path d="M24 42v7m16-7v7m-20 0h24l4 6H16l4-6Z" fill="url(#leaderboard-cup-gold)" stroke="#D99000" strokeWidth="1.5" strokeLinejoin="round" />
                <path d="M21 54h22" stroke="#FFF0A6" strokeWidth="1.5" strokeLinecap="round" />
                <path d="M17 8h30" stroke="#FFF8CC" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <CardTitle className="text-lg tracking-tight">Reseller leaderboard</CardTitle>
              <CardDescription className="mt-1">Top partners ranked by social engagement.</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] table-fixed text-left text-sm">
              <colgroup><col className="w-[12%]" /><col className="w-[34%]" /><col className="w-[12%]" /><col className="w-[12%]" /><col className="w-[14%]" /><col className="w-[16%]" /></colgroup>
              <thead className="bg-gradient-to-r from-amber-50/80 via-white to-teal-50/70 text-[11px] uppercase tracking-[.09em] text-slate-500">
                <tr>
                  <th className="py-3.5 pl-7 pr-4 font-semibold">Rank</th>
                  <th className="py-3.5 pl-16 pr-4 font-semibold">Reseller</th>
                  <th className="px-4 py-3.5 text-center font-semibold">Likes</th>
                  <th className="px-4 py-3.5 text-center font-semibold">Shares</th>
                  <th className="px-4 py-3.5 text-center font-semibold">Comments</th>
                  <th className="px-5 py-3.5 text-center font-semibold sm:px-6">Points</th>
                </tr>
              </thead>
              <tbody>
                {visibleResellers.map((reseller, index) => {
                  const medal = [
                    { disc: "from-amber-50 via-yellow-200 to-amber-400 text-amber-800 border-yellow-100 ring-yellow-100", wreath: "text-amber-700/75", ribbon: "bg-gradient-to-b from-red-600 to-red-800" },
                    { disc: "from-white via-slate-100 to-slate-300 text-slate-600 border-white ring-slate-100", wreath: "text-slate-500/70", ribbon: "bg-gradient-to-b from-blue-600 to-blue-800" },
                    { disc: "from-orange-50 via-orange-200 to-orange-400 text-orange-900 border-orange-100 ring-orange-100", wreath: "text-orange-800/75", ribbon: "bg-gradient-to-b from-orange-600 to-orange-800" },
                  ][index]
                  const rowClass = index === 0
                    ? "bg-amber-50/50 hover:bg-amber-50"
                    : index === 1
                      ? "bg-slate-50/60 hover:bg-slate-50"
                      : index === 2
                        ? "bg-orange-50/30 hover:bg-orange-50/60"
                        : "hover:bg-slate-50/70"
                  return (
                    <tr key={reseller.name} className={"border-b border-slate-100 transition-colors last:border-0 " + rowClass}>
                      <td className="px-5 py-4 sm:px-6">
                        {medal ? (
                          <span aria-label={"Rank " + (index + 1)} className="relative inline-flex h-14 w-12 items-start justify-center">
                            <span aria-hidden="true" className={"absolute left-[9px] top-8 z-0 h-7 w-3 " + medal.ribbon} style={{ clipPath: "polygon(0 0, 100% 0, 100% 100%, 50% 78%, 0 100%)" }} />
                            <span aria-hidden="true" className={"absolute right-[9px] top-8 z-0 h-7 w-3 " + medal.ribbon} style={{ clipPath: "polygon(0 0, 100% 0, 100% 100%, 50% 78%, 0 100%)" }} />
                            <span className={"relative z-10 flex size-11 flex-col items-center justify-center rounded-full border-2 bg-gradient-to-br font-bold shadow-md ring-4 " + medal.disc}>
                              <span aria-hidden="true" className="absolute inset-1 rounded-full border border-white/80 shadow-[inset_0_1px_2px_rgba(255,255,255,0.8)]" />
                              <span aria-hidden="true" className="absolute inset-1 rounded-full border border-white/80" />
                              <span className="relative text-[8px] font-semibold uppercase leading-none tracking-wide">No.</span>
                              <span className="relative text-lg leading-none">{index + 1}</span>
                            </span>
                          </span>
                        ) : (
                          <span aria-label={"Rank " + (index + 1)} className="inline-flex size-9 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold tabular-nums text-slate-600">{index + 1}</span>
                        )}
                      </td>
                      <td className="px-4 py-4">
                        <div className="flex items-center gap-3">
                          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-xs font-semibold text-slate-600 shadow-sm ring-1 ring-slate-200">{reseller.name.split(" ").map((part) => part[0]).join("").slice(0, 2)}</span>
                          <span className="font-semibold text-slate-800">{reseller.name}</span>
                        </div>
                      </td>
                      <td className="px-4 py-4 text-center tabular-nums text-slate-600">{reseller.likes}</td>
                      <td className="px-4 py-4 text-center tabular-nums text-slate-600">{reseller.shares}</td>
                      <td className="px-4 py-4 text-center tabular-nums text-slate-600">{reseller.comments}</td>
                      <td className="px-5 py-4 text-center sm:px-6">
                        <span className="inline-flex min-w-20 items-center justify-center gap-1.5 rounded-lg bg-white px-3 py-1.5 font-bold tabular-nums text-teal-800 shadow-sm ring-1 ring-slate-200/80">{reseller.score}<span className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">pts</span></span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 px-5 py-3 text-xs text-slate-500 sm:px-6">
            <span>Points are calculated automatically from engagement.</span>
            <div className="flex items-center gap-3"><span className="font-medium text-slate-600">Like × 1&nbsp; · &nbsp;Share × 2&nbsp; · &nbsp;Comment × 3</span><Button type="button" variant="ghost" size="sm" className="h-8 gap-1 px-2 font-semibold text-teal-800 hover:bg-teal-50 hover:text-teal-900" onClick={() => { if (resellerLeaderboard.length > 3) setShowAllResellers((current) => !current) }}>{showAllResellers ? "Show less" : "See more"}{showAllResellers ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}</Button></div>
          </div>
        </CardContent>
      </Card>
    )}
    {config.metrics&&<div className={`grid gap-4 sm:grid-cols-2 ${config.metrics.length>3?"xl:grid-cols-4":"xl:grid-cols-3"}`}>{config.metrics.map((metric)=><Card key={metric.label} className="border-slate-200/80 shadow-sm"><CardHeader className="pb-2"><CardDescription>{metric.label}</CardDescription><CardTitle className="text-2xl">{metric.value}</CardTitle></CardHeader>{metric.note&&<CardContent className="text-xs text-slate-500">{metric.note}</CardContent>}</Card>)}</div>}
    {config.title==="Performance analytics"&&<Card className="border-slate-200/80 shadow-sm"><CardHeader className="flex flex-row items-center justify-between"><CardTitle>Clicks &amp; conversions</CardTitle><select aria-label="Analytics period" value={filters[0]} onChange={event=>setFilters(current=>current.map((value,index)=>index===0?event.target.value:value))} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option>Last 30 days</option></select></CardHeader><CardContent><div className="flex h-56 items-end gap-2 border-b border-slate-100 px-1 sm:gap-4">{[[40,20],[55,35],[47,27],[72,52],[64,44],[83,63],[67,47],[94,74],[76,56],[86,66],[71,51],[100,80]].map(([clicks,conversions],index)=><div key={index} className="flex h-full flex-1 items-end justify-center gap-1"><span className="w-2/5 rounded-t bg-teal-700" style={{height:`${clicks}%`}}/><span className="w-2/5 rounded-t bg-amber-400" style={{height:`${conversions}%`}}/></div>)}</div><div className="mt-3 flex justify-between text-[10px] text-slate-400"><span>Sep 15</span><span>Sep 22</span><span>Sep 29</span><span>Oct 6</span><span>Oct 13</span></div><div className="mt-3 flex gap-5 text-xs text-slate-500"><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-teal-700"/>Clicks</span><span className="flex items-center gap-2"><i className="size-2.5 rounded-sm bg-amber-400"/>Conversions</span></div></CardContent></Card>}
    <Card className="border-slate-200/80 shadow-sm"><CardHeader><CardTitle>{config.title}</CardTitle><CardDescription>{config.description}</CardDescription></CardHeader><CardContent className="space-y-4"><div className="flex flex-col gap-3 lg:flex-row"><div className="relative min-w-[220px] flex-1"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400"/><Input type="search" aria-label={config.searchPlaceholder} placeholder={config.searchPlaceholder} value={query} onChange={(event)=>setQuery(event.target.value)} className="pl-9"/></div>{config.filters?.filter((filter)=>config.title!=="Performance analytics"||filter.column>=0).map((filter,index)=><select key={filter.label} aria-label={filter.label} value={filters[index]} onChange={(event)=>setFilters((current)=>current.map((value,i)=>i===index?event.target.value:value))} className="h-9 rounded-md border border-input bg-background px-3 text-sm"><option disabled>{filter.label}</option>{filter.values.map((value)=><option key={value}>{value}</option>)}</select>)}</div>
    <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead><tr className="border-b text-xs text-slate-500">{config.columns.map((column)=><th key={column} className="pb-3 pr-3 font-medium">{column}</th>)}</tr></thead><tbody>{filteredRows.map((row,rowIndex)=><tr key={`${row[0]}-${rowIndex}`} className="border-b align-top last:border-0">{row.map((cell,columnIndex)=><td key={`${columnIndex}-${cell}`} className="py-3.5 pr-3 text-slate-700">{columnIndex===config.statusColumn||badgeStyles[cell]?<Badge className={badgeStyles[cell]??"bg-slate-100 text-slate-700"}>{cell}</Badge>:columnIndex===row.length-1&&config.action==="review"?cell.split(" · ").map((action)=><Button key={action} size="sm" variant={action==="Approve"?"default":"outline"} className="mr-1 mb-1" disabled={cell==="Reviewed"} onClick={()=>reviewRow(rowIndex,action==="Approve")}>{cell==="Reviewed"?<Check className="size-3.5"/>:null}{cell==="Reviewed"?"Reviewed":action}</Button>):columnIndex===row.length-1&&config.action==="copy"?<Button size="sm" variant="outline" onClick={()=>copyValue(row[1])}>{copied===row[1]?<Check className="size-3.5"/>:<Copy className="size-3.5"/>}{copied===row[1]?"Copied":"Copy link"}</Button>:columnIndex===row.length-1&&config.action==="details"?config.title==="Resellers"?cell.split(" · ").map((action)=><Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`/tenant/reseller-detail?reseller=${encodeURIComponent(row[0].split(" Â· ")[0])}`}>{action}<ArrowUpRight className="size-3.5"/></Link>):(config.title.toLowerCase().includes("campaign")||config.title==="My campaigns")?cell.split(" · ").map((action)=>action==="View"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`${hrefPrefix}/campaign-detail?campaign=${encodeURIComponent(row[0])}${config.title==="My campaigns"?"&from=my-campaigns":""}`}>{action}<ArrowUpRight className="size-3.5"/></Link>:config.title==="My campaigns"&&action==="Tracking link"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href="/reseller/tracking-links">{action}<ArrowUpRight className="size-3.5"/></Link>:config.title==="My campaigns"&&action==="Get content"?<Link key={action} className="mr-2 inline-flex items-center gap-1 text-sm font-medium text-teal-800 hover:underline" href={`/reseller/campaign-content?campaign=${encodeURIComponent(row[0])}`}><Video className="size-3.5"/>{action}</Link>:<Button key={action} size="sm" variant="outline" className="mr-1" onClick={()=>performDetailAction(action,row)}>{action}</Button>):cell.split(" · ").map((action)=><Button key={action} size="sm" variant="outline" className="mr-1" onClick={()=>performDetailAction(action,row)}>{action}</Button>):cell}</td>)}</tr>)}</tbody></table>{filteredRows.length===0&&<p className="py-10 text-center text-sm text-slate-500">No records match the current search and filters.</p>}</div><div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500"><span>Showing {filteredRows.length} of {rows.length} sample records</span>{feedback&&<span role="status" className="text-emerald-800">{feedback}</span>}</div></CardContent></Card>
    {config.note&&<p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-900">{config.note}</p>}
  </>
}

