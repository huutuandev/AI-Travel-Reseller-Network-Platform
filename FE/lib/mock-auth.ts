export type UserRole = "platform-admin" | "tenant" | "reseller"

export type MockUser = {
  id: string
  name: string
  email: string
  password: string
  role: UserRole
  title: string
  tenantId?: string
  status: "active"
}

export type DemoSession = Omit<MockUser, "password">

// Development-only credentials. Replace this adapter with the real auth service when available.
export const MOCK_USERS: MockUser[] = [
  { id: "usr-platform-001", name: "Platform Owner", email: "admin@wayfarer.test", password: "AdminDemo2026!", role: "platform-admin", title: "Platform administrator", status: "active" },
  { id: "usr-tenant-001", name: "Avery Morgan", email: "avery@lantern-tide.test", password: "TenantDemo2026!", role: "tenant", title: "Tenant administrator", tenantId: "tenant-lantern-tide", status: "active" },
  { id: "usr-reseller-001", name: "Jordan Lee", email: "jordan@partner.test", password: "ResellerDemo2026!", role: "reseller", title: "Travel partner", status: "active" },
]

export const ROLE_HOME: Record<UserRole, string> = {
  "platform-admin": "/platform-admin/dashboard",
  tenant: "/dashboard",
  reseller: "/reseller/dashboard",
}

const SESSION_KEY = "wayfarer-demo-session"
const SESSION_EVENT = "wayfarer:session-change"

export function authenticate(email: string, password: string): DemoSession | null {
  const user = MOCK_USERS.find((candidate) => candidate.email.toLowerCase() === email.trim().toLowerCase() && candidate.password === password && candidate.status === "active")
  if (!user) return null
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
    title: user.title,
    status: user.status,
    ...(user.tenantId ? { tenantId: user.tenantId } : {}),
  }
}

export function getDemoSession(): DemoSession | null {
  if (typeof window === "undefined") return null
  try {
    const value = window.localStorage.getItem(SESSION_KEY)
    if (!value) return null
    const session = JSON.parse(value) as DemoSession
    return MOCK_USERS.some((user) => user.id === session.id && user.role === session.role) ? session : null
  } catch {
    return null
  }
}

export function getDemoSessionSnapshot(): string | null {
  if (typeof window === "undefined") return null
  return window.localStorage.getItem(SESSION_KEY) ?? "__anonymous__"
}

export function saveDemoSession(session: DemoSession): void {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new Event(SESSION_EVENT))
}

export function clearDemoSession(): void {
  window.localStorage.removeItem(SESSION_KEY)
  window.dispatchEvent(new Event(SESSION_EVENT))
}

export function subscribeDemoSession(callback: () => void): () => void {
  window.addEventListener(SESSION_EVENT, callback)
  window.addEventListener("storage", callback)
  return () => {
    window.removeEventListener(SESSION_EVENT, callback)
    window.removeEventListener("storage", callback)
  }
}
