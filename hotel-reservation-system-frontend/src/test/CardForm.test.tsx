import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import CardForm from '../components/Payment/CardForm'
import { EMPTY_CARD, type CardFormValues } from '../components/Payment/cardUtils'

function Harness() {
  const [value, setValue] = useState<CardFormValues>(EMPTY_CARD)
  return <CardForm value={value} onChange={setValue} />
}

describe('CardForm', () => {
  it('showsLuhnErrorBeforeSubmit', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const number = screen.getByLabelText('Número de tarjeta')
    expect(number).toHaveAttribute('autocomplete', 'cc-number')
    await user.type(number, '4242424242424241')
    expect(number).toHaveValue('4242 4242 4242 4241')
    await user.tab()
    expect(screen.getByText('Número de tarjeta inválido')).toBeInTheDocument()
  })

  it('has no error for a valid number and exposes the save checkbox', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.type(screen.getByLabelText('Número de tarjeta'), '4242424242424242')
    await user.tab()
    expect(screen.queryByText('Número de tarjeta inválido')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Guardar tarjeta para próximas compras')).not.toBeChecked()
  })
})
