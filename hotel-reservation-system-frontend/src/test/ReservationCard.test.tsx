import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import ReservationCard from '../components/Bookings/ReservationCard'
import { cancelledReservation, lateReservation, reservationFixture } from './reservationFixtures'

describe('ReservationCard', () => {
  it('shows hotel, room, dates, nights, guests, total and the card used (CA-05.1)', () => {
    render(<ReservationCard reservation={reservationFixture()} onCancel={vi.fn()} />)
    expect(screen.getByRole('heading', { name: 'Hotel Mar Azul' })).toBeInTheDocument()
    expect(screen.getByText('Habitación 203 · DOBLE')).toBeInTheDocument()
    expect(screen.getByText('20/12/2026 → 23/12/2026 · 3 noches')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('$450 · VISA •••• 4242')).toBeInTheDocument()
    expect(screen.getByText('Confirmada')).toBeInTheDocument()
  })

  it('shows the free cancellation deadline (CA-05.9)', () => {
    render(<ReservationCard reservation={reservationFixture()} onCancel={vi.fn()} />)
    expect(screen.getByText('Cancelación gratuita hasta el 18/12/2026 15:00')).toBeInTheDocument()
  })

  it('warns that a late cancellation has no refund', () => {
    render(<ReservationCard reservation={lateReservation()} onCancel={vi.fn()} />)
    expect(screen.getByText(/no habrá reembolso/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar reserva n.º 42' })).toBeInTheDocument()
  })

  it('hides "Cancelar" when the reservation is no longer cancellable (CA-05.4)', () => {
    const inStay = reservationFixture({
      phase: 'IN_STAY',
      cancellation: { cancellable: false, refundable: false, refundDeadline: '2026-12-18T15:00:00', refundAmountIfCancelledNow: 0 },
    })
    render(<ReservationCard reservation={inStay} onCancel={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /Cancelar/ })).not.toBeInTheDocument()
    expect(screen.getByText('En curso')).toBeInTheDocument()
  })

  it('shows when a cancelled reservation was cancelled and what was refunded', () => {
    render(<ReservationCard reservation={cancelledReservation()} onCancel={vi.fn()} />)
    expect(screen.getByText('Cancelada')).toBeInTheDocument()
    expect(screen.getByText('Cancelada el 01/12/2026 09:15. Reembolsado: $450')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Cancelar/ })).not.toBeInTheDocument()
  })

  it('asks the page to cancel this reservation', async () => {
    const onCancel = vi.fn()
    const reservation = reservationFixture()
    render(<ReservationCard reservation={reservation} onCancel={onCancel} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Cancelar reserva n.º 42' }))
    expect(onCancel).toHaveBeenCalledWith(reservation)
  })
})
