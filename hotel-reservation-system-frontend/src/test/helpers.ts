import { vi } from 'vitest'
import type { User } from '../types/auth'

export const adminUser: User = {
  id: 1,
  username: 'admin',
  email: 'admin@hotel.com',
  fullName: 'Admin',
  phone: null,
  role: 'ADMIN',
  createdAt: '2024-01-01T00:00:00',
}
export const guestUser: User = { ...adminUser, id: 2, username: 'huesped', fullName: 'Huésped', role: 'USER' }

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

export const mockFetch = () => vi.spyOn(globalThis, 'fetch')
