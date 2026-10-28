import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Bookings from '../pages/Bookings'
import { cancelReservation, listMyReservations } from '../services/reservationService'
import { cancelledReservation, reservationFixture } from './reservationFixtures'

vi.mock('../components/Layouts/Header', () => ({ default: () => null }))
vi.mock('../services/reservationService', () => ({ listMyReservations: vi.fn(), cancelReservation: vi.fn() }))

const upcoming = reservationFixture()
const inStay = reservationFixture({
  id: 41, phase: 'IN_STAY', checkIn: '2026-10-27', checkOut: '2026-10-30',
  cancellation: { cancellable: false, refundable: false, refundDeadline: '2026-10-25T15:00:00', refundAmountIfCancelledNow: 0 },
})
const past = reservationFixture({
  id: 30, phase: 'COMPLETED', checkIn: '2026-09-01', checkOut: '2026-09-03', nights: 2,
  cancellation: { cancellable: false, refundable: false, refundDeadline: '2026-08-30T15:00:00', refundAmountIfCancelledNow: 0 },
})
const cancelled = cancelledReservation({ id: 35 })

function renderPage() {
  render(<MemoryRouter><Bookings /></MemoryRouter>)
}

describe('Bookings page', () => {
  afterEach(() => vi.resetAllMocks())

  it('groups reservations by phase with counters (CA-05.1)', async () => {
    vi.mocked(listMyReservations).mockResolvedValue([upcoming, inStay, past, cancelled])
    const user = userEvent.setup()
    renderPage()

    const upcomingTab = await screen.findByRole('tab', { name: 'Próximas (2)' })
    expect(upcomingTab).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Pasadas (1)' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Canceladas (1)' })).toBeInTheDocument()
    expect(within(screen.getByRole('tabpanel')).getAllByRole('article')).toHaveLength(2)

    await user.click(screen.getByRole('tab', { name: 'Pasadas (1)' }))
    expect(within(screen.getByRole('tabpanel')).getByText('Completada')).toBeInTheDocument()

    // Arrow keys move between tabs
    await user.keyboard('{ArrowRight}')
    expect(screen.getByRole('tab', { name: 'Canceladas (1)' })).toHaveFocus()
    expect(within(screen.getByRole('tabpanel')).getByText('Cancelada')).toBeInTheDocument()
  })

  it('shows the empty state with a link to the rooms (CA-05.8)', async () => {
    vi.mocked(listMyReservations).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Aún no tienes reservas')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Explorar habitaciones' })).toHaveAttribute('href', '/rooms')
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument()
  })

  it('shows loading and then a retry on error', async () => {
    vi.mocked(listMyReservations).mockRejectedValueOnce(new Error('boom')).mockResolvedValueOnce([upcoming])
    const user = userEvent.setup()
    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus reservas...')
    await user.click(await screen.findByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('tab', { name: 'Próximas (1)' })).toBeInTheDocument()
  })

  it('moves a cancelled reservation to "Canceladas" with the server result (CA-05.2)', async () => {
    vi.mocked(listMyReservations).mockResolvedValue([upcoming])
    vi.mocked(cancelReservation).mockResolvedValue(cancelledReservation())
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancelar reserva n.º 42' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirmar cancelación' }))

    expect(cancelReservation).toHaveBeenCalledWith(42)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Canceladas (1)' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Próximas (0)' })).toBeInTheDocument()
    expect(screen.getByText('Reserva n.º 42 cancelada. Recibirás un reembolso de $450.')).toBeInTheDocument()
  })
})
