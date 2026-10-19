export type CustomerAccount = {
  id: string
  name: string
  email: string
  password: string
}

export type CustomerSession = Omit<CustomerAccount, "password">

export const CUSTOMER_DEMO_ACCOUNT: CustomerAccount = {
  id: "store-customer-demo",
  name: "Taylor Nguyen",
  email: "traveler@lantern-tide.test",
  password: "TravelDemo2026!",
}

const CUSTOMER_ACCOUNTS_KEY = "wayfarer-storefront-customer-accounts"
const CUSTOMER_SESSION_KEY = "wayfarer-storefront-customer-session"
const CUSTOMER_SESSION_EVENT = "wayfarer:customer-session-change"

function getAccounts(): CustomerAccount[] {
  try {
    const saved = window.localStorage.getItem(CUSTOMER_ACCOUNTS_KEY)
    const accounts = saved ? JSON.parse(saved) as CustomerAccount[] : []
    return [CUSTOMER_DEMO_ACCOUNT, ...accounts.filter((account) => account.id !== CUSTOMER_DEMO_ACCOUNT.id)]
  } catch {
    return [CUSTOMER_DEMO_ACCOUNT]
  }
}

function toSession(account: CustomerAccount): CustomerSession {
  return { id: account.id, name: account.name, email: account.email }
}

export function authenticateCustomer(email: string, password: string): CustomerSession | null {
  const account = getAccounts().find((candidate) => candidate.email.toLowerCase() === email.trim().toLowerCase() && candidate.password === password)
  return account ? toSession(account) : null
}

export function registerCustomer(name: string, email: string, password: string): { session?: CustomerSession; error?: string } {
  const accounts = getAccounts()
  if (accounts.some((account) => account.email.toLowerCase() === email.trim().toLowerCase())) {
    return { error: "An account with this email already exists. Sign in instead." }
  }
  const account: CustomerAccount = {
    id: `store-customer-${Date.now()}`,
    name: name.trim(),
    email: email.trim().toLowerCase(),
    password,
  }
  try {
    const savedAccounts = accounts.filter((entry) => entry.id !== CUSTOMER_DEMO_ACCOUNT.id)
    window.localStorage.setItem(CUSTOMER_ACCOUNTS_KEY, JSON.stringify([...savedAccounts, account]))
  } catch {
    return { error: "This browser could not save your demo account. Please check your storage settings and try again." }
  }
  const session = toSession(account)
  saveCustomerSession(session)
  return { session }
}

export function getCustomerSession(): CustomerSession | null {
  try {
    const value = window.localStorage.getItem(CUSTOMER_SESSION_KEY)
    if (!value) return null
    const session = JSON.parse(value) as CustomerSession
    const matchingAccount = getAccounts().find((account) => account.id === session.id && account.email === session.email)
    return matchingAccount ? toSession(matchingAccount) : null
  } catch {
    return null
  }
}

export function getCustomerSessionSnapshot(): string {
  try {
    return window.localStorage.getItem(CUSTOMER_SESSION_KEY) ?? "__anonymous__"
  } catch {
    return "__anonymous__"
  }
}

export function saveCustomerSession(session: CustomerSession): void {
  window.localStorage.setItem(CUSTOMER_SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new Event(CUSTOMER_SESSION_EVENT))
}

export function clearCustomerSession(): void {
  window.localStorage.removeItem(CUSTOMER_SESSION_KEY)
  window.dispatchEvent(new Event(CUSTOMER_SESSION_EVENT))
}

export function subscribeCustomerSession(callback: () => void): () => void {
  window.addEventListener(CUSTOMER_SESSION_EVENT, callback)
  window.addEventListener("storage", callback)
  return () => {
    window.removeEventListener(CUSTOMER_SESSION_EVENT, callback)
    window.removeEventListener("storage", callback)
  }
}
