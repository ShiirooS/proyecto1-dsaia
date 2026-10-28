import { render, screen } from '@testing-library/react'
import App from '../App'

describe('App', () => {
  it('renderiza la página de inicio con la marca del hotel', () => {
    render(<App />)
    expect(screen.getAllByText(/HOTEL MN/i).length).toBeGreaterThan(0)
  })
})
