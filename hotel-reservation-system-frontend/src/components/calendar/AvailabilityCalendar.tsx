import { useState } from 'react'
import type { IsoDate } from '../../services/roomService'
import {
  RANGE_ERROR_MESSAGES,
  addMonths,
  buildMonthGrid,
  dayState,
  formatLongDate,
  formatMonthTitle,
  isSelectableAsCheckIn,
  localTodayIso,
  monthStart,
  validateRange,
  type OccupiedNights,
  type RangeError,
} from './calendarUtils'
import { useAvailability } from './use-availability'

export interface DateRange {
  checkIn?: IsoDate
  checkOut?: IsoDate
}

interface AvailabilityCalendarProps {
  roomId: number | string
  pricePerNight: number
  value: DateRange
  onChange: (range: DateRange) => void
}

const WEEKDAYS = [
  { short: 'L', full: 'lunes' },
  { short: 'M', full: 'martes' },
  { short: 'X', full: 'miércoles' },
  { short: 'J', full: 'jueves' },
  { short: 'V', full: 'viernes' },
  { short: 'S', full: 'sábado' },
  { short: 'D', full: 'domingo' },
]

const STATE_LABEL = { past: 'no disponible', occupied: 'ocupado', available: 'disponible' } as const

const MONTHS_AHEAD = 12

const navButton =
  'rounded-full px-3 py-1 text-xl text-gray-700 hover:bg-orange-100 focus:outline-none focus-visible:ring focus-visible:ring-orange-400 disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:bg-transparent'

const dayBase = 'h-10 w-full rounded-md text-sm focus:outline-none focus-visible:ring focus-visible:ring-orange-400 '

interface DayInfo {
  label: string
  selectable: boolean
  pressed: boolean
  className: string
}

/**
 * Con la entrada fijada, cualquier fecha posterior es pulsable aunque esté ocupada: el intento
 * se rechaza con un mensaje (CA-02.6) y el check-out puede caer en un día ocupado (RN-02.3).
 */
function describeDay(date: IsoDate, today: IsoDate, occupied: OccupiedNights, { checkIn, checkOut }: DateRange): DayInfo {
  const state = dayState(date, today, occupied)
  const long = formatLongDate(date)
  const isStart = date === checkIn
  const isEnd = date === checkOut
  const inRange = checkIn !== undefined && checkOut !== undefined && date > checkIn && date < checkOut
  const awaitingCheckOut = checkIn !== undefined && checkOut === undefined
  const selectable =
    isStart || isEnd || (awaitingCheckOut && date > checkIn) || isSelectableAsCheckIn(date, today, occupied)

  if (isStart || isEnd) {
    return {
      label: `${long}, ${isStart ? 'entrada' : 'salida'} seleccionada`,
      selectable,
      pressed: true,
      className: `${dayBase}bg-orange-600 font-semibold text-white`,
    }
  }
  if (inRange) {
    return { label: `${long}, dentro de la estancia`, selectable, pressed: false, className: `${dayBase}bg-orange-100 text-orange-900 hover:bg-orange-200` }
  }
  const className =
    state === 'past'
      ? `${dayBase}cursor-not-allowed bg-gray-100 text-gray-400`
      : state === 'occupied'
        ? `${dayBase}bg-red-50 text-red-600 line-through ${selectable ? 'hover:bg-red-100' : 'cursor-not-allowed'}`
        : selectable
          ? `${dayBase}text-gray-900 hover:bg-orange-100`
          : `${dayBase}cursor-not-allowed text-gray-300`
  return { label: `${long}, ${STATE_LABEL[state]}`, selectable, pressed: false, className }
}

const formatStay = (nights: number, pricePerNight: number) =>
  `${nights} ${nights === 1 ? 'noche' : 'noches'} · $${nights * pricePerNight}`

