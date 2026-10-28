import { describe, expect, it } from 'vitest'
import {
  addDays,
  buildMonthGrid,
  dayState,
  firstBlockedNightAfter,
  nightsBetween,
  validateRange,
} from '../components/calendar/calendarUtils'

const today = '2026-10-28'
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
  })
})

describe('fechas', () => {
  it('suma días cruzando mes y año', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
  })
  it('cuenta noches', () => {
    expect(nightsBetween('2026-10-30', '2026-11-02')).toBe(3)
  })
  it('firstBlockedNightAfter', () => {
    expect(firstBlockedNightAfter('2026-11-08', occupied)).toBe('2026-11-10')
    expect(firstBlockedNightAfter('2026-11-13', occupied)).toBeNull()
  })
})

describe('dayState', () => {
  it.each([
    ['2026-10-27', 'past'],
    ['2026-10-28', 'available'],
    ['2026-11-09', 'available'],
    ['2026-11-10', 'occupied'],
    ['2026-11-12', 'occupied'],
    ['2026-11-13', 'available'],
  ])('%s -> %s', (date, expected) => {
    expect(dayState(date, today, occupied)).toBe(expected)
  })
})

describe('validateRange', () => {
  it('CA-02.5: el check-out puede caer en un día ocupado', () => {
    expect(validateRange('2026-11-08', '2026-11-10', today, occupied, 30)).toEqual({ ok: true, nights: 2 })
  })

  it('CA-02.4: rango libre de 3 noches', () => {
    expect(validateRange('2026-11-20', '2026-11-23', today, occupied, 30)).toEqual({ ok: true, nights: 3 })
  })

  it('CA-02.6: rechaza un rango que atraviesa noches ocupadas', () => {
    expect(validateRange('2026-11-08', '2026-11-12', today, occupied, 30)).toEqual({ ok: false, error: 'OCCUPIED' })
    expect(validateRange('2026-11-10', '2026-11-11', today, occupied, 30)).toEqual({ ok: false, error: 'OCCUPIED' })
  })

  it('CA-02.7: acepta 30 noches y rechaza 31', () => {
    expect(validateRange('2026-12-01', '2026-12-31', today, new Set(), 30)).toEqual({ ok: true, nights: 30 })
    expect(validateRange('2026-12-01', '2027-01-01', today, new Set(), 30)).toEqual({ ok: false, error: 'MAX_NIGHTS' })
  })

  it('rechaza check-out no posterior, check-in pasado o más allá de 365 días', () => {
    expect(validateRange('2026-11-15', '2026-11-15', today, occupied, 30)).toEqual({ ok: false, error: 'INVALID' })
    expect(validateRange('2026-10-27', '2026-10-29', today, occupied, 30)).toEqual({ ok: false, error: 'INVALID' })
    expect(validateRange('2027-10-30', '2027-11-01', today, new Set(), 30)).toEqual({ ok: false, error: 'INVALID' })
  })
})
