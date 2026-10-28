export type Role = 'USER' | 'ADMIN'

export interface User {
  id: number
  username: string
  email: string
  fullName: string
  phone: string | null
  role: Role
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
  fullName: string
  phone?: string
}

export interface LoginPayload {
  username: string
  password: string
}
