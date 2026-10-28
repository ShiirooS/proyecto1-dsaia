import { useId, useState } from 'react';
import {
  cvvLength,
  detectBrand,
  digitsOnly,
  formatCardNumber,
  formatExpiry,
  validateCard,
  type CardField,
  type CardFormValues,
} from './cardUtils';

interface CardFormProps {
  value: CardFormValues;
  onChange: (value: CardFormValues) => void;
  /** Show every error, e.g. after a failed submit attempt. Otherwise a field reports on blur. */
  showErrors?: boolean;
  disabled?: boolean;
  /** Label of the "save card" checkbox; `null` hides it. */
  saveCardLabel?: string | null;
}

const inputClass =
  'mt-1 w-full rounded-md border border-gray-300 p-2 focus:outline-none focus-visible:ring focus-visible:ring-orange-400 aria-[invalid=true]:border-red-500';

/** Controlled card fields. The card number lives only in the parent's local state, never in a store or localStorage. */
export default function CardForm({
  value,
  onChange,
  showErrors = false,
  disabled = false,
  saveCardLabel = 'Guardar tarjeta para próximas compras',
}: CardFormProps) {
  const uid = useId();
  const [touched, setTouched] = useState<ReadonlySet<CardField>>(new Set());
  const errors = validateCard(value);
  const brand = detectBrand(value.cardNumber);

  const id = (name: string) => `${uid}-${name}`;
  const errorFor = (name: CardField) => (showErrors || touched.has(name) ? errors[name] : undefined);

  const fieldProps = (name: CardField) => {
    const error = errorFor(name);
    return {
      id: id(name),
      disabled,
      'aria-invalid': error !== undefined,
      'aria-describedby': error ? `${id(name)}-error` : undefined,
      onBlur: () => setTouched((prev) => new Set(prev).add(name)),
    };
  };
  const errorText = (name: CardField) => {
    const error = errorFor(name);
    return error ? (
      <p id={`${id(name)}-error`} role="alert" className="mt-1 text-sm text-red-600">
        {error}
      </p>
    ) : null;
  };

  return (
    <div className="space-y-3">
      <div>
        <label htmlFor={id('cardNumber')} className="block font-semibold text-gray-600">Número de tarjeta</label>
        <input
          {...fieldProps('cardNumber')}
          type="text"
          inputMode="numeric"
          autoComplete="cc-number"
          placeholder="4242 4242 4242 4242"
          value={value.cardNumber}
          onChange={(e) => onChange({ ...value, cardNumber: formatCardNumber(e.target.value) })}
          className={inputClass}
        />
        {errorText('cardNumber')}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label htmlFor={id('expiry')} className="block font-semibold text-gray-600">Vencimiento (MM/AA)</label>
          <input
            {...fieldProps('expiry')}
            type="text"
            inputMode="numeric"
            autoComplete="cc-exp"
            placeholder="MM/AA"
            maxLength={5}
            value={value.expiry}
            onChange={(e) => onChange({ ...value, expiry: formatExpiry(e.target.value) })}
            className={inputClass}
          />
          {errorText('expiry')}
        </div>
        <div>
          <label htmlFor={id('cvv')} className="block font-semibold text-gray-600">CVV</label>
          <input
            {...fieldProps('cvv')}
            type="password"
            inputMode="numeric"
            autoComplete="cc-csc"
            maxLength={cvvLength(brand)}
            value={value.cvv}
            onChange={(e) => onChange({ ...value, cvv: digitsOnly(e.target.value) })}
            className={inputClass}
          />
          {errorText('cvv')}
        </div>
      </div>
      <div>
        <label htmlFor={id('holderName')} className="block font-semibold text-gray-600">Titular</label>
        <input
          {...fieldProps('holderName')}
          type="text"
          autoComplete="cc-name"
          maxLength={60}
          value={value.holderName}
          onChange={(e) => onChange({ ...value, holderName: e.target.value })}
          className={inputClass}
        />
        {errorText('holderName')}
      </div>
      {saveCardLabel !== null && (
        <label className="flex items-center gap-2 text-gray-700">
          <input
            type="checkbox"
            disabled={disabled}
            checked={value.saveCard}
            onChange={(e) => onChange({ ...value, saveCard: e.target.checked })}
          />
          {saveCardLabel}
        </label>
      )}
    </div>
  );
}
