import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import { storeToken } from '../api/client'
import ProtectedRoute from '../components/ProtectedRoute'
import type { User } from '../types/auth'
import { adminUser, guestUser, jsonResponse, mockFetch } from './helpers'

function LoginProbe() {
  const { pathname, search } = useLocation()
  return <p>Login {pathname}{search}</p>
}

function renderAdminRoute(me?: User) {
  if (me) {
    storeToken('tok')
    mockFetch().mockResolvedValue(jsonResponse(200, me))
  }
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route path="/admin" element={<ProtectedRoute role="ADMIN"><p>Panel secreto</p></ProtectedRoute>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('redirige a /login con redirect cuando no hay sesión', () => {
    renderAdminRoute()
    expect(screen.getByText('Login /login?redirect=%2Fadmin')).toBeInTheDocument()
  })

  it('bloquea a un USER en una ruta ADMIN', async () => {
    renderAdminRoute(guestUser)
    expect(await screen.findByText(/403/)).toBeInTheDocument()
    expect(screen.queryByText('Panel secreto')).not.toBeInTheDocument()
  })

  it('deja pasar a un ADMIN', async () => {
    renderAdminRoute(adminUser)
    expect(await screen.findByText('Panel secreto')).toBeInTheDocument()
  })
})
