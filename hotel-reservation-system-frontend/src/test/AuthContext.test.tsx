import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import AuthProvider, { useAuth } from '../contexts/AuthContext'
import * as userService from '../services/userService'

vi.mock('../services/userService', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../services/axiosConfig', () => ({ default: {} }))

function Probe() {
  const { loading, isAuthenticated, role } = useAuth()
  return <p>{loading ? 'cargando' : `auth=${isAuthenticated} role=${role}`}</p>
}

describe('AuthContext', () => {
  it('limpia la sesión si /api/user/current responde 401', async () => {
    localStorage.setItem('token', 'expirado')
    vi.mocked(userService.getCurrentUser).mockRejectedValue({ response: { status: 401 } })
    render(<AuthProvider><Probe /></AuthProvider>)
    expect(screen.getByText('cargando')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('auth=false role=null')).toBeInTheDocument())
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('carga el usuario y su rol con un token válido', async () => {
    localStorage.setItem('token', 'ok')
    vi.mocked(userService.getCurrentUser).mockResolvedValue({ id: 1, username: 'a', role: 'ADMIN' })
    render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(screen.getByText('auth=true role=ADMIN')).toBeInTheDocument())
  })
})
