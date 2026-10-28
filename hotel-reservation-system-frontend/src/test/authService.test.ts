import { afterEach, describe, expect, it, vi } from 'vitest'
import { getToken, login, logout } from '../services/authService'

// Pruebas de caracterización: fijan el comportamiento existente de developer antes de modificarlo
describe('authService (existente)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('login guarda el token JWT en localStorage', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ token: 'jwt-de-prueba' }), { status: 200 }),
    )
    await login('ana@correo.com', 'Clave1234')
    expect(getToken()).toBe('jwt-de-prueba')
    logout()
    expect(getToken()).toBeNull()
  })

  it('login lanza error si el backend responde con error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 403 }))
    await expect(login('ana@correo.com', 'mala')).rejects.toThrow('Login failed')
    expect(getToken()).toBeNull()
  })
})
