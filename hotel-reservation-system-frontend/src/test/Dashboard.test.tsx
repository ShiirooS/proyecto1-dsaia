import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { cloneElement, type ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from '../components/Admin/Dashboard/Dashboard'
import { getDemandForecast } from '../services/analyticsService'
import { fetchHotels, type HotelDTO } from '../services/hotelService'
import { forecastFixture } from './forecastFixtures'

vi.mock('../services/analyticsService', () => ({ getDemandForecast: vi.fn() }))
// jsdom does not lay out: give the charts a fixed size so the legend and the lines are drawn
vi.mock('recharts', async (importOriginal) => {
  const recharts = await importOriginal<typeof import('recharts')>()
  return {
    ...recharts,
    ResponsiveContainer: ({ children }: { children: ReactElement }) => cloneElement(children, { width: 800, height: 300 }),
  }
})
vi.mock('../services/hotelService', () => ({ fetchHotels: vi.fn() }))

const marAzul = { id: 1, name: 'Hotel Mar Azul' } as HotelDTO
const boquete = { id: 2, name: 'Boquete Mountain Lodge' } as HotelDTO

function apiError(status: number, code: string) {
  const headers = new AxiosHeaders()
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', { headers }, null, {
    status, statusText: '', headers, config: { headers }, data: { message: 'x', code },
  })
}

const tableRows = () => within(screen.getByRole('table')).getAllByRole('row').slice(1)

describe('Dashboard (demand forecast)', () => {
  beforeEach(() => {
    vi.mocked(fetchHotels).mockResolvedValue([marAzul, boquete])
  })
  afterEach(() => vi.resetAllMocks())

  it('requests the default horizon and renders six rows (CA-03.1)', async () => {
    vi.mocked(getDemandForecast).mockResolvedValue(forecastFixture(6))
    render(<Dashboard />)

    expect(screen.getByRole('status')).toHaveTextContent('Calculando la previsión...')
    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(getDemandForecast).toHaveBeenCalledWith({ horizon: 6, hotelId: null })

    const rows = tableRows()
    expect(rows).toHaveLength(6)
    const january = within(rows[0]).getAllByRole('cell').map((cell) => cell.textContent)
    expect(rows[0]).toHaveTextContent('enero 2027')
    expect(january).toEqual(['69.0 %', '214 / 310', '35', '0 %', '$100', 'Mantener', '10'])
    expect(rows[4]).toHaveTextContent('mayo 2027')
    expect(within(rows[4]).getByText('+15 %')).toBeInTheDocument()
    expect(within(rows[4]).getByText('Aumentar')).toBeInTheDocument()

    // KPIs reales y gráficos con leyenda
    const peak = screen.getByText('Mes pico previsto (99.0 %)').parentElement as HTMLElement
    expect(within(peak).getByText('junio 2027')).toBeInTheDocument()
    expect(screen.getByRole('figure', { name: 'Ocupación real, tendencia y previsión' })).toBeInTheDocument()
    for (const series of ['Ocupación real', 'Tendencia', 'Previsión', 'Ya reservado']) {
      expect(screen.getByText(series, { selector: '.recharts-legend-item-text' })).toBeInTheDocument()
    }  })

  it('refetches when the horizon and the hotel change (CA-03.2)', async () => {
    vi.mocked(getDemandForecast).mockImplementation(async ({ horizon }) => forecastFixture(horizon))
    const user = userEvent.setup()
    render(<Dashboard />)
    await screen.findByRole('table')
    await waitFor(() => expect(screen.getByRole('option', { name: 'Hotel Mar Azul' })).toBeInTheDocument())

    await user.selectOptions(screen.getByLabelText('Horizonte'), '12')
    await user.selectOptions(screen.getByLabelText('Hotel'), 'Hotel Mar Azul')

    await waitFor(() => expect(getDemandForecast).toHaveBeenLastCalledWith({ horizon: 12, hotelId: 1 }))
    await waitFor(() => expect(tableRows()).toHaveLength(12))
  })

  it('shows the insufficient history message without charts (CA-03.6)', async () => {
    vi.mocked(getDemandForecast).mockRejectedValue(apiError(422, 'INSUFFICIENT_HISTORY'))
    render(<Dashboard />)

    expect(await screen.findByText('No hay suficiente histórico para calcular la previsión')).toBeInTheDocument()
    expect(screen.queryByRole('figure')).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('shows loading and then a retry on other errors (CA-03.10)', async () => {
    vi.mocked(getDemandForecast).mockRejectedValueOnce(apiError(500, 'INTERNAL_ERROR')).mockResolvedValueOnce(forecastFixture(6))
    const user = userEvent.setup()
    render(<Dashboard />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar la previsión')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(getDemandForecast).toHaveBeenCalledTimes(2)
  })

  it('still works when the hotel list cannot be loaded', async () => {
    vi.mocked(fetchHotels).mockRejectedValue(new Error('down'))
    vi.mocked(getDemandForecast).mockResolvedValue(forecastFixture(6))
    render(<Dashboard />)

    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(within(screen.getByLabelText('Hotel')).getAllByRole('option')).toHaveLength(1)
  })
})
