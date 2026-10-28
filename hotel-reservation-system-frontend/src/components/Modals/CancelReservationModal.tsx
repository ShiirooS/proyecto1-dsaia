import { useEffect, useRef, useState } from 'react';
import { toApiError } from '../../services/apiError';
import { cancelReservation, type MyReservation } from '../../services/reservationService';
import { formatDate } from '../Bookings/bookingUtils';

interface CancelReservationModalProps {
  reservation: MyReservation;
  onClose: () => void;
  /** Called with the server's answer: it may differ from what the dialog showed (RN-05.6). */
  onCancelled: (updated: MyReservation) => void;
  /** Called on 404/409: the list is stale and must be reloaded. */
  onStale: () => void;
}

/** Messages are chosen by error `code`, never by the server `message`. */
function messageForCode(code: string | null): string {
  switch (code) {
    case 'RESERVATION_ALREADY_CANCELLED':
      return 'Esta reserva ya estaba cancelada.';
    case 'RESERVATION_NOT_CANCELLABLE':
      return 'Ya no se puede cancelar: llegó la hora de check-in.';
    case 'RESERVATION_NOT_FOUND':
      return 'La reserva ya no existe.';
    default:
      return 'No se pudo cancelar la reserva. Inténtalo de nuevo.';
  }
}

const FOCUSABLE = 'button:not([disabled]), [href], input:not([disabled]), [tabindex]:not([tabindex="-1"])';

export default function CancelReservationModal({ reservation, onClose, onCancelled, onStale }: CancelReservationModalProps) {
  const { id, hotel, room, checkIn, checkOut, cancellation } = reservation;
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const keepButtonRef = useRef<HTMLButtonElement>(null);

  // Focus starts on the safe option and returns to the "Cancelar" button that opened the dialog
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    keepButtonRef.current?.focus();
    return () => opener?.focus();
  }, []);

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Escape' && !submitting) {
      event.stopPropagation();
      onClose();
      return;
    }
    if (event.key !== 'Tab' || !dialogRef.current) return;
    const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE));
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  const handleConfirm = async () => {
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      onCancelled(await cancelReservation(id));
    } catch (e) {
      const { code, status } = toApiError(e);
      setError(messageForCode(code));
      if (status === 404 || status === 409) onStale();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="cancel-reservation-title"
        aria-describedby="cancel-reservation-policy"
        onKeyDown={handleKeyDown}
        className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg"
      >
        <h2 id="cancel-reservation-title" className="text-lg font-semibold text-gray-900">
          ¿Cancelar la reserva n.º {id}?
        </h2>
        <p className="mt-2 text-gray-600">
          {hotel?.name} · Habitación {room.roomNumber} · {formatDate(checkIn)} → {formatDate(checkOut)}
        </p>
        <p
          id="cancel-reservation-policy"
          className={`mt-4 rounded-md p-3 text-sm ${cancellation.refundable ? 'bg-green-50 text-green-800' : 'bg-amber-50 text-amber-800'}`}
        >
          {cancellation.refundable
            ? `Recibirás un reembolso total de $${cancellation.refundAmountIfCancelledNow}`
            : 'Faltan menos de 48 h para el check-in: no habrá reembolso'}
        </p>

        {error && (
          <p role="alert" className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="mt-6 flex justify-end gap-3">
          <button
            ref={keepButtonRef}
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="rounded-md bg-gray-200 px-4 py-2 text-gray-700 hover:bg-gray-300 focus:outline-none focus-visible:ring focus-visible:ring-gray-400 disabled:opacity-50"
          >
            Mantener reserva
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className="rounded-md bg-red-600 px-4 py-2 text-white hover:bg-red-700 focus:outline-none focus-visible:ring focus-visible:ring-red-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? 'Cancelando...' : 'Confirmar cancelación'}
          </button>
        </div>
      </div>
    </div>
  );
}
