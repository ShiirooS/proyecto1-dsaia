import type { DemandForecast, InventoryAction } from '../../../services/analyticsService';

export const MONTH_NAMES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
const LONG_MONTH_NAMES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

/** '2026-10' -> 'Oct 26' (axis label). */
export function shortMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-');
  return `${MONTH_NAMES[Number(month) - 1]} ${year.slice(2)}`;
}

/** '2026-10' -> 'octubre 2026'. */
export function longMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-');
  return `${LONG_MONTH_NAMES[Number(month) - 1]} ${year}`;
}

/** 0.69 -> '69.0 %'. */
export const formatRate = (rate: number): string => `${(rate * 100).toFixed(1)} %`;

/** 15 -> '+15 %', -10 -> '−10 %', 0 -> '0 %'. */
export const formatSignedPct = (pct: number): string => (pct > 0 ? `+${pct} %` : pct < 0 ? `−${Math.abs(pct)} %` : '0 %');

export const INVENTORY_LABELS: Record<InventoryAction, string> = {
  INCREASE: 'Aumentar',
  KEEP: 'Mantener',
  REDUCE: 'Reducir',
};

export interface ChartPoint {
  month: string;
  label: string;
  /** Percentages (0..100) so the axis and tooltips read naturally. */
  actual?: number;
  trend?: number;
  forecast?: number;
  booked?: number;
}

const toPct = (rate: number) => Math.round(rate * 1000) / 10;

/**
 * History (24 months) followed by the forecast. The trend line continues into the forecast months (a + b·t) and
 * the forecast line starts at the last real month so both segments join.
 */
export function buildChartData(data: DemandForecast): ChartPoint[] {
  const points: ChartPoint[] = data.history.map((h) => ({
    month: h.month,
    label: shortMonthLabel(h.month),
    actual: toPct(h.occupancyRate),
    trend: toPct(h.trendRate),
  }));
  if (points.length > 0) points[points.length - 1].forecast = points[points.length - 1].actual;
  data.forecast.forEach((f, i) => {
    const t = data.history.length + i + 1;
    points.push({
      month: f.month,
      label: shortMonthLabel(f.month),
      trend: toPct(data.model.intercept + data.model.slope * t),
      forecast: toPct(f.forecastOccupancyRate),
      booked: toPct(f.bookedOccupancyRate),
    });
  });
  return points;
}

export interface ForecastKpis {
  lastYearOccupancy: number;
  forecastOccupancy: number;
  peakMonth: string;
  peakOccupancy: number;
}

export function computeKpis(data: DemandForecast): ForecastKpis {
  const lastYear = data.history.slice(-12);
  const average = (values: number[]) => values.reduce((sum, v) => sum + v, 0) / Math.max(values.length, 1);
  const peak = data.forecast.reduce((best, f) => (f.forecastOccupancyRate > best.forecastOccupancyRate ? f : best), data.forecast[0]);
  return {
    lastYearOccupancy: average(lastYear.map((h) => h.occupancyRate)),
    forecastOccupancy: average(data.forecast.map((f) => f.forecastOccupancyRate)),
    peakMonth: peak ? longMonthLabel(peak.month) : '—',
    peakOccupancy: peak ? peak.forecastOccupancyRate : 0,
  };
}
