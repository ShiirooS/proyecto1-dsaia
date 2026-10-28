import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import Register from '../pages/Register'
import { jsonResponse, mockFetch } from './helpers'

function renderRegister() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <Register />
      </AuthProvider>
    </MemoryRouter>,
  )
}

async function fill(values: { name: string; user: string; email: string; pass: string }) {
  await userEvent.type(screen.getByLabelText('Nombre completo'), values.name)
  await userEvent.type(screen.getByLabelText('Usuario'), values.user)
  await userEvent.type(screen.getByLabelText('Email'), values.email)
  await userEvent.type(screen.getByLabelText('Contraseña'), values.pass)
  await userEvent.click(screen.getByRole('button', { name: 'Registrarse' }))
}

describe('Register', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('valida en cliente contraseña débil y email inválido sin llamar a fetch', async () => {
    const fetchMock = mockFetch()
    renderRegister()
    await fill({ name: 'Ana', user: 'ana', email: 'no-es-email', pass: 'corta' })
    expect(screen.getByText('Ingresa un email válido')).toHaveAttribute('role', 'alert')
    expect(screen.getByText(/Entre 8 y 72 caracteres/)).toHaveAttribute('role', 'alert')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('muestra el mensaje 409 del servidor', async () => {
    mockFetch().mockResolvedValue(jsonResponse(409, { message: 'El email ya está registrado' }))
    renderRegister()
    await fill({ name: 'Ana', user: 'ana', email: 'ana@hotel.com', pass: 'Clave1234' })
    expect(await screen.findByText('El email ya está registrado')).toHaveAttribute('role', 'alert')
    expect(screen.getByRole('button', { name: 'Registrarse' })).toBeEnabled()
  })
})
