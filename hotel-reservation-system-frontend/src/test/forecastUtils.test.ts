import { describe, expect, it } from 'vitest'
import {
  buildChartData,
  computeKpis,
  formatRate,
  formatSignedPct,
  longMonthLabel,
  shortMonthLabel,
} from '../components/Admin/Dashboard/forecastUtils'
import { forecastFixture } from './forecastFixtures'

describe('forecastUtils', () => {
  it('formats months, rates and signed percentages', () => {
    expect(shortMonthLabel('2026-10')).toBe('Oct 26')
    expect(longMonthLabel('2027-01')).toBe('enero 2027')
    expect(formatRate(0.69)).toBe('69.0 %')
    expect(formatRate(1)).toBe('100.0 %')
    expect(formatSignedPct(15)).toBe('+15 %')
    expect(formatSignedPct(-10)).toBe('−10 %')
    expect(formatSignedPct(0)).toBe('0 %')
  })

  it('joins history and forecast and extends the trend line (a + b·t)', () => {
    const data = buildChartData(forecastFixture(6))

    expect(data).toHaveLength(30)
    expect(data[0]).toMatchObject({ month: '2025-01', actual: 45, trend: 54.5 })
    // The dashed forecast starts at the last real month so both lines join
    expect(data[23]).toMatchObject({ month: '2026-12', actual: 57, forecast: 57 })
    expect(data[24]).toMatchObject({ month: '2027-01', forecast: 69, trend: 78.5, booked: 11.3 })
    expect(data[24].actual).toBeUndefined()
  })

  it('computes the KPIs from the real data', () => {
    const kpis = computeKpis(forecastFixture(12))
    expect(kpis.lastYearOccupancy).toBeCloseTo(0.72, 10)
    expect(kpis.peakMonth).toBe('julio 2027')
    expect(kpis.peakOccupancy).toBe(1)
  })
})
