import React from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { BarChart3, Copy, Sparkles, TrendingUp, Wallet } from "lucide-react";

export default function ResellerDashboard() {
  return (
    <DashboardLayout role="reseller">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-navy-900">Welcome back, Minh Travel</h1>
          <p className="text-slate-500 text-sm">Here's how your affiliate links are performing today.</p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline">Withdraw Funds</Button>
          <Button variant="accent">
            <Sparkles className="w-4 h-4 mr-2" />
            Create AI Content
          </Button>
        </div>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Available Balance</CardTitle>
            <Wallet className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">2,450,000 ₫</div>
            <p className="text-xs text-slate-500 mt-1">Ready for withdrawal</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Pending Commission</CardTitle>
            <TrendingUp className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">850,000 ₫</div>
            <p className="text-xs text-slate-500 mt-1">Awaiting merchant approval</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Total Clicks</CardTitle>
            <BarChart3 className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">4,231</div>
            <p className="text-xs text-green-600 flex items-center mt-1">
              +15% this week
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Conversion Rate</CardTitle>
            <BarChart3 className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">3.4%</div>
            <p className="text-xs text-green-600 flex items-center mt-1">
              +0.2% this week
            </p>
          </CardContent>
        </Card>
      </div>

      <h2 className="text-lg font-semibold text-navy-900 mb-4">Recommended Campaigns</h2>
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 mb-8">
        {[
          { title: "Ba Na Hills Cable Car", merchant: "SunWorld", comm: "10%", price: "900,000 ₫", img: "bg-blue-100" },
          { title: "VinWonders Nam Hoi An", merchant: "VinWonders", comm: "8%", price: "1,200,000 ₫", img: "bg-red-100" },
          { title: "Hoi An Memories Show", merchant: "Hoi An Memories", comm: "15%", price: "1,500,000 ₫", img: "bg-amber-100" },
        ].map((campaign, i) => (
          <Card key={i} className="overflow-hidden flex flex-col">
            <div className={`h-32 ${campaign.img} flex items-center justify-center`}>
              <span className="text-slate-400 text-sm font-medium">Image Placeholder</span>
            </div>
            <CardHeader className="pb-4 flex-1">
              <div className="flex justify-between items-start mb-2">
                <Badge variant="secondary">{campaign.merchant}</Badge>
                <Badge variant="success">Earn {campaign.comm}</Badge>
              </div>
              <CardTitle className="text-lg">{campaign.title}</CardTitle>
              <CardDescription>Price: {campaign.price}</CardDescription>
            </CardHeader>
            <CardContent className="pt-0 flex gap-2">
              <Button variant="outline" className="flex-1">
                <Copy className="w-4 h-4 mr-2" />
                Copy Link
              </Button>
              <Button variant="accent" className="px-3" title="Generate AI Content">
                <Sparkles className="w-4 h-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </DashboardLayout>
  );
}
