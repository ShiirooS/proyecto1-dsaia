import { AxiosError, AxiosAdapter } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import api from '../services/axiosConfig'
import { UNAUTHORIZED_EVENT } from '../services/authEvents'

function respondWith(status: number, data: unknown) {
  const adapter: AxiosAdapter = async (config) => {
    const response = { data, status, statusText: '', headers: {}, config }
    throw new AxiosError(`HTTP ${status}`, 'ERR_BAD_REQUEST', config, null, response)
  }
  api.defaults.adapter = adapter
}

describe('interceptor de respuesta', () => {
  it('on401ClearsTokenAndEmitsLogout: un 401 con token borra el token y notifica', async () => {
    localStorage.setItem('token', 'expirado')
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    respondWith(401, { code: 'UNAUTHORIZED' })
    await expect(api.get('/api/user/current')).rejects.toBeInstanceOf(AxiosError)
    expect(localStorage.getItem('token')).toBeNull()
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('un 401 sin token no notifica', async () => {
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    respondWith(401, {})
    await expect(api.get('/x')).rejects.toBeInstanceOf(AxiosError)
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('un 403 propaga el message del backend y conserva el token', async () => {
    localStorage.setItem('token', 'valido')
    respondWith(403, { message: 'No tiene permisos para acceder a este recurso', code: 'FORBIDDEN' })
    await expect(api.get('/api/admin/x')).rejects.toThrow('No tiene permisos para acceder a este recurso')
    expect(localStorage.getItem('token')).toBe('valido')
  })
})
