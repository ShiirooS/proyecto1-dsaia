import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import RoomDetails from '../pages/RoomDetails'
import { jsonResponse, mockFetch } from './helpers'

const room = {
  id: 1,
  roomNumber: '101',
  type: 'Suite',
  pricePerNight: 100,
  capacity: 3,
  imageUrl: null,
  description: 'Vista al mar',
  hotel: { id: 1, name: 'Hotel MN', location: 'Cartagena', description: null, rating: 4.5 },
}
const availability = {
  roomId: 1,
  from: '2026-11-01',
  to: '2026-12-31',
  today: '2026-11-05',
  checkInTime: '15:00',
  maxNights: 30,
  occupiedDates: ['2026-11-10', '2026-11-11', '2026-11-12'],
}

function renderPage(path = '/rooms/1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/rooms/:id" element={<RoomDetails />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('RoomDetails', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 5, 12))
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('calcula el total y habilita Reservar al elegir un rango válido', async () => {
    mockFetch().mockImplementation(async (input) =>
      String(input).includes('/availability') ? jsonResponse(200, availability) : jsonResponse(200, room),
    )
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByRole('heading', { name: /Suite · Habitación 101/ })).toBeInTheDocument()
    expect(screen.getByText('Vista al mar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reservar' })).toBeDisabled()

    await user.click(await screen.findByRole('button', { name: /^13 de noviembre de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^16 de noviembre de 2026/ }))
    await user.selectOptions(screen.getByLabelText('Huéspedes'), '2')

    expect(screen.getByText(/3 noches × \$100/)).toBeInTheDocument()
    expect(screen.getByTestId('total')).toHaveTextContent('$300')
    expect(screen.getByRole('link', { name: 'Reservar' })).toHaveAttribute(
      'href',
      '/rooms/1/checkout?checkIn=2026-11-13&checkOut=2026-11-16&guests=2',
    )
  })

  it('muestra "Habitación no encontrada" ante un 404', async () => {
    mockFetch().mockResolvedValue(jsonResponse(404, { code: 'ROOM_NOT_FOUND', message: 'x' }))
    renderPage('/rooms/99')
    expect(await screen.findByRole('heading', { name: 'Habitación no encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /listado/i })).toHaveAttribute('href', '/products')
  })
})
