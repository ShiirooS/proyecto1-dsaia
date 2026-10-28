import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { MONTH_NAMES } from './forecastUtils';

interface SeasonalityChartProps {
  indices: { month: number; index: number }[];
}

const points = (value: number) => `${value > 0 ? '+' : ''}${value} pp`;

/** CA-03.3: the 12 monthly seasonal indices in percentage points (positive = month above the trend). */
export default function SeasonalityChart({ indices }: SeasonalityChartProps) {
  const data = [...indices]
    .sort((a, b) => a.month - b.month)
    .map(({ month, index }) => ({ name: MONTH_NAMES[month - 1], value: Math.round(index * 1000) / 10 }));

  return (
    <figure aria-label="Índices estacionales por mes" className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} interval={0} />
          <YAxis tickFormatter={points} tick={{ fontSize: 12 }} width={56} />
          <Tooltip formatter={(value: number) => [points(value), 'Índice estacional']} />
          <ReferenceLine y={0} stroke="#6b7280" />
          <Bar dataKey="value" name="Índice estacional">
            {data.map((entry) => (
              <Cell key={entry.name} fill={entry.value >= 0 ? '#ea580c' : '#0891b2'} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </figure>
  );
}
