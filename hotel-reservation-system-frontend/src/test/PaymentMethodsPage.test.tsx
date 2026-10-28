import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PaymentMethodsPage from '../pages/PaymentMethodsPage'
import { deletePaymentMethod, getPaymentMethods, setDefaultPaymentMethod, type PaymentMethod } from '../services/paymentMethodService'

vi.mock('../components/Layouts/Header', () => ({ default: () => null }))
vi.mock('../services/paymentMethodService', () => ({
  getPaymentMethods: vi.fn(), setDefaultPaymentMethod: vi.fn(), deletePaymentMethod: vi.fn(), createPaymentMethod: vi.fn(),
}))

const visa: PaymentMethod = { id: 1, brand: 'VISA', last4: '4242', expMonth: 12, expYear: 2030, holderName: 'ANA', isDefault: true, expired: false, createdAt: '' }
const mc: PaymentMethod = { ...visa, id: 2, brand: 'MASTERCARD', last4: '4444', isDefault: false }

describe('PaymentMethodsPage', () => {
  beforeEach(() => {
    vi.mocked(getPaymentMethods).mockResolvedValue([visa, mc])
  })
  afterEach(() => vi.resetAllMocks())

  it('marks a card as default and reloads', async () => {
    const user = userEvent.setup()
    vi.mocked(setDefaultPaymentMethod).mockResolvedValue({ ...mc, isDefault: true })
    render(<MemoryRouter><PaymentMethodsPage /></MemoryRouter>)
    await user.click(await screen.findByRole('button', { name: 'Marcar MASTERCARD •••• 4444 como predeterminada' }))
    expect(setDefaultPaymentMethod).toHaveBeenCalledWith(2)
    expect(getPaymentMethods).toHaveBeenCalledTimes(2)
  })

  it('deletes only after confirmation', async () => {
    const user = userEvent.setup()
    vi.mocked(deletePaymentMethod).mockResolvedValue()
    render(<MemoryRouter><PaymentMethodsPage /></MemoryRouter>)
    await user.click(await screen.findByRole('button', { name: 'Eliminar MASTERCARD •••• 4444' }))
    expect(deletePaymentMethod).not.toHaveBeenCalled()
    const dialog = screen.getByRole('alertdialog')
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar eliminación' }))
    expect(deletePaymentMethod).toHaveBeenCalledWith(2)
  })
})
