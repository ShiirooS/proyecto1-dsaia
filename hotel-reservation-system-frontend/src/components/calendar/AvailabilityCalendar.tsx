import { useState } from 'react'
import type { IsoDate } from '../../types/api'
import {
  addDays,
  addMonths,
  buildMonthGrid,
  formatLongDate,
  formatMonthTitle,
  isSelectableAsCheckIn,
  isSelectableAsCheckOut,
  localTodayIso,
  monthStart,
  type OccupiedNights,
} from './calendarUtils'
import { useAvailability } from './use-availability'

export interface DateRange {
  checkIn?: IsoDate
  checkOut?: IsoDate
}

interface AvailabilityCalendarProps {
  roomId: number | string
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

const navButton =
  'rounded-full px-3 py-1 text-xl text-gray-700 hover:bg-orange-100 focus:outline-none focus-visible:ring focus-visible:ring-orange-400 disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:bg-transparent'

const dayBase = 'h-10 w-full rounded-md text-sm focus:outline-none focus-visible:ring focus-visible:ring-orange-400 '

interface DayInfo {
  label: string
  selectable: boolean
  pressed: boolean
  className: string
}

function describeDay(
  date: IsoDate,
  today: IsoDate,
  occupied: OccupiedNights,
  maxNights: number,
  { checkIn, checkOut }: DateRange,
): DayInfo {
  const label = formatLongDate(date)
  const isStart = date === checkIn
  const isEnd = date === checkOut
  const inRange = checkIn !== undefined && checkOut !== undefined && date > checkIn && date < checkOut
  const canCheckOut =
    checkIn !== undefined && checkOut === undefined && isSelectableAsCheckOut(date, checkIn, occupied, maxNights)
  const awaitingCheckOut = checkIn !== undefined && checkOut === undefined
  const canRestart = (!awaitingCheckOut || date < checkIn) && isSelectableAsCheckIn(date, today, occupied)
  const selectable = isStart || isEnd || canCheckOut || canRestart

  if (isStart || isEnd) {
    return {
      label: `${label}, ${isStart ? 'entrada' : 'salida'} seleccionada`,
      selectable,
      pressed: true,
      className: `${dayBase}bg-orange-600 font-semibold text-white`,
    }
  }
  if (inRange) {
    return {
      label: `${label}, dentro de la estancia`,
      selectable,
      pressed: false,
      className: `${dayBase}bg-orange-100 text-orange-900 hover:bg-orange-200`,
    }
  }
  if (date < today) {
    return { label: `${label}, pasado`, selectable: false, pressed: false, className: `${dayBase}cursor-not-allowed bg-gray-100 text-gray-400` }
  }
  if (occupied.has(date)) {
    return {
      label: canCheckOut ? `${label}, ocupado, disponible como salida` : `${label}, ocupado`,
      selectable,
      pressed: false,
      className: `${dayBase}bg-red-50 text-red-600 line-through ${canCheckOut ? 'ring-1 ring-orange-400 hover:bg-red-100' : 'cursor-not-allowed'}`,
    }
  }
  if (!selectable) {
    return { label: `${label}, no seleccionable`, selectable, pressed: false, className: `${dayBase}cursor-not-allowed text-gray-300` }
  }
  return { label: `${label}, disponible`, selectable, pressed: false, className: `${dayBase}text-gray-900 hover:bg-orange-100` }
}

/**
 * Calendario de disponibilidad con selección en dos clics (entrada, salida). Se cargan siempre
 * dos meses y el segundo se oculta en móvil con CSS, para no depender de matchMedia.
 */
export default function AvailabilityCalendar({ roomId, value, onChange }: AvailabilityCalendarProps) {
  const [viewMonth, setViewMonth] = useState<IsoDate>(() => monthStart(value.checkIn ?? localTodayIso()))
  const { today, maxNights, occupied, loading, error, retry } = useAvailability(roomId, viewMonth)

  const todayMonth = today ? monthStart(today) : null
  if (todayMonth && viewMonth < todayMonth) setViewMonth(todayMonth)
  const lastMonth = today ? monthStart(addDays(today, 365)) : null
  const canGoBack = todayMonth !== null && viewMonth > todayMonth
  const canGoForward = lastMonth !== null && addMonths(viewMonth, 1) <= lastMonth
  const interactive = today !== null && !loading && !error

  const handleSelect = (date: IsoDate) => {
    const { checkIn, checkOut } = value
    if (checkIn !== undefined && checkOut === undefined && date > checkIn) {
      onChange({ checkIn, checkOut: date })
    } else if (date === checkIn && checkOut === undefined) {
      onChange({})
    } else {
      onChange({ checkIn: date })
    }
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <button type="button" aria-label="Mes anterior" className={navButton} disabled={!canGoBack} onClick={() => setViewMonth(addMonths(viewMonth, -1))}>
          <span aria-hidden="true">&lsaquo;</span>
        </button>
        <button type="button" aria-label="Mes siguiente" className={navButton} disabled={!canGoForward} onClick={() => setViewMonth(addMonths(viewMonth, 1))}>
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
                const day = describeDay(date, today, occupied, maxNights, value)
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
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-gray-200" />Pasado</li>
        <li className="flex items-center gap-1"><span className="h-3 w-3 rounded bg-orange-600" />Seleccionado</li>
      </ul>
    </div>
  )
}
