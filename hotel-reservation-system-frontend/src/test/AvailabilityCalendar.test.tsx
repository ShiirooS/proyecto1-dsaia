import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import AvailabilityCalendar, { type DateRange } from '../components/calendar/AvailabilityCalendar'
import { getAvailability, type RoomAvailability } from '../services/roomService'

vi.mock('../services/roomService', () => ({ getAvailability: vi.fn() }))

const availability: RoomAvailability = {
  roomId: 3,
  from: '2026-10-01',
  to: '2026-11-30',
  today: '2026-10-28',
  checkInTime: '15:00',
  maxNights: 30,
  occupiedDates: ['2026-11-10', '2026-11-11', '2026-11-12'],
}

function Harness() {
  const [value, setValue] = useState<DateRange>({})
  return <AvailabilityCalendar roomId={3} pricePerNight={150} value={value} onChange={setValue} />
}

const day = (text: string, month = 'noviembre') =>
  screen.findByRole('button', { name: new RegExp(`^${text} de ${month} de 2026`) })

describe('AvailabilityCalendar', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 28, 12))
    vi.mocked(getAvailability).mockResolvedValue(availability)
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.resetAllMocks()
  })

  it('occupiedDaysAreDisabled', async () => {
    render(<Harness />)
    for (const d of ['10', '11', '12']) {
      const button = await day(d)
      expect(button).toBeDisabled()
      expect(button).toHaveAccessibleName(`${d} de noviembre de 2026, ocupado`)
    }
    const free = await day('13')
    expect(free).toBeEnabled()
    expect(free).toHaveAccessibleName('13 de noviembre de 2026, disponible')
    expect(vi.mocked(getAvailability)).toHaveBeenCalledWith(3, '2026-10-01', '2026-11-30')
  })

  it('pastDaysAreDisabled', async () => {
    render(<Harness />)
    // Esperar al nombre completo: el botón existe antes de que termine de cargar la disponibilidad
    const past = await screen.findByRole('button', { name: '27 de octubre de 2026, no disponible' })
    expect(past).toBeDisabled()
    expect(await day('28', 'octubre')).toBeEnabled()
  })

  it('selectsValidRangeAndShowsTotal', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.click(await day('20'))
    expect(await day('20')).toHaveAttribute('aria-pressed', 'true')
    await user.click(await day('23'))
    expect(await day('23')).toHaveAccessibleName('23 de noviembre de 2026, salida seleccionada')
    expect(screen.getByText('3 noches · $450')).toBeInTheDocument()
  })

  it('crossingOccupiedNightsShowsMessageAndCheckoutOnOccupiedDayIsValid', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.click(await day('8'))
    await user.click(await day('12'))
    expect(screen.getByRole('alert')).toHaveTextContent('El rango incluye noches ocupadas')
    expect(await day('8')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByText(/noches ·/)).not.toBeInTheDocument()
    await user.click(await day('10'))
    expect(screen.getByText('2 noches · $300')).toBeInTheDocument()
  })

  it('clickBeforeCheckInRestartsSelection', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.click(await day('15'))
    await user.click(await day('9'))
    expect(await day('9')).toHaveAttribute('aria-pressed', 'true')
    expect(await day('15')).toHaveAttribute('aria-pressed', 'false')
  })

  it('nextMonthRequestsVisibleRange', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await day('10')
    await user.click(screen.getByRole('button', { name: 'Mes siguiente' }))
    await day('1', 'diciembre')
    expect(vi.mocked(getAvailability)).toHaveBeenLastCalledWith(3, '2026-11-01', '2026-12-31')
  })

  it('prevDisabledOnCurrentMonth', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await day('10')
    expect(screen.getByRole('button', { name: 'Mes anterior' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Mes siguiente' }))
    await day('1', 'diciembre')
    expect(screen.getByRole('button', { name: 'Mes anterior' })).toBeEnabled()
  })

  it('showsRetryOnError', async () => {
    const user = userEvent.setup()
    vi.mocked(getAvailability).mockRejectedValueOnce(new Error('network')).mockResolvedValue(availability)
    render(<Harness />)
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar la disponibilidad')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await day('13')).toBeEnabled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
