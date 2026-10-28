import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import AvailabilityCalendar, { type DateRange } from '../components/calendar/AvailabilityCalendar'
import { jsonResponse, mockFetch } from './helpers'

const availability = {
  roomId: 1,
  from: '2026-11-01',
  to: '2026-12-31',
  today: '2026-11-05',
  checkInTime: '15:00',
  maxNights: 30,
  occupiedDates: ['2026-11-10', '2026-11-11', '2026-11-12'],
}

function Harness({ onChange }: { onChange: (range: DateRange) => void }) {
  const [value, setValue] = useState<DateRange>({})
  return (
    <AvailabilityCalendar
      roomId={1}
      value={value}
      onChange={(range) => {
        setValue(range)
        onChange(range)
      }}
    />
  )
}

const day = (text: string) => screen.findByRole('button', { name: new RegExp(`^${text} de noviembre de 2026`) })

describe('AvailabilityCalendar', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 5, 12))
    mockFetch().mockImplementation(async () => jsonResponse(200, availability))
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('pide la disponibilidad del rango visible', async () => {
    render(<Harness onChange={vi.fn()} />)
    await day('8')
    expect(fetch).toHaveBeenCalledWith('/api/rooms/1/availability?from=2026-11-01&to=2026-12-31', expect.anything())
  })

  it('deshabilita días pasados y ocupados', async () => {
    render(<Harness onChange={vi.fn()} />)
    const past = await screen.findByRole('button', { name: /^4 de noviembre de 2026, pasado/ })
    expect(past).toBeDisabled()
    const busy = await day('10')
    expect(busy).toBeDisabled()
    expect(busy).toHaveAccessibleName('10 de noviembre de 2026, ocupado')
    expect(await day('8')).toBeEnabled()
  })

  it('no permite un check-out que cruce una noche ocupada, pero sí el primer día ocupado', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<Harness onChange={onChange} />)

    await user.click(await day('8'))
    expect(onChange).toHaveBeenLastCalledWith({ checkIn: '2026-11-08' })
    expect(await day('9')).toBeEnabled()
    expect(await day('10')).toBeEnabled()
    expect(await day('11')).toBeDisabled()
    expect(await day('14')).toBeDisabled()

    await user.click(await day('10'))
    expect(onChange).toHaveBeenLastCalledWith({ checkIn: '2026-11-08', checkOut: '2026-11-10' })
    expect(await day('8')).toHaveAttribute('aria-pressed', 'true')
  })

  it('permite check-in el día de salida de otra reserva (13 nov)', async () => {
    render(<Harness onChange={vi.fn()} />)
    expect(await day('13')).toBeEnabled()
  })

  it('un clic en una fecha anterior al check-in reinicia la selección', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<Harness onChange={onChange} />)
    await user.click(await day('15'))
    await user.click(await day('7'))
    expect(onChange).toHaveBeenLastCalledWith({ checkIn: '2026-11-07' })
  })

  it('muestra el error y reintenta', async () => {
    const user = userEvent.setup()
    vi.restoreAllMocks()
    mockFetch()
      .mockResolvedValueOnce(jsonResponse(500, { message: 'boom' }))
      .mockImplementation(async () => jsonResponse(200, availability))
    render(<Harness onChange={vi.fn()} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar la disponibilidad')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await day('8')).toBeEnabled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
