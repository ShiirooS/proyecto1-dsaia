import { useCallback, useEffect, useRef, useState } from 'react';
import { getDemandForecast, type DemandForecast } from '../../../services/analyticsService';
import { toApiError } from '../../../services/apiError';
import { fetchHotels, type HotelDTO } from '../../../services/hotelService';
import ForecastChart from './ForecastChart';
import ForecastTable from './ForecastTable';
import SeasonalityChart from './SeasonalityChart';
import StatOverview from './StatOverview';
import { buildChartData, formatRate } from './forecastUtils';

const HORIZONS = [3, 6, 12];
const ALL_HOTELS = '';

type LoadState =
  | { status: 'loading' }
  | { status: 'ready'; data: DemandForecast }
  | { status: 'insufficient' }
  | { status: 'error' };

const selectClass =
  'mt-1 block rounded-md border border-gray-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-orange-500 focus:outline-none focus:ring-1 focus:ring-orange-500';

/** HU-03: demand forecast for the administrator (replaces the placeholder dashboard). */
export default function Dashboard() {
  const [horizon, setHorizon] = useState(6);
  const [hotelId, setHotelId] = useState(ALL_HOTELS);
  const [hotels, setHotels] = useState<HotelDTO[]>([]);
  const [state, setState] = useState<LoadState>({ status: 'loading' });
  const latestRequest = useRef(0);

  useEffect(() => {
    fetchHotels()
      .then((list) => setHotels(Array.isArray(list) ? list : []))
      .catch(() => setHotels([]));
  }, []);

  const load = useCallback(async () => {
    const request = ++latestRequest.current;
    setState({ status: 'loading' });
    try {
      const data = await getDemandForecast({ horizon, hotelId: hotelId === ALL_HOTELS ? null : Number(hotelId) });
      if (request === latestRequest.current) setState({ status: 'ready', data });
    } catch (e) {
      if (request !== latestRequest.current) return;
      setState(toApiError(e).code === 'INSUFFICIENT_HISTORY' ? { status: 'insufficient' } : { status: 'error' });
    }
  }, [horizon, hotelId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section aria-labelledby="forecast-title" className="mx-auto max-w-screen-xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 id="forecast-title" className="text-2xl font-bold text-gray-900">
            Previsión de demanda
          </h2>
          <p className="mt-1 max-w-2xl text-gray-600">
            Ocupación de los últimos 24 meses y su proyección, con una sugerencia de precio e inventario por mes.
          </p>
        </div>
        <div className="flex flex-wrap gap-4">
          <label className="text-sm font-medium text-gray-700">
            Horizonte
            <select value={horizon} onChange={(e) => setHorizon(Number(e.target.value))} className={selectClass}>
              {HORIZONS.map((h) => (
                <option key={h} value={h}>
                  {h} meses
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-gray-700">
            Hotel
            <select value={hotelId} onChange={(e) => setHotelId(e.target.value)} className={selectClass}>
              <option value={ALL_HOTELS}>Todos los hoteles</option>
              {hotels.map((hotel) => (
                <option key={hotel.id} value={String(hotel.id)}>
                  {hotel.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {state.status === 'loading' && (
        <p role="status" className="rounded-lg bg-white p-8 text-center text-gray-500 shadow-sm">
          Calculando la previsión...
        </p>
      )}

      {state.status === 'insufficient' && (
        <p role="status" className="rounded-lg bg-white p-8 text-center text-gray-700 shadow-sm">
          No hay suficiente histórico para calcular la previsión
        </p>
      )}

      {state.status === 'error' && (
        <p role="alert" className="rounded-lg bg-white p-8 text-center text-red-700 shadow-sm">
          No se pudo cargar la previsión.{' '}
          <button type="button" onClick={load} className="font-medium underline">
            Reintentar
          </button>
        </p>
      )}

      {state.status === 'ready' && (
        <>
          <StatOverview data={state.data} />

          <div className="rounded-lg bg-white p-4 shadow-sm">
            <h3 className="mb-2 text-lg font-semibold text-gray-900">Ocupación real, tendencia y previsión</h3>
            <ForecastChart data={buildChartData(state.data)} />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="rounded-lg bg-white p-4 shadow-sm lg:col-span-2">
              <h3 className="mb-2 text-lg font-semibold text-gray-900">Estacionalidad</h3>
              <SeasonalityChart indices={state.data.model.seasonalIndices} />
            </div>
            <div className="rounded-lg bg-white p-4 text-sm text-gray-600 shadow-sm">
              <h3 className="mb-2 text-lg font-semibold text-gray-900">Cómo se calcula</h3>
              <p>
                Tendencia lineal más un índice por mes, ajustados por mínimos cuadrados sobre {state.data.historyFrom} a{' '}
                {state.data.historyTo}.
              </p>
              <p className="mt-2">
                La ocupación cambia {state.data.model.slope >= 0 ? '+' : '−'}
                {Math.abs(state.data.model.slope * 100).toFixed(2)} pp por mes (base {formatRate(state.data.model.intercept)}).
              </p>
              <p className="mt-2">
                Precio: +15 % sobre 85 %, +8 % entre 70 % y 85 %, −10 % bajo 40 %. Inventario objetivo: 80 % de ocupación.
                Las sugerencias no se aplican solas.
              </p>
            </div>
          </div>

          <div className="rounded-lg bg-white p-4 shadow-sm">
            <h3 className="mb-2 text-lg font-semibold text-gray-900">Precio e inventario sugeridos</h3>
            <ForecastTable forecast={state.data.forecast} averagePricePerNight={state.data.averagePricePerNight} />
          </div>
        </>
      )}
    </section>
  );
}
