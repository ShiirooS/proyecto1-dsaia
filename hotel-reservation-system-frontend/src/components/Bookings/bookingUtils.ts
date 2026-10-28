import type { MyReservation } from '../../services/reservationService';

export type BookingTab = 'upcoming' | 'past' | 'cancelled';

export const BOOKING_TABS: { id: BookingTab; label: string }[] = [
  { id: 'upcoming', label: 'Próximas' },
  { id: 'past', label: 'Pasadas' },
  { id: 'cancelled', label: 'Canceladas' },
];

/** RN-05.5: Próximas = UPCOMING + IN_STAY; Pasadas = COMPLETED; Canceladas = CANCELLED. */
export function tabOf(reservation: Pick<MyReservation, 'phase'>): BookingTab {
  switch (reservation.phase) {
    case 'COMPLETED':
      return 'past';
    case 'CANCELLED':
      return 'cancelled';
    default:
      return 'upcoming';
  }
}

export function groupByTab(reservations: MyReservation[]): Record<BookingTab, MyReservation[]> {
  const groups: Record<BookingTab, MyReservation[]> = { upcoming: [], past: [], cancelled: [] };
  for (const reservation of reservations) {
    groups[tabOf(reservation)].push(reservation);
  }
  return groups;
}

/** '2026-12-20' -> '20/12/2026' (string based: no time zone shifts). */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.slice(0, 10).split('-');
  return `${day}/${month}/${year}`;
}

/** '2026-12-18T15:00:00' (hotel time, ADR-006) -> '18/12/2026 15:00'. */
export function formatDateTime(isoDateTime: string): string {
  return `${formatDate(isoDateTime)} ${isoDateTime.slice(11, 16)}`;
}

export const PHASE_LABELS: Record<MyReservation['phase'], string> = {
  UPCOMING: 'Confirmada',
  IN_STAY: 'En curso',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};
