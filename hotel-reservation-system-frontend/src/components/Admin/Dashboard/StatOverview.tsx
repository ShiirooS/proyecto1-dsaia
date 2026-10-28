import type { DemandForecast } from '../../../services/analyticsService';
import { computeKpis, formatRate } from './forecastUtils';

interface StatOverviewProps {
  data: DemandForecast;
}

/** Real KPIs of the forecast: recent occupancy, expected occupancy and peak month. */
export default function StatOverview({ data }: StatOverviewProps) {
  const kpis = computeKpis(data);
  const stats = [
    { label: 'Ocupación media (últimos 12 meses)', value: formatRate(kpis.lastYearOccupancy) },
    { label: `Ocupación prevista media (${data.horizon} meses)`, value: formatRate(kpis.forecastOccupancy) },
    { label: `Mes pico previsto (${formatRate(kpis.peakOccupancy)})`, value: kpis.peakMonth },
    { label: `Habitaciones activas · precio medio $${data.averagePricePerNight.toFixed(2)}`, value: String(data.activeRooms) },
  ];

  return (
    <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {stats.map((stat) => (
        <div key={stat.label} className="flex flex-col rounded-lg bg-white px-4 py-5 shadow-sm">
          <dt className="order-last mt-1 text-sm text-gray-500">{stat.label}</dt>
          <dd className="text-2xl font-bold capitalize text-gray-900">{stat.value}</dd>
        </div>
      ))}
    </dl>
  );
}
