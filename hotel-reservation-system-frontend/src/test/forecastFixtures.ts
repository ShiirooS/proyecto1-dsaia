import type { DemandForecast, InventoryAction } from '../services/analyticsService'

const ADR_004_SEASONAL = [-0.095, -0.105, -0.015, -0.025, 0.065, 0.155, 0.245, 0.235, 0.025, -0.085, -0.195, -0.205]
const ADR_004_FORECAST: [string, number, number, number, number, InventoryAction, number][] = [
  ['2027-01', 0.69, 214, 0, 100, 'KEEP', 10],
  ['2027-02', 0.69, 193, 0, 100, 'KEEP', 10],
  ['2027-03', 0.79, 245, 8, 108, 'KEEP', 10],
  ['2027-04', 0.79, 237, 8, 108, 'KEEP', 10],
  ['2027-05', 0.89, 276, 15, 115, 'INCREASE', 12],
  ['2027-06', 0.99, 297, 15, 115, 'INCREASE', 13],
  ['2027-07', 1, 310, 15, 115, 'INCREASE', 13],
  ['2027-08', 1, 310, 15, 115, 'INCREASE', 13],
  ['2027-09', 0.89, 267, 15, 115, 'INCREASE', 12],
  ['2027-10', 0.79, 245, 8, 108, 'KEEP', 10],
  ['2027-11', 0.69, 207, 0, 100, 'KEEP', 10],
  ['2027-12', 0.69, 214, 0, 100, 'KEEP', 10],
]
const YEAR_1 = [0.45, 0.45, 0.55, 0.55, 0.65, 0.75, 0.85, 0.85, 0.65, 0.55, 0.45, 0.45]

/** Normative case of ADR-004 §5.1 (clock 2027-01-15, R = 10, P̄ = 100) as the API would return it. */
export function forecastFixture(horizon = 6): DemandForecast {
  const history = Array.from({ length: 24 }, (_, i) => {
    const year = i < 12 ? 2025 : 2026
    const month = String((i % 12) + 1).padStart(2, '0')
    const days = new Date(Date.UTC(year, i % 12 + 1, 0)).getUTCDate()
    const occupancyRate = YEAR_1[i % 12] + (i < 12 ? 0 : 0.12)
    return {
      month: `${year}-${month}`,
      occupiedRoomNights: Math.round(occupancyRate * 10 * days),
      availableRoomNights: 10 * days,
      occupancyRate,
      trendRate: Math.round((0.535 + 0.01 * (i + 1)) * 10000) / 10000,
    }
  })
  return {
    generatedAt: '2027-01-15T10:00:00',
    hotelId: null,
    horizon,
    activeRooms: 10,
    averagePricePerNight: 100,
    historyFrom: '2025-01',
    historyTo: '2026-12',
    model: {
      type: 'LINEAR_TREND_MONTHLY_SEASONALITY',
      intercept: 0.535,
      slope: 0.01,
      seasonalIndices: ADR_004_SEASONAL.map((index, i) => ({ month: i + 1, index })),
    },
    history,
    forecast: ADR_004_FORECAST.slice(0, horizon).map(([month, occ, nights, pct, price, action, rooms], i) => ({
      month,
      forecastOccupancyRate: occ,
      forecastRoomNights: nights,
      availableRoomNights: i === 1 ? 280 : 310,
      bookedRoomNights: i === 0 ? 35 : 0,
      bookedOccupancyRate: i === 0 ? 0.1129 : 0,
      priceAdjustmentPct: pct,
      suggestedPricePerNight: price,
      inventoryAction: action,
      recommendedActiveRooms: rooms,
    })),
  }
}
