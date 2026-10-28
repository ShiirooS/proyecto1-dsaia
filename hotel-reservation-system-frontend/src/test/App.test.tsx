import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import App from '../App'

// El Home pide habitaciones/hoteles con axios: se aísla el test de la red
vi.mock('../services/axiosConfig', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: [] }) },
}))

describe('App', () => {
  it('renderiza la página de inicio con la marca del hotel', () => {
    render(<App />)
    expect(screen.getAllByText(/HOTEL MN/i).length).toBeGreaterThan(0)
  })
})
