import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet, apiPost, onUnauthorized, storeToken, TOKEN_KEY } from '../api/client'
import { jsonResponse, mockFetch } from './helpers'

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('agrega el header Bearer cuando hay token', async () => {
    storeToken('abc')
    const fetchMock = mockFetch().mockResolvedValue(jsonResponse(200, { ok: true }))
    await expect(apiGet('/api/x')).resolves.toEqual({ ok: true })
    const headers = fetchMock.mock.calls[0][1]?.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('no envía Authorization sin token y manda JSON en POST', async () => {
    const fetchMock = mockFetch().mockResolvedValue(jsonResponse(201, {}))
    await apiPost('/api/x', { a: 1 })
    const init = fetchMock.mock.calls[0][1]
    expect((init?.headers as Headers).has('Authorization')).toBe(false)
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe('{"a":1}')
  })

  it('lanza ApiError con status y message del body', async () => {
    mockFetch().mockImplementation(async () =>
      jsonResponse(409, { message: 'El email ya está registrado', code: 'EMAIL_ALREADY_EXISTS' }),
    )
    await expect(apiPost('/api/x', {})).rejects.toMatchObject({
      name: 'ApiError',
      status: 409,
      code: 'EMAIL_ALREADY_EXISTS',
      message: 'El email ya está registrado',
    })
    await expect(apiPost('/api/x', {})).rejects.toBeInstanceOf(ApiError)
  })

  it('expone los errores por campo de VALIDATION_ERROR', async () => {
    mockFetch().mockImplementation(async () =>
      jsonResponse(400, { message: 'Datos inválidos', code: 'VALIDATION_ERROR', errors: { email: 'no es válido' } }),
    )
    await expect(apiPost('/api/x', {})).rejects.toMatchObject({
      code: 'VALIDATION_ERROR',
      errors: { email: 'no es válido' },
    })
  })

  it('en 401 autenticado limpia el token y notifica', async () => {
    storeToken('abc')
    mockFetch().mockResolvedValue(jsonResponse(401, { message: 'Debes iniciar sesión para continuar' }))
    const listener = vi.fn()
    const off = onUnauthorized(listener)
    await expect(apiGet('/api/x')).rejects.toMatchObject({ status: 401 })
    expect(listener).toHaveBeenCalledOnce()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    off()
  })
})
