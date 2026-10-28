import type { IsoDate } from '../../services/roomService'

export type OccupiedNights = ReadonlySet<IsoDate>

const MS_PER_DAY = 86_400_000
const pad = (n: number, width = 2) => String(n).padStart(width, '0')

const toUtcMs = (date: IsoDate): number => {
  const [year, month, day] = date.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}

const fromUtcMs = (ms: number): IsoDate => {
  const d = new Date(ms)
  return `${pad(d.getUTCFullYear(), 4)}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`
}

export const compareDates = (a: IsoDate, b: IsoDate): number => (a < b ? -1 : a > b ? 1 : 0)

export const addDays = (date: IsoDate, days: number): IsoDate => fromUtcMs(toUtcMs(date) + days * MS_PER_DAY)

export const nightsBetween = (checkIn: IsoDate, checkOut: IsoDate): number =>
  Math.round((toUtcMs(checkOut) - toUtcMs(checkIn)) / MS_PER_DAY)

export const monthStart = (date: IsoDate): IsoDate => `${date.slice(0, 7)}-01`

export function addMonths(monthStartDate: IsoDate, months: number): IsoDate {
  const [year, month] = monthStartDate.split('-').map(Number)
  const index = year * 12 + (month - 1) + months
  return `${pad(Math.floor(index / 12), 4)}-${pad((index % 12) + 1)}-01`
}

export const lastDayOfMonth = (date: IsoDate): IsoDate => addDays(addMonths(monthStart(date), 1), -1)

/** Semanas (lunes primero) de un mes; las celdas fuera del mes son `null`. */
export function buildMonthGrid(monthStartDate: IsoDate): (IsoDate | null)[][] {
  const first = monthStart(monthStartDate)
  const daysInMonth = Number(lastDayOfMonth(first).slice(8))
  const leadingBlanks = (new Date(toUtcMs(first)).getUTCDay() + 6) % 7
  const cells: (IsoDate | null)[] = [
    ...Array<null>(leadingBlanks).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => addDays(first, i)),
  ]
  while (cells.length % 7 !== 0) cells.push(null)
  return Array.from({ length: cells.length / 7 }, (_, week) => cells.slice(week * 7, week * 7 + 7))
}

/** Primera noche ocupada igual o posterior al check-in, o `null` si no hay. */
export function firstBlockedNightAfter(checkIn: IsoDate, occupied: OccupiedNights): IsoDate | null {
  let first: IsoDate | null = null
  for (const night of occupied) {
    if (night >= checkIn && (first === null || night < first)) first = night
  }
  return first
}

/** Una estancia [checkIn, checkOut) es válida si ninguna de sus noches está ocupada y respeta maxNights. */
export function isRangeAvailable(
  checkIn: IsoDate,
  checkOut: IsoDate,
  occupied: OccupiedNights,
  maxNights = Number.POSITIVE_INFINITY,
): boolean {
  const nights = nightsBetween(checkIn, checkOut)
  if (nights < 1 || nights > maxNights) return false
  const blocked = firstBlockedNightAfter(checkIn, occupied)
  return blocked === null || blocked >= checkOut
}

export const isSelectableAsCheckIn = (date: IsoDate, today: IsoDate, occupied: OccupiedNights): boolean =>
  date >= today && date <= addDays(today, 365) && !occupied.has(date)

export const isSelectableAsCheckOut = (
  date: IsoDate,
  checkIn: IsoDate,
  occupied: OccupiedNights,
  maxNights: number,
): boolean => isRangeAvailable(checkIn, date, occupied, maxNights)

const longDateFormat = new Intl.DateTimeFormat('es', {
  timeZone: 'UTC',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})
export const formatLongDate = (date: IsoDate): string => longDateFormat.format(toUtcMs(date))

const monthTitleFormat = new Intl.DateTimeFormat('es', { timeZone: 'UTC', month: 'long', year: 'numeric' })
export const formatMonthTitle = (date: IsoDate): string => monthTitleFormat.format(toUtcMs(date))

export const localTodayIso = (): IsoDate => {
  const now = new Date()
  return `${pad(now.getFullYear(), 4)}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

export type DayState = 'past' | 'occupied' | 'available'

export const dayState = (date: IsoDate, today: IsoDate, occupied: OccupiedNights): DayState =>
  date < today ? 'past' : occupied.has(date) ? 'occupied' : 'available'

export const RANGE_ERROR_MESSAGES = {
  OCCUPIED: 'El rango incluye noches ocupadas',
  MAX_NIGHTS: 'Máximo 30 noches',
  INVALID: 'Selecciona un check-out posterior al check-in',
} as const

export type RangeError = keyof typeof RANGE_ERROR_MESSAGES

export type RangeValidation = { ok: true; nights: number } | { ok: false; error: RangeError }

/** `[checkIn, checkOut)` es válido si cada noche está libre; el check-out sí puede caer en un día ocupado. */
export function validateRange(
  checkIn: IsoDate,
  checkOut: IsoDate,
  today: IsoDate,
  occupied: OccupiedNights,
  maxNights: number,
): RangeValidation {
  const nights = nightsBetween(checkIn, checkOut)
  if (nights < 1 || !isSelectableAsCheckIn(checkIn, today, new Set())) return { ok: false, error: 'INVALID' }
  if (nights > maxNights) return { ok: false, error: 'MAX_NIGHTS' }
  const blocked = firstBlockedNightAfter(checkIn, occupied)
  if (blocked !== null && blocked < checkOut) return { ok: false, error: 'OCCUPIED' }
  return { ok: true, nights }
}
