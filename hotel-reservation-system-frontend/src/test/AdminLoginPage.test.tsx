import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthProvider from '../contexts/AuthContext'
import LoginAdmin from '../pages/AdminLoginPage'
import * as authService from '../services/authService'
import * as userService from '../services/userService'

vi.mock('../services/userService', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../services/axiosConfig', () => ({ default: {} }))

function renderLogin() {
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/admin/login']}>
        <Routes>
          <Route path="/admin/login" element={<LoginAdmin />} />
          <Route path="/admin/dashboard" element={<p>panel admin</p>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

async function submit() {
  await userEvent.type(screen.getByLabelText('Username'), 'u@correo.com')
  await userEvent.type(screen.getByLabelText('Password'), 'Clave1234')
  await userEvent.click(screen.getByRole('button', { name: 'Access' }))
}

describe('AdminLoginPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('rejectsNonAdmin: con un USER muestra el aviso, cierra la sesión y no navega', async () => {
    vi.spyOn(authService, 'login').mockImplementation(async () => {
      localStorage.setItem('token', 't')
      return { token: 't' }
    })
    vi.mocked(userService.getCurrentUser).mockResolvedValue({ id: 2, username: 'u', role: 'USER' })
    renderLogin()
    await submit()
    expect(await screen.findByRole('alert')).toHaveTextContent('Esta cuenta no tiene permisos de administrador')
    expect(screen.queryByText('panel admin')).not.toBeInTheDocument()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('con un ADMIN navega al dashboard', async () => {
    vi.spyOn(authService, 'login').mockResolvedValue({ token: 't' })
    vi.mocked(userService.getCurrentUser).mockResolvedValue({ id: 1, username: 'a', role: 'ADMIN' })
    renderLogin()
    await submit()
    await waitFor(() => expect(screen.getByText('panel admin')).toBeInTheDocument())
  })

  it('en 401 muestra credenciales incorrectas', async () => {
    vi.spyOn(authService, 'login').mockRejectedValue(new authService.LoginError(401))
    renderLogin()
    await submit()
    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña incorrectos')
  })
})
