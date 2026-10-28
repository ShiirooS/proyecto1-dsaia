import { Bar, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ChartPoint } from './forecastUtils';

interface ForecastChartProps {
  data: ChartPoint[];
}

const percent = (value: number) => `${value}%`;
const tooltipValue = (value: number) => `${value.toFixed(1)} %`;

/** CA-03.1: 24 months of real occupancy, the fitted trend and the forecast (dashed), plus nights already booked. */
export default function ForecastChart({ data }: ForecastChartProps) {
  return (
    <figure aria-label="Ocupación real, tendencia y previsión" className="h-80 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis dataKey="label" tick={{ fontSize: 12 }} interval="preserveStartEnd" minTickGap={12} />
          <YAxis domain={[0, 100]} tickFormatter={percent} tick={{ fontSize: 12 }} width={44} />
          <Tooltip formatter={tooltipValue} />
          <Legend />
          <Bar dataKey="booked" name="Ya reservado" fill="#fdba74" barSize={14} />
          <Line type="monotone" dataKey="actual" name="Ocupación real" stroke="#0891b2" strokeWidth={2} dot={false} />
          <Line type="linear" dataKey="trend" name="Tendencia" stroke="#9ca3af" strokeWidth={1.5} dot={false} />
          <Line
            type="monotone"
            dataKey="forecast"
            name="Previsión"
            stroke="#ea580c"
            strokeWidth={2.5}
            strokeDasharray="6 4"
            dot={{ r: 3 }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </figure>
  );
}
