import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import RoomsPage from '../pages/RoomsPages'
import { fetchRooms, type RoomDTO } from '../services/roomService'
import { fetchHotelById } from '../services/hotelService'

vi.mock('../services/roomService', () => ({ fetchRooms: vi.fn() }))
vi.mock('../services/hotelService', () => ({ fetchHotelById: vi.fn() }))
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false, isAdmin: false, user: null, login: vi.fn(), logout: vi.fn() }),
}))

const room: RoomDTO = {
  id: 3, roomNumber: '301', type: 'Double', pricePerNight: 150, availability: true,
  description: 'd', capacity: 2, image: 'img.jpg', amenities: [], hotelId: 1,
}

describe('RoomsPages', () => {
  it('pide solo habitaciones activas y enlaza a /roomDetail/{id}', async () => {
    vi.mocked(fetchRooms).mockResolvedValue([room])
    vi.mocked(fetchHotelById).mockResolvedValue({
      id: 1, name: 'H', location: 'Centro', description: 'D', rating: 4, image: 'i', province: 'Salta',
    })
    render(<MemoryRouter><RoomsPage /></MemoryRouter>)

    const link = await screen.findByRole('link', { name: /more details/i })
    expect(fetchRooms).toHaveBeenCalledWith({ activeOnly: true })
    expect(link).toHaveAttribute('href', '/roomDetail/3')
  })
})
