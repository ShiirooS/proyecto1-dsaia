import { apiGet, apiPost } from './client'
import type { AuthResponse, LoginPayload, RegisterPayload, User } from '../types/auth'

export const register = (payload: RegisterPayload) => apiPost<AuthResponse>('/api/auth/register', payload)
export const login = (payload: LoginPayload) => apiPost<AuthResponse>('/api/auth/login', payload)
export const me = () => apiGet<User>('/api/auth/me')
