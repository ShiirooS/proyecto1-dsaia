import type { CardBrand, CardInput, PaymentMethod } from '../../services/paymentMethodService';

/** UX-only helpers; the server (CardValidator) is the authority. */

export const digitsOnly = (value: string): string => value.replace(/\D/g, '');

export function detectBrand(value: string): CardBrand | null {
  const digits = digitsOnly(value);
  if (/^4/.test(digits)) return 'VISA';
  if (/^3[47]/.test(digits)) return 'AMEX';
  const firstFour = Number(digits.slice(0, 4));
  if (/^5[1-5]/.test(digits) || (digits.length >= 4 && firstFour >= 2221 && firstFour <= 2720)) return 'MASTERCARD';
  return null;
}

export const cardNumberLength = (brand: CardBrand | null): number => (brand === 'AMEX' ? 15 : 16);
export const cvvLength = (brand: CardBrand | null): number => (brand === 'AMEX' ? 4 : 3);

export function passesLuhn(value: string): boolean {
  const digits = digitsOnly(value);
  if (digits.length === 0) return false;
  let sum = 0;
  for (let i = 0; i < digits.length; i++) {
    let digit = Number(digits[digits.length - 1 - i]);
    if (i % 2 === 1) {
      digit *= 2;
      if (digit > 9) digit -= 9;
    }
    sum += digit;
  }
  return sum % 10 === 0;
}

export function isValidCardNumber(value: string): boolean {
  const digits = digitsOnly(value);
  const brand = detectBrand(digits);
  return brand !== null && digits.length === cardNumberLength(brand) && passesLuhn(digits);
}

export function formatCardNumber(value: string): string {
  const brand = detectBrand(value);
  const digits = digitsOnly(value).slice(0, cardNumberLength(brand));
  const groups = brand === 'AMEX' ? [4, 6, 5] : [4, 4, 4, 4];
  const parts: string[] = [];
  let start = 0;
  for (const size of groups) {
    if (start >= digits.length) break;
    parts.push(digits.slice(start, start + size));
    start += size;
  }
  return parts.join(' ');
}

/** Typing "1226" yields "12/26"; the slash is inserted once a month is complete. */
export function formatExpiry(value: string): string {
  const digits = digitsOnly(value).slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
}

export interface Expiry {
  month: number;
  year: number;
}

export function parseExpiry(value: string): Expiry | null {
  const match = /^(\d{2})\/(\d{2})$/.exec(value);
  if (!match) return null;
  const month = Number(match[1]);
  if (month < 1 || month > 12) return null;
  return { month, year: 2000 + Number(match[2]) };
}

export function isExpired({ month, year }: Expiry, now: Date = new Date()): boolean {
  const currentYear = now.getFullYear();
  return year < currentYear || (year === currentYear && month < now.getMonth() + 1);
}

/** Raw form values; `cardNumber` and `expiry` hold the formatted text. Lives only in component state. */
export interface CardFormValues {
  cardNumber: string;
  expiry: string;
  cvv: string;
  holderName: string;
  saveCard: boolean;
}

export const EMPTY_CARD: CardFormValues = { cardNumber: '', expiry: '', cvv: '', holderName: '', saveCard: false };

export type CardField = 'cardNumber' | 'expiry' | 'cvv' | 'holderName';
export type CardErrors = Partial<Record<CardField, string>>;

export function validateCard(values: CardFormValues, now: Date = new Date()): CardErrors {
  const errors: CardErrors = {};
  const brand = detectBrand(values.cardNumber);
  if (!isValidCardNumber(values.cardNumber)) errors.cardNumber = 'Número de tarjeta inválido';
  const expiry = parseExpiry(values.expiry);
  if (!expiry) errors.expiry = 'Fecha de vencimiento inválida';
  else if (isExpired(expiry, now)) errors.expiry = 'La tarjeta está vencida';
  if (!/^\d+$/.test(values.cvv) || values.cvv.length !== cvvLength(brand)) {
    errors.cvv = `El CVV debe tener ${cvvLength(brand)} dígitos`;
  }
  const holder = values.holderName.trim();
  if (holder.length < 2 || holder.length > 60) errors.holderName = 'Ingresa el nombre del titular';
  return errors;
}

/** Call only after `validateCard` returned no errors. */
export function toCardInput(values: CardFormValues): CardInput {
  const expiry = parseExpiry(values.expiry);
  return {
    cardNumber: digitsOnly(values.cardNumber),
    expMonth: expiry?.month ?? 0,
    expYear: expiry?.year ?? 0,
    cvv: values.cvv,
    holderName: values.holderName.trim(),
  };
}

export const NEW_CARD = 'new' as const;
export type PaymentSelection = number | typeof NEW_CARD;

export const cardLabel = (method: Pick<PaymentMethod, 'brand' | 'last4'>) => `${method.brand} •••• ${method.last4}`;

/** The default card if usable, else the first usable one, else a new card. */
export function initialSelection(methods: PaymentMethod[]): PaymentSelection {
  const usable = methods.find((m) => m.isDefault && !m.expired) ?? methods.find((m) => !m.expired);
  return usable ? usable.id : NEW_CARD;
}
