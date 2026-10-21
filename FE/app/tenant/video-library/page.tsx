"use client"

import Image from "next/image"
import { useMemo, useRef, useState } from "react"
import { Film, Play, Search, Sparkles, Upload, Video } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { storefrontJourneys } from "@/lib/storefront-data"

type VideoSource = "AI generated" | "Business upload"
type VideoAsset = {
  id: string
  title: string
  campaign: string
  source: VideoSource
  updated: string
  duration: string
  image: string
  fileUrl?: string
}

const sampleVideos: VideoAsset[] = [
  { id: "v-01", title: "A weekend among the karsts", campaign: "Ha Long Bay Weekend Escape", source: "AI generated", updated: "Today · 10:42 AM", duration: "0:32", image: storefrontJourneys[0].image },
  { id: "v-02", title: "Lanterns along the river", campaign: "Hoi An Lantern Season", source: "AI generated", updated: "Yesterday · 3:18 PM", duration: "0:27", image: storefrontJourneys[1].image },
  { id: "v-03", title: "A slower kind of island stay", campaign: "Phu Quoc Island Escape", source: "Business upload", updated: "Oct 18 · 11:06 AM", duration: "1:14", image: storefrontJourneys[2].image },
  { id: "v-04", title: "The coast, just before sunset", campaign: "Ha Long Bay Weekend Escape", source: "Business upload", updated: "Oct 16 · 9:30 AM", duration: "0:48", image: storefrontJourneys[0].image },
]

