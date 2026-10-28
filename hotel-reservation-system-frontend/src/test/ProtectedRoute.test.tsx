import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useSearchParams } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { useAuth } from '../contexts/AuthContext'

vi.mock('../contexts/AuthContext', () => ({ useAuth: vi.fn() }))

type AuthState = Partial<ReturnType<typeof useAuth>>

function Home() {
  const [params] = useSearchParams()
  const login = params.get('login')
  return <p>{login ? `inicio con login=${login} redirect=${params.get('redirect')}` : 'inicio'}</p>
}

function renderRoute(state: AuthState, entry = '/admin/dashboard') {
  vi.mocked(useAuth).mockReturnValue(state as ReturnType<typeof useAuth>)
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/admin/login" element={<p>pantalla login</p>} />
        <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
          <Route path="/admin/dashboard" element={<p>panel admin</p>} />
        </Route>
        <Route element={<ProtectedRoute redirectPath="/?login=1&redirect=%2Fbookings" />}>
          <Route path="/bookings" element={<p>mis reservas</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('redirige al login a un anónimo', () => {
    renderRoute({ isAuthenticated: false, role: null, loading: false })
    expect(screen.getByText('pantalla login')).toBeInTheDocument()
  })

  it('userOnAdminRedirectsHome', () => {
    renderRoute({ isAuthenticated: true, role: 'USER', loading: false })
    expect(screen.getByText('inicio')).toBeInTheDocument()
    expect(screen.queryByText('panel admin')).not.toBeInTheDocument()
  })

  it('anonymousOnBookingsOpensLogin', () => {
    renderRoute({ isAuthenticated: false, role: null, loading: false }, '/bookings')
    expect(screen.getByText('inicio con login=1 redirect=/bookings')).toBeInTheDocument()
  })

  it('renderiza el contenido para un ADMIN', () => {
    renderRoute({ isAuthenticated: true, role: 'ADMIN', loading: false })
    expect(screen.getByText('panel admin')).toBeInTheDocument()
  })

  it('no redirige mientras carga', () => {
    renderRoute({ isAuthenticated: false, role: null, loading: true })
    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.queryByText('pantalla login')).not.toBeInTheDocument()
  })
})
