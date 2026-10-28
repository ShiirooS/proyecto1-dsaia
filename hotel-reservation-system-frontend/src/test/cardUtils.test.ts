import { describe, expect, it } from 'vitest'
import {
  EMPTY_CARD, cvvLength, detectBrand, formatCardNumber, formatExpiry, isExpired, isValidCardNumber, parseExpiry, passesLuhn, validateCard,
} from '../components/Payment/cardUtils'

describe('cardUtils', () => {
  it.each([
    ['4242 4242 4242 4242', 'VISA'],
    ['5555555555554444', 'MASTERCARD'],
    ['2221000000000009', 'MASTERCARD'],
    ['3782 822463 10005', 'AMEX'],
    ['6011000000000004', null],
  ])('detectBrand(%s) is %s', (number, brand) => {
    expect(detectBrand(number)).toBe(brand)
  })

  it('luhn accepts the test cards and rejects 4242 4242 4242 4241', () => {
    expect(passesLuhn('4242424242424242')).toBe(true)
    expect(passesLuhn('4242424242424241')).toBe(false)
    expect(isValidCardNumber('4000 0000 0000 0002')).toBe(true)
    expect(isValidCardNumber('4242 4242 4242 4241')).toBe(false)
    expect(isValidCardNumber('3782 822463 10005')).toBe(true)
    expect(isValidCardNumber('4242')).toBe(false)
  })

  it('AMEX uses a 4-digit CVV, the rest 3', () => {
    expect(cvvLength(detectBrand('378282246310005'))).toBe(4)
    expect(cvvLength(detectBrand('4242424242424242'))).toBe(3)
    const amex = { ...EMPTY_CARD, cardNumber: '3782 822463 10005', expiry: '12/45', holderName: 'Ana' }
    expect(validateCard({ ...amex, cvv: '123' }).cvv).toBeDefined()
    expect(validateCard({ ...amex, cvv: '1234' }).cvv).toBeUndefined()
  })

  it('formats number by brand and expiry as MM/YY', () => {
    expect(formatCardNumber('4242424242424242')).toBe('4242 4242 4242 4242')
    expect(formatCardNumber('378282246310005')).toBe('3782 822463 10005')
    expect(formatExpiry('1226')).toBe('12/26')
    expect(formatExpiry('12')).toBe('12')
  })

  it('parses and checks expiry', () => {
    expect(parseExpiry('13/26')).toBeNull()
    const now = new Date(2026, 9, 28)
    expect(isExpired({ month: 9, year: 2026 }, now)).toBe(true)
    expect(isExpired({ month: 10, year: 2026 }, now)).toBe(false)
    expect(isExpired({ month: 1, year: 2027 }, now)).toBe(false)
  })
})
