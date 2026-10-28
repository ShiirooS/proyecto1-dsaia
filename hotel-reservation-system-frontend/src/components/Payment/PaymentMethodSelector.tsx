import type { PaymentMethod } from '../../services/paymentMethodService';
import { NEW_CARD, cardLabel, type PaymentSelection } from './cardUtils';

interface PaymentMethodSelectorProps {
  methods: PaymentMethod[];
  value: PaymentSelection;
  onChange: (value: PaymentSelection) => void;
  disabled?: boolean;
}

const optionClass =
  'flex items-center gap-3 rounded-md border border-gray-200 p-3 has-[:checked]:border-orange-500 has-[:disabled]:text-gray-400';

/** Saved cards never ask for a CVV (RN-01.4). Expired cards are listed but cannot be picked. */
export default function PaymentMethodSelector({ methods, value, onChange, disabled = false }: PaymentMethodSelectorProps) {
  return (
    <fieldset className="space-y-2" disabled={disabled}>
      <legend className="mb-2 font-semibold text-gray-700">Método de pago</legend>
      {methods.map((method) => (
        <label key={method.id} className={optionClass}>
          <input
            type="radio"
            name="payment-method"
            checked={value === method.id}
            disabled={method.expired}
            onChange={() => onChange(method.id)}
          />
          <span>
            {cardLabel(method)}
            <span className="ml-2 text-sm text-gray-500">
              {String(method.expMonth).padStart(2, '0')}/{String(method.expYear).slice(-2)}
            </span>
            {method.isDefault && (
              <span className="ml-2 rounded bg-orange-100 px-2 py-0.5 text-xs text-orange-800">Predeterminada</span>
            )}
            {method.expired && <span className="ml-2 text-sm text-red-600">Vencida</span>}
          </span>
        </label>
      ))}
      <label className={optionClass}>
        <input type="radio" name="payment-method" checked={value === NEW_CARD} onChange={() => onChange(NEW_CARD)} />
        Usar otra tarjeta
      </label>
    </fieldset>
  );
}
