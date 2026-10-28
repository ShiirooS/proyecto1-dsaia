import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CancelReservationModal from '../components/Modals/CancelReservationModal'
import { cancelReservation } from '../services/reservationService'
import { cancelledReservation, lateReservation, reservationFixture } from './reservationFixtures'

vi.mock('../services/reservationService', () => ({ cancelReservation: vi.fn() }))

function apiError(status: number, code: string) {
  const headers = new AxiosHeaders()
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', { headers }, null, {
    status, statusText: '', headers, config: { headers }, data: { message: 'x', code },
  })
}

function renderModal(reservation = reservationFixture()) {
  const props = { onClose: vi.fn(), onCancelled: vi.fn(), onStale: vi.fn() }
  render(<CancelReservationModal reservation={reservation} {...props} />)
  return props
}

describe('CancelReservationModal', () => {
  afterEach(() => {
    vi.mocked(cancelReservation).mockReset()
  })

  it('announces the full refund and confirms against the API (CA-05.2)', async () => {
    const cancelled = cancelledReservation()
    vi.mocked(cancelReservation).mockResolvedValue(cancelled)
    const { onCancelled } = renderModal()

    expect(screen.getByRole('dialog', { name: '¿Cancelar la reserva n.º 42?' })).toBeInTheDocument()
    expect(screen.getByText('Recibirás un reembolso total de $450')).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Confirmar cancelación' }))

    expect(cancelReservation).toHaveBeenCalledWith(42)
    expect(onCancelled).toHaveBeenCalledWith(cancelled)
  })

  it('warns that there is no refund under 48 h (CA-05.3)', () => {
    renderModal(lateReservation())
    expect(screen.getByText('Faltan menos de 48 h para el check-in: no habrá reembolso')).toBeInTheDocument()
    expect(screen.queryByText(/reembolso total/)).not.toBeInTheDocument()
  })

  it('is keyboard friendly: focus on the safe option, focus trapped, Escape closes', async () => {
    const user = userEvent.setup()
    const { onClose } = renderModal()
    const keep = screen.getByRole('button', { name: 'Mantener reserva' })
    const confirm = screen.getByRole('button', { name: 'Confirmar cancelación' })

    expect(keep).toHaveFocus()
    await user.tab()
    expect(confirm).toHaveFocus()
    await user.tab()
    expect(keep).toHaveFocus()
    await user.tab({ shift: true })
    expect(confirm).toHaveFocus()

    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalled()
    expect(cancelReservation).not.toHaveBeenCalled()
  })

  it('explains a 409 by code and asks the page to reload', async () => {
    vi.mocked(cancelReservation).mockRejectedValue(apiError(409, 'RESERVATION_NOT_CANCELLABLE'))
    const { onCancelled, onStale } = renderModal()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Confirmar cancelación' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya no se puede cancelar: llegó la hora de check-in.')
    expect(onStale).toHaveBeenCalled()
    expect(onCancelled).not.toHaveBeenCalled()
  })

  it('keeps the list as is on a network error', async () => {
    vi.mocked(cancelReservation).mockRejectedValue(new Error('Network Error'))
    const { onStale } = renderModal()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Confirmar cancelación' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cancelar la reserva. Inténtalo de nuevo.')
    expect(onStale).not.toHaveBeenCalled()
  })
})
