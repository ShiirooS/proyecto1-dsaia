import { FaCalendarAlt, FaCreditCard, FaMapMarkerAlt, FaUserFriends } from 'react-icons/fa';
import { cardLabel } from '../Payment/cardUtils';
import type { MyReservation } from '../../services/reservationService';
import { PHASE_LABELS, formatDate, formatDateTime } from './bookingUtils';

interface ReservationCardProps {
  reservation: MyReservation;
  onCancel: (reservation: MyReservation) => void;
}

const BADGE_CLASSES: Record<MyReservation['phase'], string> = {
  UPCOMING: 'bg-green-100 text-green-800',
  IN_STAY: 'bg-cyan-100 text-cyan-800',
  COMPLETED: 'bg-gray-100 text-gray-700',
  CANCELLED: 'bg-red-100 text-red-800',
};

function PolicyNote({ reservation }: { reservation: MyReservation }) {
  const { cancellation, payment, phase, cancelledAt } = reservation;
  if (phase === 'CANCELLED') {
    const refunded = payment?.refundedAmount ?? 0;
    return (
      <p className="text-sm text-gray-600">
        {cancelledAt ? `Cancelada el ${formatDateTime(cancelledAt)}. ` : ''}
        {refunded > 0 ? `Reembolsado: $${refunded}` : 'Sin reembolso'}
      </p>
    );
  }
  if (!cancellation.cancellable) return null;
  return cancellation.refundable ? (
    <p className="text-sm text-green-700">Cancelación gratuita hasta el {formatDateTime(cancellation.refundDeadline)}</p>
  ) : (
    <p className="text-sm text-amber-700">Faltan menos de 48 h para el check-in: si cancelas no habrá reembolso</p>
  );
}

export default function ReservationCard({ reservation, onCancel }: ReservationCardProps) {
  const { id, hotel, room, checkIn, checkOut, nights, guests, totalAmount, payment, phase, cancellation } = reservation;

  return (
    <article aria-labelledby={`reservation-${id}-title`} className="flex flex-col overflow-hidden rounded-lg bg-white shadow-md">
      {room.image && <img src={room.image} alt="" className="h-40 w-full object-cover" />}
      <div className="flex flex-1 flex-col p-6">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div>
            <h2 id={`reservation-${id}-title`} className="text-xl font-bold text-gray-800">
              {hotel?.name ?? 'Hotel'}
            </h2>
            <p className="text-gray-600">
              Habitación {room.roomNumber} · {room.type}
            </p>
          </div>
          <span className={`shrink-0 rounded-full px-3 py-1 text-sm font-semibold ${BADGE_CLASSES[phase]}`}>{PHASE_LABELS[phase]}</span>
        </div>

        <dl className="grid grid-cols-1 gap-3 text-gray-600">
          {hotel?.location && (
            <div className="flex items-center">
              <FaMapMarkerAlt aria-hidden="true" className="mr-2 h-4 w-4 text-gray-500" />
              <dt className="sr-only">Ubicación</dt>
              <dd>{hotel.location}</dd>
            </div>
          )}
          <div className="flex items-center">
            <FaCalendarAlt aria-hidden="true" className="mr-2 h-4 w-4 text-gray-500" />
            <dt className="mr-2 font-semibold">Fechas:</dt>
            <dd>
              {formatDate(checkIn)} → {formatDate(checkOut)} · {nights} {nights === 1 ? 'noche' : 'noches'}
            </dd>
          </div>
          <div className="flex items-center">
            <FaUserFriends aria-hidden="true" className="mr-2 h-4 w-4 text-gray-500" />
            <dt className="mr-2 font-semibold">Huéspedes:</dt>
            <dd>{guests}</dd>
          </div>
          <div className="flex items-center">
            <FaCreditCard aria-hidden="true" className="mr-2 h-4 w-4 text-gray-500" />
            <dt className="mr-2 font-semibold">Total:</dt>
            <dd>
              ${totalAmount}
              {payment && ` · ${cardLabel({ brand: payment.cardBrand, last4: payment.cardLast4 })}`}
            </dd>
          </div>
          <div className="flex items-center text-sm text-gray-500">
            <dt className="mr-2">Reserva n.º</dt>
            <dd>{id}</dd>
          </div>
        </dl>

        <div className="mt-4 flex flex-1 flex-wrap items-end justify-between gap-3">
          <PolicyNote reservation={reservation} />
          {cancellation.cancellable && (
            <button
              type="button"
              onClick={() => onCancel(reservation)}
              aria-label={`Cancelar reserva n.º ${id}`}
              className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 focus:outline-none focus-visible:ring focus-visible:ring-red-300"
            >
              Cancelar
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
