import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ConfirmBookingModal from '../components/Modals/ConfirmBookingModal'
import { getPaymentMethods, type PaymentMethod } from '../services/paymentMethodService'
import { bookReservation, type MyReservation } from '../services/reservationService'
import type { RoomDTO } from '../services/roomService'

vi.mock('../services/paymentMethodService', () => ({ getPaymentMethods: vi.fn() }))
vi.mock('../services/reservationService', () => ({ bookReservation: vi.fn() }))

const room: RoomDTO = {
  id: 3, roomNumber: '301', type: 'Double', pricePerNight: 150, availability: true,
  description: '', capacity: 2, image: 'img.jpg', amenities: [], hotelId: 1,
}
const booking = { room, checkIn: '2026-12-20', checkOut: '2026-12-23', guests: 2 }
const visa: PaymentMethod = { id: 5, brand: 'VISA', last4: '4242', expMonth: 12, expYear: 2030, holderName: 'ANA', isDefault: true, expired: false, createdAt: '' }
const reservation = {
  id: 42, status: 'CONFIRMED', checkIn: '2026-12-20', checkOut: '2026-12-23', guests: 2, totalAmount: 450,
  payment: { amount: 450, cardBrand: 'VISA', cardLast4: '4242' },
} as MyReservation

function apiError(status: number, code: string) {
  return new AxiosError('fail', undefined, undefined, undefined, {
    status, data: { code, message: 'ignored' }, statusText: '', headers: {}, config: { headers: new AxiosHeaders() },
  })
}

function renderModal() {
  const onRoomUnavailable = vi.fn()
  render(
    <MemoryRouter>
      <ConfirmBookingModal isOpen closeModal={vi.fn()} booking={booking} onRoomUnavailable={onRoomUnavailable} />
    </MemoryRouter>,
  )
  return { onRoomUnavailable }
}

describe('ConfirmBookingModal', () => {
  beforeEach(() => {
    vi.mocked(getPaymentMethods).mockResolvedValue([visa])
  })
  afterEach(() => vi.resetAllMocks())

  it('preselectsDefaultCardAndSubmitsPaymentMethodId', async () => {
    const user = userEvent.setup()
    vi.mocked(bookReservation).mockResolvedValue(reservation)
    renderModal()
    expect(await screen.findByText('3 noches · $450')).toBeInTheDocument()
    expect(await screen.findByRole('radio', { name: /VISA •••• 4242/ })).toBeChecked()
    expect(screen.queryByLabelText('CVV')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Pagar $450' }))
    expect(bookReservation).toHaveBeenCalledWith({ roomId: 3, checkIn: '2026-12-20', checkOut: '2026-12-23', guests: 2, paymentMethodId: 5 })
    expect(await screen.findByText('Reserva n.º 42')).toBeInTheDocument()
    expect(screen.getByText(/VISA •••• 4242/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver mis reservas' })).toHaveAttribute('href', '/bookings')
  })

  it('disablesPayButtonWhilePending', async () => {
    const user = userEvent.setup()
    vi.mocked(bookReservation).mockReturnValue(new Promise(() => {}))
    renderModal()
    const pay = await screen.findByRole('button', { name: 'Pagar $450' })
    await waitFor(() => expect(pay).toBeEnabled())
    await user.dblClick(pay)
    expect(pay).toBeDisabled()
    expect(bookReservation).toHaveBeenCalledTimes(1)
  })

  it('shows the declined message on 402', async () => {
    const user = userEvent.setup()
    vi.mocked(bookReservation).mockRejectedValue(apiError(402, 'PAYMENT_DECLINED'))
    renderModal()
    await user.click(await screen.findByRole('button', { name: 'Pagar $450' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El pago fue rechazado. Prueba con otra tarjeta.')
    expect(screen.getByRole('button', { name: 'Pagar $450' })).toBeEnabled()
  })

  it('on409ClosesAndRequestsCalendarReload', async () => {
    const user = userEvent.setup()
    vi.mocked(bookReservation).mockRejectedValue(apiError(409, 'ROOM_NOT_AVAILABLE'))
    const { onRoomUnavailable } = renderModal()
    await user.click(await screen.findByRole('button', { name: 'Pagar $450' }))
    await waitFor(() => expect(onRoomUnavailable).toHaveBeenCalledTimes(1))
  })

  it('sends saveCard true and the card for a new card', async () => {
    const user = userEvent.setup()
    vi.mocked(getPaymentMethods).mockResolvedValue([])
    vi.mocked(bookReservation).mockResolvedValue(reservation)
    renderModal()
    await user.type(await screen.findByLabelText('Número de tarjeta'), '5555555555554444')
    await user.type(screen.getByLabelText(/Vencimiento/), '0630')
    await user.type(screen.getByLabelText('CVV'), '321')
    await user.type(screen.getByLabelText('Titular'), 'Ana Pérez')
    await user.click(screen.getByLabelText('Guardar tarjeta para próximas compras'))
    await user.click(screen.getByRole('button', { name: 'Pagar $450' }))
    expect(bookReservation).toHaveBeenCalledWith({
      roomId: 3, checkIn: '2026-12-20', checkOut: '2026-12-23', guests: 2,
      card: { cardNumber: '5555555555554444', expMonth: 6, expYear: 2030, cvv: '321', holderName: 'Ana Pérez' },
      saveCard: true,
    })
  })

  it('does not call the API when the new card is invalid', async () => {
    const user = userEvent.setup()
    vi.mocked(getPaymentMethods).mockResolvedValue([])
    renderModal()
    await user.click(await screen.findByRole('button', { name: 'Pagar $450' }))
    expect(bookReservation).not.toHaveBeenCalled()
    expect(screen.getByText('Número de tarjeta inválido')).toBeInTheDocument()
  })
})
