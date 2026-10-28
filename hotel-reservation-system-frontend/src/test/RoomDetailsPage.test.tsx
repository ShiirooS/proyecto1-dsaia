import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RoomDetails from '../pages/RoomDetailsPage'
import { getAvailability, getRoomById, type RoomDTO } from '../services/roomService'
import { fetchHotelById } from '../services/hotelService'

vi.mock('../services/roomService', () => ({ getRoomById: vi.fn(), getAvailability: vi.fn() }))
vi.mock('../services/hotelService', () => ({ fetchHotelById: vi.fn() }))
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false, isAdmin: false, user: null, login: vi.fn(), logout: vi.fn() }),
}))

const room: RoomDTO = {
  id: 3, roomNumber: '301', type: 'Double', pricePerNight: 150, availability: true,
  description: 'Vista al mar', capacity: 2, image: 'img.jpg', amenities: [], hotelId: 1,
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/roomDetail/3']}>
      <Routes>
        <Route path="/roomDetail/:id" element={<RoomDetails />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RoomDetailsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 28, 12))
    vi.mocked(getRoomById).mockResolvedValue(room)
    vi.mocked(fetchHotelById).mockResolvedValue({
      id: 1, name: 'H', location: 'Centro', description: 'D', rating: 4, image: 'i', province: 'Salta',
    })
    vi.mocked(getAvailability).mockResolvedValue({
      roomId: 3, from: '2026-10-01', to: '2026-11-30', today: '2026-10-28', checkInTime: '15:00',
      maxNights: 30, occupiedDates: ['2026-11-10', '2026-11-11', '2026-11-12'],
    })
  })
  afterEach(() => {
    // Desmontar antes de resetear los mocks: si no, un re-render tardío llama a getAvailability ya sin valor
    cleanup()
    vi.useRealTimers()
    vi.resetAllMocks()
  })

  it('noAvailableFlagText', async () => {
    renderPage()
    expect(await screen.findByRole('button', { name: /^13 de noviembre de 2026/ })).toBeInTheDocument()
    expect(screen.queryByText(/not available/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/^available$/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Availability' })).not.toBeInTheDocument()
  })

  it('bookNowIsDisabledUntilValidRangeThenOpensModalWithDates', async () => {
    const user = userEvent.setup()
    renderPage()
    const book = await screen.findByRole('button', { name: 'Book now' })
    expect(book).toBeDisabled()
    expect(screen.getAllByRole('option')).toHaveLength(2)

    await user.click(await screen.findByRole('button', { name: /^20 de noviembre de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^23 de noviembre de 2026/ }))
    expect(screen.getByText('3 noches · $450')).toBeInTheDocument()
    expect(book).toBeEnabled()

    await user.click(book)
    expect(screen.getByRole('heading', { name: 'Confirm Booking' })).toBeInTheDocument()
    expect(screen.getByText('2026-11-20')).toBeInTheDocument()
    expect(screen.getByText('2026-11-23')).toBeInTheDocument()
    expect(screen.getByText('$450')).toBeInTheDocument()
    expect(screen.queryByLabelText('Check-in')).not.toBeInTheDocument()
  })
})
