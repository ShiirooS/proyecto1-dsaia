import { render, screen } from '@testing-library/react'
import { cloneElement, type ReactElement } from 'react'
import { describe, expect, it, vi } from 'vitest'
import SeasonalityChart from '../components/Admin/Dashboard/SeasonalityChart'
import { forecastFixture } from './forecastFixtures'

// jsdom does not lay out: ResponsiveContainer measures 0 px and draws nothing. Give the chart a fixed size instead.
vi.mock('recharts', async (importOriginal) => {
  const recharts = await importOriginal<typeof import('recharts')>()
  return {
    ...recharts,
    ResponsiveContainer: ({ children }: { children: ReactElement }) => cloneElement(children, { width: 800, height: 300 }),
  }
})

describe('SeasonalityChart', () => {
  it('renders the twelve month labels and one bar per month (CA-03.3)', () => {
    const { container } = render(<SeasonalityChart indices={forecastFixture().model.seasonalIndices} />)

    for (const month of ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']) {
      expect(screen.getByText(month)).toBeInTheDocument()
    }
    expect(container.querySelectorAll('.recharts-bar-rectangle')).toHaveLength(12)
  })

  it('orders the months by calendar even if the API sends them unordered', () => {
    const indices = forecastFixture().model.seasonalIndices
    const { container } = render(<SeasonalityChart indices={[...indices].reverse()} />)
    const labels = Array.from(container.querySelectorAll('.recharts-cartesian-axis-tick-value')).map((t) => t.textContent)
    expect(labels.slice(0, 3)).toEqual(['Ene', 'Feb', 'Mar'])
  })
})
