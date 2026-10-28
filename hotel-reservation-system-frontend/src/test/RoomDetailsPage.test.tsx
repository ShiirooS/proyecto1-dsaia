import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RoomDetails from '../pages/RoomDetailsPage'
import { getAvailability, getRoomById, type RoomDTO } from '../services/roomService'
import { fetchHotelById } from '../services/hotelService'
import { getPaymentMethods } from '../services/paymentMethodService'

vi.mock('../services/roomService', () => ({ getRoomById: vi.fn(), getAvailability: vi.fn() }))
vi.mock('../services/hotelService', () => ({ fetchHotelById: vi.fn() }))
const auth = vi.hoisted(() => ({ isAuthenticated: false }))
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: auth.isAuthenticated, isAdmin: false, user: null, login: vi.fn(), logout: vi.fn() }),
}))
vi.mock('../services/paymentMethodService', () => ({ getPaymentMethods: vi.fn() }))
vi.mock('../services/reservationService', () => ({ bookReservation: vi.fn() }))

const room: RoomDTO = {
  id: 3, roomNumber: '301', type: 'Double', pricePerNight: 150, availability: true,
  description: 'Vista al mar', capacity: 2, image: 'img.jpg', amenities: [], hotelId: 1,
}

function Where() {
  const { pathname, search } = useLocation()
  return <output data-testid="where">{pathname + search}</output>
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/roomDetail/3']}>
      <Where />
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
    vi.mocked(getPaymentMethods).mockResolvedValue([])
    vi.mocked(fetchHotelById).mockResolvedValue({
      id: 1, name: 'H', location: 'Centro', description: 'D', rating: 4, image: 'i', province: 'Salta',
    })
    vi.mocked(getAvailability).mockResolvedValue({
      roomId: 3, from: '2026-10-01', to: '2026-11-30', today: '2026-10-28', checkInTime: '15:00',
      maxNights: 30, occupiedDates: ['2026-11-10', '2026-11-11', '2026-11-12'],
    })
  })
  afterEach(() => {
    auth.isAuthenticated = false
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

  async function pickNovember20to23(user: ReturnType<typeof userEvent.setup>) {
    await user.click(await screen.findByRole('button', { name: /^20 de noviembre de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^23 de noviembre de 2026/ }))
  }

  it('bookNowIsDisabledUntilValidRangeThenOpensPaymentModal', async () => {
    auth.isAuthenticated = true
    const user = userEvent.setup()
    renderPage()
    const book = await screen.findByRole('button', { name: 'Book now' })
    expect(book).toBeDisabled()
    expect(screen.getAllByRole('option')).toHaveLength(2)

    await pickNovember20to23(user)
    expect(screen.getByText('3 noches · $450')).toBeInTheDocument()
    expect(book).toBeEnabled()

    await user.click(book)
    expect(screen.getByRole('heading', { name: 'Confirm Booking' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Pagar $450' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Check-in')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Name')).not.toBeInTheDocument()
  })

  it('bookNowWithoutSessionOpensLoginKeepingSelection', async () => {
    const user = userEvent.setup()
    renderPage()
    await pickNovember20to23(user)
    await user.click(screen.getByRole('button', { name: 'Book now' }))

    expect(screen.queryByRole('heading', { name: 'Confirm Booking' })).not.toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Welcome Back' })).toBeInTheDocument()
    const params = new URLSearchParams(screen.getByTestId('where').textContent?.split('?')[1])
    expect(params.get('login')).toBe('1')
    expect(params.get('checkIn')).toBe('2026-11-20')
    expect(params.get('checkOut')).toBe('2026-11-23')
    expect(params.get('guests')).toBe('1')
    expect(params.get('redirect')).toBe('/roomDetail/3?checkIn=2026-11-20&checkOut=2026-11-23&guests=1')
  })
})
