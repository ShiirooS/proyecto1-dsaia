import { AxiosAdapter } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import api from '../services/axiosConfig'
import { createHotel, deleteHotel, updateHotel, HotelDTO } from '../services/hotelService'

const hotel: HotelDTO = { id: 1, name: 'H', location: 'L', description: 'D', rating: 5, image: 'i', province: 'P' }

describe('hotelService', () => {
  it('sendsBearerToken en crear, editar y borrar', async () => {
    localStorage.setItem('token', 'jwt-admin')
    const adapter = vi.fn<AxiosAdapter>(async (config) => ({
      data: {}, status: 200, statusText: 'OK', headers: {}, config,
    }))
    api.defaults.adapter = adapter
    await createHotel(hotel)
    await updateHotel(1, hotel)
    await deleteHotel(1)
    expect(adapter).toHaveBeenCalledTimes(3)
    for (const [config] of adapter.mock.calls) {
      expect(config.headers.get('Authorization')).toBe('Bearer jwt-admin')
    }
  })
})
