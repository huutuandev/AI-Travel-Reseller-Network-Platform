import React from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Server, Users, Globe, Activity } from "lucide-react";

export default function PlatformAdminDashboard() {
  return (
    <DashboardLayout role="platform-admin">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-navy-900">Platform Overview</h1>
          <p className="text-slate-500 text-sm">System health, tenants, and global metrics.</p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline">System Logs</Button>
          <Button variant="accent">Onboard New Tenant</Button>
        </div>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 mb-8">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Active Tenants</CardTitle>
            <Globe className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">24</div>
            <p className="text-xs text-green-600 mt-1">+2 this month</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Total Resellers</CardTitle>
            <Users className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">3,492</div>
            <p className="text-xs text-green-600 mt-1">+145 this week</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Global GMV</CardTitle>
            <Activity className="h-4 w-4 text-slate-400" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">4.2B ₫</div>
            <p className="text-xs text-slate-500 mt-1">Platform-wide last 30 days</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">API Health</CardTitle>
            <Server className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-navy-900">99.98%</div>
            <p className="text-xs text-slate-500 mt-1">All systems operational</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tenant Management</CardTitle>
          <CardDescription>Recent merchants and businesses using the platform.</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tenant Name</TableHead>
                <TableHead>Subdomain</TableHead>
                <TableHead>Plan</TableHead>
                <TableHead>Resellers</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[
                { name: "SunWorld Parks", domain: "sunworld.aitravel.com", plan: "Enterprise", users: "142", status: "ACTIVE", s_var: "success" },
                { name: "VinWonders", domain: "vinwonders.aitravel.com", plan: "Enterprise", users: "285", status: "ACTIVE", s_var: "success" },
                { name: "Hoi An Memories", domain: "memories.aitravel.com", plan: "Pro", users: "56", status: "ACTIVE", s_var: "success" },
                { name: "Danang Local Tours", domain: "dntours.aitravel.com", plan: "Starter", users: "12", status: "PENDING_SETUP", s_var: "warning" },
                { name: "Saigon River Cruise", domain: "sgcruise.aitravel.com", plan: "Pro", users: "0", status: "SUSPENDED", s_var: "danger" },
              ].map((tenant, i) => (
                <TableRow key={i}>
                  <TableCell className="font-medium text-navy-900">{tenant.name}</TableCell>
                  <TableCell className="text-slate-500">{tenant.domain}</TableCell>
                  <TableCell>{tenant.plan}</TableCell>
                  <TableCell>{tenant.users}</TableCell>
                  <TableCell>
                    <Badge variant={tenant.s_var as any}>{tenant.status}</Badge>
                  </TableCell>
                  <TableCell>
                    <Button variant="ghost" size="sm">Manage</Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </DashboardLayout>
  );
}
