import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { IoCloseOutline } from 'react-icons/io5';
import CardForm from '../Payment/CardForm';
import PaymentMethodSelector from '../Payment/PaymentMethodSelector';
import { EMPTY_CARD, NEW_CARD, cardLabel, initialSelection, toCardInput, validateCard, type CardFormValues, type PaymentSelection } from '../Payment/cardUtils';
import { nightsBetween } from '../calendar/calendarUtils';
import { toApiError } from '../../services/apiError';
import { getPaymentMethods, type PaymentMethod } from '../../services/paymentMethodService';
import { bookReservation, type BookRequest, type MyReservation } from '../../services/reservationService';
import type { RoomDTO } from '../../services/roomService';

export interface BookingSelection {
  room: RoomDTO;
  checkIn: string;
  checkOut: string;
  guests: number;
}

interface ModalProps {
  isOpen: boolean;
  closeModal: () => void;
  booking: BookingSelection;
  /** Called on 409 ROOM_NOT_AVAILABLE; the page closes the modal, reloads the calendar and warns the user. */
  onRoomUnavailable: () => void;
}

const DECLINED_MESSAGE = 'El pago fue rechazado. Prueba con otra tarjeta.';

/** Messages are chosen by error `code`, never by the server `message`. */
function messageForCode(code: string | null, status: number | null): string {
  switch (code) {
    case 'PAYMENT_DECLINED':
      return DECLINED_MESSAGE;
    case 'CARD_INVALID':
      return 'Número de tarjeta inválido';
    case 'CARD_EXPIRED':
      return 'La tarjeta está vencida';
    case 'CARD_BRAND_UNSUPPORTED':
      return 'Esa marca de tarjeta no está soportada. Usa VISA, MASTERCARD o AMEX.';
    case 'CHECKIN_IN_PAST':
    case 'INVALID_STAY_DATES':
    case 'MAX_NIGHTS_EXCEEDED':
    case 'BOOKING_TOO_FAR':
    case 'GUESTS_EXCEED_CAPACITY':
      return 'Las fechas o los huéspedes elegidos no son válidos. Revisa tu selección.';
    case 'PAYMENT_METHOD_NOT_FOUND':
      return 'La tarjeta seleccionada ya no existe. Elige otra.';
    case 'ROOM_NOT_FOUND':
      return 'La habitación ya no está disponible.';
    default:
      return status === 401
        ? 'Tu sesión expiró. Inicia sesión de nuevo.'
        : 'No se pudo completar la reserva. Inténtalo de nuevo.';
  }
}

export default function ConfirmBookingModal({ closeModal, isOpen, booking, onRoomUnavailable }: ModalProps) {
  const { room, checkIn, checkOut, guests } = booking;
  const nights = nightsBetween(checkIn, checkOut);
  const total = nights * room.pricePerNight;

  const [methods, setMethods] = useState<PaymentMethod[] | null>(null);
  const [selection, setSelection] = useState<PaymentSelection>(NEW_CARD);
  const [card, setCard] = useState<CardFormValues>(EMPTY_CARD);
  const [showErrors, setShowErrors] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState<MyReservation | null>(null);
  const inFlight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    getPaymentMethods()
      .then((list) => {
        if (cancelled) return;
        setMethods(list);
        setSelection(initialSelection(list));
      })
      .catch(() => {
        if (!cancelled) setMethods([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handlePay = async () => {
    if (inFlight.current || methods === null) return;
    setError(null);

    let request: BookRequest;
    const stay = { roomId: room.id, checkIn, checkOut, guests };
    if (selection === NEW_CARD) {
      if (Object.keys(validateCard(card)).length > 0) {
        setShowErrors(true);
        return;
      }
      request = { ...stay, card: toCardInput(card), saveCard: card.saveCard };
    } else {
      request = { ...stay, paymentMethodId: selection };
    }

    inFlight.current = true;
    setSubmitting(true);
    try {
      setConfirmed(await bookReservation(request));
    } catch (e) {
      const { code, status } = toApiError(e);
      if (code === 'ROOM_NOT_AVAILABLE') {
        onRoomUnavailable();
        return;
      }
      setError(messageForCode(code, status));
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black bg-opacity-50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-booking-title"
        className="relative flex max-h-full w-full max-w-md flex-col overflow-y-auto rounded-lg bg-white p-6 shadow-lg"
      >
        <div className="mb-4 flex items-center justify-between border-b border-gray-200 pb-4">
          <h2 id="confirm-booking-title" className="text-xl font-semibold text-gray-800">
            {confirmed ? 'Reserva confirmada' : 'Confirm Booking'}
          </h2>
          <button onClick={closeModal} className="text-gray-600 hover:text-gray-900" aria-label="Close Modal">
            <IoCloseOutline className="h-6 w-6" />
          </button>
        </div>

        {confirmed ? (
          <div className="space-y-3 text-gray-700">
            <p role="status" className="text-lg font-semibold text-green-700">
              ¡Tu reserva está confirmada!
            </p>
            <p>Reserva n.º {confirmed.id}</p>
            <p>
              {confirmed.checkIn} → {confirmed.checkOut} · {confirmed.guests} {confirmed.guests === 1 ? 'huésped' : 'huéspedes'}
            </p>
            <p>
              Pagado ${confirmed.payment.amount} con {cardLabel({ brand: confirmed.payment.cardBrand, last4: confirmed.payment.cardLast4 })}
            </p>
            <Link to="/bookings" className="inline-block rounded-md bg-orange-600 px-4 py-2 text-white hover:bg-orange-700">
              Ver mis reservas
            </Link>
          </div>
        ) : (
          <>
            <img src={room.image} alt={`Habitación ${room.roomNumber}`} className="mb-4 h-40 w-full rounded-md object-cover" />
            <dl className="mb-4 grid grid-cols-2 gap-1 text-gray-600">
              <dt className="font-semibold">Estancia</dt>
              <dd>{checkIn} → {checkOut}</dd>
              <dt className="font-semibold">Huéspedes</dt>
              <dd>{guests}</dd>
            </dl>
            <p className="mb-4 text-lg font-semibold text-gray-900">
              {nights} {nights === 1 ? 'noche' : 'noches'} · ${total}
            </p>

            {methods === null ? (
              <p role="status" className="text-gray-500">Cargando métodos de pago...</p>
            ) : (
              <div className="space-y-4">
                {methods.length > 0 && (
                  <PaymentMethodSelector methods={methods} value={selection} onChange={setSelection} disabled={submitting} />
                )}
                {selection === NEW_CARD && <CardForm value={card} onChange={setCard} showErrors={showErrors} disabled={submitting} />}
              </div>
            )}

            {error && (
              <p role="alert" className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">
                {error}
              </p>
            )}

            <div className="mt-6 flex justify-end space-x-4">
              <button type="button" onClick={closeModal} className="rounded-md bg-gray-200 px-4 py-2 text-gray-700 hover:bg-gray-300">
                Cancel
              </button>
              <button
                type="button"
                onClick={handlePay}
                disabled={submitting || methods === null}
                className="rounded-md bg-orange-600 px-4 py-2 text-white hover:bg-orange-700 disabled:cursor-not-allowed disabled:bg-gray-300"
              >
                {`Pagar $${total}`}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
