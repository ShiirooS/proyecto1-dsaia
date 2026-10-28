import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import PaymentMethodSelector from '../components/Payment/PaymentMethodSelector'
import { initialSelection } from '../components/Payment/cardUtils'
import type { PaymentMethod } from '../services/paymentMethodService'

const visa: PaymentMethod = { id: 1, brand: 'VISA', last4: '4242', expMonth: 12, expYear: 2030, holderName: 'ANA', isDefault: true, expired: false, createdAt: '' }
const mc: PaymentMethod = { ...visa, id: 2, brand: 'MASTERCARD', last4: '4444', isDefault: false }
const old: PaymentMethod = { ...visa, id: 3, last4: '1111', isDefault: false, expired: true, expYear: 2020 }

describe('PaymentMethodSelector', () => {
  it('preselectsDefaultAndDisablesExpired', () => {
    const methods = [mc, visa, old]
    render(<PaymentMethodSelector methods={methods} value={initialSelection(methods)} onChange={vi.fn()} />)
    expect(screen.getByRole('radio', { name: /VISA •••• 4242/ })).toBeChecked()
    expect(screen.getByRole('radio', { name: /VISA •••• 1111/ })).toBeDisabled()
    expect(screen.getByRole('radio', { name: /MASTERCARD •••• 4444/ })).toBeEnabled()
    expect(screen.getByRole('radio', { name: 'Usar otra tarjeta' })).not.toBeChecked()
  })

  it('falls back to a new card when nothing is usable', () => {
    expect(initialSelection([old])).toBe('new')
    expect(initialSelection([])).toBe('new')
  })
})
