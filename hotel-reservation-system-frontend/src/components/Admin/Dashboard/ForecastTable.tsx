import type { DemandForecast, InventoryAction } from '../../../services/analyticsService';
import { INVENTORY_LABELS, formatRate, formatSignedPct, longMonthLabel } from './forecastUtils';

interface ForecastTableProps {
  forecast: DemandForecast['forecast'];
  averagePricePerNight: number;
}

const ACTION_CLASSES: Record<InventoryAction, string> = {
  INCREASE: 'bg-orange-100 text-orange-800',
  KEEP: 'bg-gray-100 text-gray-700',
  REDUCE: 'bg-cyan-100 text-cyan-800',
};

const pctClass = (pct: number) => (pct > 0 ? 'text-orange-700' : pct < 0 ? 'text-cyan-700' : 'text-gray-600');

/** CA-03.1: one row per forecast month with the price and inventory suggestion (decision support only). */
export default function ForecastTable({ forecast, averagePricePerNight }: ForecastTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <caption className="mb-2 text-left text-gray-600">
          Sugerencias por mes sobre un precio medio actual de ${averagePricePerNight.toFixed(2)} por noche
        </caption>
        <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
          <tr>
            <th scope="col" className="px-3 py-2">Mes</th>
            <th scope="col" className="px-3 py-2 text-right">Ocupación prevista</th>
            <th scope="col" className="px-3 py-2 text-right">Noches previstas</th>
            <th scope="col" className="px-3 py-2 text-right">Ya reservadas</th>
            <th scope="col" className="px-3 py-2 text-right">Ajuste de precio</th>
            <th scope="col" className="px-3 py-2 text-right">Precio sugerido</th>
            <th scope="col" className="px-3 py-2">Inventario</th>
            <th scope="col" className="px-3 py-2 text-right">Habitaciones recomendadas</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {forecast.map((row) => (
            <tr key={row.month}>
              <th scope="row" className="whitespace-nowrap px-3 py-2 text-left font-medium capitalize text-gray-900">
                {longMonthLabel(row.month)}
              </th>
              <td className="px-3 py-2 text-right tabular-nums">{formatRate(row.forecastOccupancyRate)}</td>
              <td className="px-3 py-2 text-right tabular-nums">
                {row.forecastRoomNights} / {row.availableRoomNights}
              </td>
              <td className="px-3 py-2 text-right tabular-nums">{row.bookedRoomNights}</td>
              <td className={`px-3 py-2 text-right font-semibold tabular-nums ${pctClass(row.priceAdjustmentPct)}`}>
                {formatSignedPct(row.priceAdjustmentPct)}
              </td>
              <td className="px-3 py-2 text-right tabular-nums">${row.suggestedPricePerNight}</td>
              <td className="px-3 py-2">
                <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${ACTION_CLASSES[row.inventoryAction]}`}>
                  {INVENTORY_LABELS[row.inventoryAction]}
                </span>
              </td>
              <td className="px-3 py-2 text-right tabular-nums">{row.recommendedActiveRooms}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
