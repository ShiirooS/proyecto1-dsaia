import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import { TOKEN_KEY } from '../api/client'
import Login from '../pages/Login'
import { guestUser, jsonResponse, mockFetch } from './helpers'

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login?redirect=/products']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/products" element={<p>Pantalla de habitaciones</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('Login', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('muestra el mensaje del servidor en 401', async () => {
    mockFetch().mockResolvedValue(jsonResponse(401, { message: 'Usuario o contraseña incorrectos' }))
    renderLogin()
    await userEvent.type(screen.getByLabelText('Usuario o email'), 'huesped')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala')
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    expect(await screen.findByText('Usuario o contraseña incorrectos')).toHaveAttribute('role', 'alert')
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })

  it('con éxito guarda el token y redirige a redirect', async () => {
    mockFetch().mockResolvedValue(jsonResponse(200, { token: 'tok123', user: guestUser }))
    renderLogin()
    await userEvent.type(screen.getByLabelText('Usuario o email'), 'huesped')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Huesped123')
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    await waitFor(() => expect(screen.getByText('Pantalla de habitaciones')).toBeInTheDocument())
    expect(localStorage.getItem(TOKEN_KEY)).toBe('tok123')
  })
})
