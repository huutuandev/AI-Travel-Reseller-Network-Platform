export type MockBookingInput = {
  productKey: string
  productName: string
  firstName: string
  lastName: string
  email: string
  phone: string
  travelDate: string
  guests: number
  notes: string
  total: number
}

export type MockBooking = MockBookingInput & {
  reference: string
  createdAt: string
}

const BOOKINGS_KEY = "wayfarer-storefront-demo-bookings"

export function createMockBooking(input: MockBookingInput): MockBooking {
  const booking: MockBooking = {
    ...input,
    reference: `LT-${Date.now().toString(36).toUpperCase()}-${Math.random().toString(36).slice(2, 6).toUpperCase()}`,
    createdAt: new Date().toISOString(),
  }

  try {
    const saved = JSON.parse(window.localStorage.getItem(BOOKINGS_KEY) ?? "[]") as MockBooking[]
    window.localStorage.setItem(BOOKINGS_KEY, JSON.stringify([booking, ...saved]))
  } catch {
    // Confirmation remains available for private browsing or storage-disabled demos.
  }

  return booking
}
