import { describe, expect, it } from 'vitest'
import {
  addDays,
  buildMonthGrid,
  firstBlockedNightAfter,
  isRangeAvailable,
  nightsBetween,
} from '../components/calendar/calendarUtils'

const occupied = new Set(['2026-11-10', '2026-11-11', '2026-11-12'])

describe('buildMonthGrid', () => {
  it('empieza en lunes y rellena con null', () => {
    const weeks = buildMonthGrid('2026-11-01')
    expect(weeks[0]).toEqual([null, null, null, null, null, null, '2026-11-01'])
    expect(weeks.every((w) => w.length === 7)).toBe(true)
    expect(weeks.flat().filter(Boolean)).toHaveLength(30)
  })

  it('maneja febrero bisiesto 2028 (29 días, 1 feb es martes)', () => {
    const weeks = buildMonthGrid('2028-02-01')
    expect(weeks.flat().filter(Boolean)).toHaveLength(29)
    expect(weeks[0].indexOf('2028-02-01')).toBe(1)
    expect(weeks.flat()).toContain('2028-02-29')
  })

  it('febrero 2027 tiene 28 días', () => {
    expect(buildMonthGrid('2027-02-01').flat().filter(Boolean)).toHaveLength(28)
  })
})

describe('fechas', () => {
  it('suma días cruzando mes y año', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29')
  })
  it('cuenta noches', () => {
    expect(nightsBetween('2026-11-08', '2026-11-10')).toBe(2)
    expect(nightsBetween('2026-10-30', '2026-11-02')).toBe(3)
  })
})

describe('isRangeAvailable (reserva existente 10 -> 13 nov)', () => {
  it.each([
    ['2026-11-08', '2026-11-10', true],
    ['2026-11-13', '2026-11-15', true],
    ['2026-11-09', '2026-11-11', false],
    ['2026-11-10', '2026-11-11', false],
    ['2026-11-15', '2026-11-15', false],
    ['2026-11-15', '2026-11-14', false],
  ])('%s -> %s = %s', (checkIn, checkOut, expected) => {
    expect(isRangeAvailable(checkIn, checkOut, occupied)).toBe(expected)
  })

  it('respeta maxNights', () => {
    expect(isRangeAvailable('2026-12-01', '2026-12-31', new Set(), 30)).toBe(true)
    expect(isRangeAvailable('2026-12-01', '2027-01-01', new Set(), 30)).toBe(false)
  })
})

describe('firstBlockedNightAfter', () => {
  it('devuelve la primera noche ocupada desde el check-in', () => {
    expect(firstBlockedNightAfter('2026-11-08', occupied)).toBe('2026-11-10')
    expect(firstBlockedNightAfter('2026-11-11', occupied)).toBe('2026-11-11')
  })
  it('devuelve null si no hay ninguna', () => {
    expect(firstBlockedNightAfter('2026-11-13', occupied)).toBeNull()
  })
})
