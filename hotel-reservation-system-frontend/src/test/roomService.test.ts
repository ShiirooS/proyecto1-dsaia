import { AxiosAdapter } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import api from '../services/axiosConfig'
import { fetchRooms, getAvailability } from '../services/roomService'

function mockAdapter(status: number, data: unknown) {
  const adapter = vi.fn<AxiosAdapter>(async (config) => ({ data, status, statusText: '', headers: {}, config }))
  api.defaults.adapter = adapter
  return adapter
}

describe('roomService', () => {
  it('fetchRooms convierte un 204 sin cuerpo en []', async () => {
    mockAdapter(204, '')
    expect(await fetchRooms({ activeOnly: true })).toEqual([])
  })

  it('fetchRooms envía activeOnly solo cuando se pide', async () => {
    const adapter = mockAdapter(200, [])
    await fetchRooms()
    await fetchRooms({ activeOnly: true })
    expect(adapter.mock.calls[0][0].params).toBeUndefined()
    expect(adapter.mock.calls[1][0].params).toEqual({ activeOnly: true })
  })

  it('getAvailability pide /get-availability/{id} con from y to', async () => {
    const adapter = mockAdapter(200, { occupiedDates: [] })
    await getAvailability(3, '2026-11-01', '2026-11-30')
    const [config] = adapter.mock.calls[0]
    expect(config.url).toBe('/api/room/get-availability/3')
    expect(config.params).toEqual({ from: '2026-11-01', to: '2026-11-30' })
  })
})