/** Calendario de dos meses con selección en dos clics (entrada, salida). El segundo mes se oculta en móvil con CSS. */
export default function AvailabilityCalendar({ roomId, pricePerNight, value, onChange }: AvailabilityCalendarProps) {
  const [viewMonth, setViewMonth] = useState<IsoDate>(() => monthStart(value.checkIn ?? localTodayIso()))
  const [rangeError, setRangeError] = useState<RangeError | null>(null)
  const { today, maxNights, occupied, loading, error, retry } = useAvailability(roomId, viewMonth)

  const todayMonth = today ? monthStart(today) : null
  if (todayMonth && viewMonth < todayMonth) setViewMonth(todayMonth)
  const canGoBack = todayMonth !== null && viewMonth > todayMonth
  const canGoForward = todayMonth !== null && viewMonth < addMonths(todayMonth, MONTHS_AHEAD)
  const interactive = today !== null && !loading && !error

  const goToMonth = (month: IsoDate) => {
    setRangeError(null)
    setViewMonth(month)
  }

  const handleSelect = (date: IsoDate) => {
    if (!today) return
    const { checkIn, checkOut } = value
    if (checkIn !== undefined && checkOut === undefined && date > checkIn) {
      const result = validateRange(checkIn, date, today, occupied, maxNights)
      if (result.ok) {
        setRangeError(null)
        onChange({ checkIn, checkOut: date })
      } else {
        setRangeError(result.error)
      }
      return
    }
    setRangeError(null)
    onChange({ checkIn: date })
  }

  const stay =
    interactive && value.checkIn && value.checkOut
      ? validateRange(value.checkIn, value.checkOut, today, occupied, maxNights)
      : null

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <button type="button" aria-label="Mes anterior" className={navButton} disabled={!canGoBack} onClick={() => goToMonth(addMonths(viewMonth, -1))}>
          <span aria-hidden="true">&lsaquo;</span>
        </button>
        <button type="button" aria-label="Mes siguiente" className={navButton} disabled={!canGoForward} onClick={() => goToMonth(addMonths(viewMonth, 1))}>
          <span aria-hidden="true">&rsaquo;</span>
        </button>
      </div>

      {error && (
        <div role="alert" className="mb-3 flex items-center justify-between gap-3 rounded-md bg-red-50 p-3 text-sm text-red-700">
          <span>{error}</span>
          <button type="button" onClick={retry} className="rounded-md bg-orange-600 px-3 py-1 font-medium text-white hover:bg-orange-700">
            Reintentar
          </button>
        </div>
      )}
      {loading && <p role="status" className="mb-3 text-sm text-gray-500">Cargando disponibilidad...</p>}
      {rangeError && (
        <p role="alert" className="mb-3 rounded-md bg-red-50 p-3 text-sm text-red-700">
          {RANGE_ERROR_MESSAGES[rangeError]}
        </p>
      )}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2" aria-busy={loading}>
        {[viewMonth, addMonths(viewMonth, 1)].map((month, index) => (
          <section key={month} aria-labelledby={`cal-${roomId}-${month}`} className={index === 1 ? 'hidden md:block' : undefined}>
            <h3 id={`cal-${roomId}-${month}`} className="mb-2 text-center font-semibold capitalize text-gray-900">
              {formatMonthTitle(month)}
            </h3>
            <div className="grid grid-cols-7 gap-1 text-center text-xs text-gray-500">
              {WEEKDAYS.map((day) => (
                <abbr key={day.full} title={day.full} className="no-underline">{day.short}</abbr>
              ))}
            </div>
            <div className="mt-1 grid grid-cols-7 gap-1">
              {buildMonthGrid(month).flat().map((date, cell) => {
                if (date === null) return <span key={`blank-${cell}`} />
                if (!interactive) {
                  return (
                    <button key={date} type="button" disabled aria-label={formatLongDate(date)} className={`${dayBase}bg-gray-50 text-gray-300`}>
                      {Number(date.slice(8))}
                    </button>
                  )
                }
                const day = describeDay(date, today, occupied, value)
                return (
                  <button
                    key={date}
                    type="button"
                    aria-label={day.label}
                    aria-pressed={day.pressed}
                    disabled={!day.selectable}
                    onClick={() => handleSelect(date)}
                    className={day.className}
                  >
                    {Number(date.slice(8))}
                  </button>
                )
              })}
            </div>
          </section>
        ))}
      </div>

      <ul className="mt-4 flex flex-wrap gap-4 text-xs text-gray-600" aria-label="Leyenda">
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded border border-gray-300 bg-white" />Disponible</li>
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-red-100" /><span className="line-through">Ocupado</span></li>
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-gray-200" />No disponible</li>
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-orange-600" />Seleccionado</li>
      </ul>

      {stay?.ok && (
        <p role="status" className="mt-4 text-lg font-semibold text-gray-900">
          {formatStay(stay.nights, pricePerNight)}
        </p>
      )}
    </div>
  )
}
