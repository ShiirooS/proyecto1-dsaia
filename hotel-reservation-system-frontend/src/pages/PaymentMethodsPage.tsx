import { useCallback, useEffect, useState } from 'react';
import Header from '../components/Layouts/Header';
import HeaderBanner from '../components/Layouts/HeaderBanner';
import CardForm from '../components/Payment/CardForm';
import { EMPTY_CARD, cardLabel, toCardInput, validateCard, type CardFormValues } from '../components/Payment/cardUtils';
import { toApiError } from '../services/apiError';
import {
  createPaymentMethod,
  deletePaymentMethod,
  getPaymentMethods,
  setDefaultPaymentMethod,
  type PaymentMethod,
} from '../services/paymentMethodService';

function messageForCode(code: string | null): string {
  switch (code) {
    case 'DUPLICATE_PAYMENT_METHOD':
      return 'Esa tarjeta ya está guardada.';
    case 'CARD_INVALID':
      return 'Número de tarjeta inválido';
    case 'CARD_EXPIRED':
      return 'La tarjeta está vencida';
    case 'CARD_BRAND_UNSUPPORTED':
      return 'Esa marca de tarjeta no está soportada. Usa VISA, MASTERCARD o AMEX.';
    case 'PAYMENT_METHOD_NOT_FOUND':
      return 'La tarjeta ya no existe.';
    default:
      return 'No se pudo completar la operación. Inténtalo de nuevo.';
  }
}

const buttonClass =
  'rounded-md px-3 py-1.5 text-sm font-medium focus:outline-none focus-visible:ring focus-visible:ring-orange-400 disabled:cursor-not-allowed disabled:opacity-50';

export default function PaymentMethodsPage() {
  const [methods, setMethods] = useState<PaymentMethod[] | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<PaymentMethod | null>(null);
  const [adding, setAdding] = useState(false);
  const [card, setCard] = useState<CardFormValues>(EMPTY_CARD);
  const [makeDefault, setMakeDefault] = useState(false);
  const [showErrors, setShowErrors] = useState(false);

  const reload = useCallback(async () => {
    try {
      setMethods(await getPaymentMethods());
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const run = async (action: () => Promise<unknown>): Promise<boolean> => {
    setBusy(true);
    setError(null);
    try {
      await action();
      await reload();
      return true;
    } catch (e) {
      setError(messageForCode(toApiError(e).code));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const handleSetDefault = (method: PaymentMethod) => run(() => setDefaultPaymentMethod(method.id));

  const handleConfirmDelete = async () => {
    if (!pendingDelete) return;
    const target = pendingDelete;
    setPendingDelete(null);
    await run(() => deletePaymentMethod(target.id));
  };

  const handleAdd = async (event: React.FormEvent) => {
    event.preventDefault();
    if (Object.keys(validateCard(card)).length > 0) {
      setShowErrors(true);
      return;
    }
    const saved = await run(() => createPaymentMethod(toCardInput(card), makeDefault));
    if (saved) {
      setAdding(false);
      setCard(EMPTY_CARD);
      setMakeDefault(false);
      setShowErrors(false);
    }
  };

  return (
    <div>
      <Header />
      <HeaderBanner />
      <main className="mx-auto max-w-2xl px-4 py-10">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">Métodos de pago</h1>
          {!adding && (
            <button type="button" onClick={() => setAdding(true)} className={`${buttonClass} bg-orange-600 text-white hover:bg-orange-700`}>
              Añadir tarjeta
            </button>
          )}
        </div>

        {error && (
          <p role="alert" className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        )}

        {adding && (
          <form onSubmit={handleAdd} aria-label="Añadir tarjeta" className="mb-6 space-y-4 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <CardForm value={card} onChange={setCard} showErrors={showErrors} disabled={busy} saveCardLabel={null} />
            <label className="flex items-center gap-2 text-gray-700">
              <input type="checkbox" checked={makeDefault} disabled={busy} onChange={(e) => setMakeDefault(e.target.checked)} />
              Usar como predeterminada
            </label>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setAdding(false);
                  setCard(EMPTY_CARD);
                  setShowErrors(false);
                }}
                className={`${buttonClass} bg-gray-200 text-gray-700 hover:bg-gray-300`}
              >
                Cancelar
              </button>
              <button type="submit" disabled={busy} className={`${buttonClass} bg-orange-600 text-white hover:bg-orange-700`}>
                Guardar tarjeta
              </button>
            </div>
          </form>
        )}

        {loadFailed ? (
          <p role="alert" className="text-red-700">
            No se pudieron cargar tus tarjetas.{' '}
            <button type="button" onClick={reload} className="underline">Reintentar</button>
          </p>
        ) : methods === null ? (
          <p role="status" className="text-gray-500">Cargando...</p>
        ) : methods.length === 0 ? (
          <p className="text-gray-600">No tienes tarjetas guardadas.</p>
        ) : (
          <ul className="space-y-3">
            {methods.map((method) => (
              <li key={method.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
                <div>
                  <p className="font-semibold text-gray-900">
                    {cardLabel(method)}
                    {method.isDefault && (
                      <span className="ml-2 rounded bg-orange-100 px-2 py-0.5 text-xs text-orange-800">Predeterminada</span>
                    )}
                    {method.expired && <span className="ml-2 text-sm text-red-600">Vencida</span>}
                  </p>
                  <p className="text-sm text-gray-500">
                    {method.holderName} · vence {String(method.expMonth).padStart(2, '0')}/{String(method.expYear).slice(-2)}
                  </p>
                </div>
                <div className="flex gap-2">
                  {!method.isDefault && (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => handleSetDefault(method)}
                      aria-label={`Marcar ${cardLabel(method)} como predeterminada`}
                      className={`${buttonClass} border border-gray-300 text-gray-700 hover:bg-gray-100`}
                    >
                      Marcar como predeterminada
                    </button>
                  )}
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setPendingDelete(method)}
                    aria-label={`Eliminar ${cardLabel(method)}`}
                    className={`${buttonClass} border border-red-300 text-red-700 hover:bg-red-50`}
                  >
                    Eliminar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {pendingDelete && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div role="alertdialog" aria-modal="true" aria-labelledby="delete-card-title" className="w-full max-w-sm rounded-lg bg-white p-6 shadow-lg">
              <h2 id="delete-card-title" className="text-lg font-semibold text-gray-900">
                ¿Eliminar {cardLabel(pendingDelete)}?
              </h2>
              <p className="mt-2 text-gray-600">Esta acción no se puede deshacer.</p>
              <div className="mt-6 flex justify-end gap-3">
                <button type="button" onClick={() => setPendingDelete(null)} className={`${buttonClass} bg-gray-200 text-gray-700 hover:bg-gray-300`}>
                  Cancelar
                </button>
                <button type="button" onClick={handleConfirmDelete} className={`${buttonClass} bg-red-600 text-white hover:bg-red-700`}>
                  Confirmar eliminación
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