export default function TenantVideoLibraryPage() {
  const fileInput = useRef<HTMLInputElement>(null)
  const [videos, setVideos] = useState(sampleVideos)
  const [filter, setFilter] = useState<"All videos" | VideoSource>("All videos")
  const [search, setSearch] = useState("")
  const [message, setMessage] = useState("")

  const filteredVideos = useMemo(() => videos.filter((video) => {
    const matchesSource = filter === "All videos" || video.source === filter
    const matchesSearch = `${video.title} ${video.campaign}`.toLowerCase().includes(search.trim().toLowerCase())
    return matchesSource && matchesSearch
  }), [filter, search, videos])

  function addVideos(files: FileList | null) {
    if (!files?.length) return
    const selected = Array.from(files).filter((file) => file.type.startsWith("video/"))
    if (!selected.length) {
      setMessage("Please choose a video file to add it to this preview library.")
      return
    }
    const additions = selected.map((file, index): VideoAsset => ({
      id: `${file.name}-${file.lastModified}-${index}`,
      title: file.name.replace(/\.[^/.]+$/, ""),
      campaign: "Business upload",
      source: "Business upload",
      updated: "Added just now",
      duration: "Local preview",
      image: storefrontJourneys[index % storefrontJourneys.length].image,
      fileUrl: URL.createObjectURL(file),
    }))
    setVideos((current) => [...additions, ...current])
    setFilter("All videos")
    setMessage(`${selected.length} video${selected.length === 1 ? "" : "s"} added to this browser preview.`)
  }

  const aiCount = videos.filter((video) => video.source === "AI generated").length
  const uploadCount = videos.filter((video) => video.source === "Business upload").length

  return <main className="mx-auto w-full max-w-[1440px] space-y-6 p-4 sm:p-7">
    <section className="relative overflow-hidden rounded-2xl border border-slate-200/80 bg-gradient-to-br from-white via-white to-emerald-50/80 p-5 shadow-sm sm:p-7">
      <div className="pointer-events-none absolute -right-10 -top-20 size-64 rounded-full bg-emerald-100/60 blur-3xl" />
      <div className="relative flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div className="max-w-2xl">
          <div className="mb-3 inline-flex size-11 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200/80"><Film className="size-5" /></div>
          <p className="text-xs font-semibold uppercase tracking-[.16em] text-emerald-800">Campaign media workspace</p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight text-slate-900">Video library</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">Keep company videos and AI campaign creatives together, ready to review and reuse.</p>
        </div>
        <div>
          <input ref={fileInput} type="file" accept="video/*" multiple className="sr-only" onChange={(event) => { addVideos(event.target.files); event.currentTarget.value = "" }} />
          <Button className="h-10 gap-2 bg-emerald-800 px-4 text-white hover:bg-emerald-900" onClick={() => fileInput.current?.click()}><Upload className="size-4" />Upload videos</Button>
        </div>
      </div>
      {message && <p role="status" className="relative mt-4 rounded-xl border border-emerald-200 bg-white/80 px-3 py-2 text-sm text-emerald-900">{message}</p>}
    </section>

    <section className="grid gap-4 sm:grid-cols-3" aria-label="Video library summary">
      {[{ label: "All videos", value: videos.length, icon: Video, detail: "In your media library" }, { label: "AI generated", value: aiCount, icon: Sparkles, detail: "Created for campaigns" }, { label: "Business uploads", value: uploadCount, icon: Upload, detail: "Added by your team" }].map(({ label, value, icon: Icon, detail }) => <Card key={label} className="border-slate-200/80 shadow-sm"><CardContent className="flex items-center justify-between gap-3 p-4 sm:p-5"><div><p className="text-sm font-medium text-slate-500">{label}</p><p className="mt-1 text-3xl font-semibold tracking-tight text-slate-900">{value}</p><p className="mt-1 text-xs text-slate-500">{detail}</p></div><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-emerald-50 text-emerald-800 ring-1 ring-emerald-100"><Icon className="size-5" /></span></CardContent></Card>)}
    </section>

    <section className="space-y-4">
      <div className="flex flex-col justify-between gap-3 lg:flex-row lg:items-center">
        <div><h2 className="text-xl font-semibold tracking-tight text-slate-900">Your videos</h2><p className="mt-1 text-sm text-slate-500">Browse creative assets linked to your campaigns.</p></div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <label className="relative block sm:w-72"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" /><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search videos or campaigns" className="h-10 bg-white pl-9" aria-label="Search videos or campaigns" /></label>
          <div className="flex flex-wrap gap-1 rounded-xl border border-slate-200 bg-white p-1" aria-label="Filter videos">
            {(["All videos", "AI generated", "Business upload"] as const).map((item) => <button key={item} type="button" onClick={() => setFilter(item)} aria-pressed={filter === item} className={`rounded-lg px-3 py-2 text-xs font-semibold transition-colors ${filter === item ? "bg-emerald-800 text-white shadow-sm" : "text-slate-600 hover:bg-slate-50"}`}>{item}</button>)}
          </div>
        </div>
      </div>

      {filteredVideos.length ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{filteredVideos.map((video) => <Card key={video.id} className="group overflow-hidden border-slate-200/80 bg-white shadow-sm transition duration-200 hover:-translate-y-1 hover:shadow-lg hover:shadow-slate-900/5">
        <div className="relative aspect-[16/9] overflow-hidden bg-slate-100">
          {video.fileUrl ? <video src={video.fileUrl} className="size-full object-cover" controls preload="metadata" aria-label={video.title} /> : <><Image src={video.image} alt="" fill sizes="(max-width: 640px) 100vw, (max-width: 1280px) 50vw, 33vw" className="object-cover transition duration-500 group-hover:scale-[1.03]" /><span className="absolute inset-0 grid place-items-center bg-slate-950/10"><span className="grid size-12 place-items-center rounded-full border border-white/50 bg-white/90 text-emerald-900 shadow-lg"><Play className="ml-0.5 size-5 fill-current" /></span></span></>}
          <span className={`absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1.5 text-[11px] font-semibold shadow-sm ${video.source === "AI generated" ? "bg-white/95 text-violet-800" : "bg-white/95 text-slate-700"}`}>{video.source === "AI generated" ? <Sparkles className="size-3.5" /> : <Upload className="size-3.5" />}{video.source}</span>
          {!video.fileUrl && <span className="absolute bottom-3 right-3 rounded-md bg-slate-950/75 px-2 py-1 text-xs font-medium text-white">{video.duration}</span>}
        </div>
        <CardContent className="space-y-3 p-4">
          <div><h3 className="truncate text-base font-semibold text-slate-900" title={video.title}>{video.title}</h3><p className="mt-1 truncate text-sm text-slate-500" title={video.campaign}>{video.campaign}</p></div>
          <div className="flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500"><span>{video.updated}</span><span className="rounded-full bg-emerald-50 px-2.5 py-1 font-medium text-emerald-800">Ready</span></div>
        </CardContent>
      </Card>)}</div> : <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-5 py-14 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-slate-100 text-slate-500"><Film className="size-5" /></span><h3 className="mt-4 font-semibold text-slate-900">No videos found</h3><p className="mt-1 text-sm text-slate-500">Try another search or choose a different video type.</p></div>}
    </section>

    <p className="text-center text-xs text-slate-400">Sample campaign videos are illustrative. Selected uploads are previewed in this browser only.</p>
  </main>
}
