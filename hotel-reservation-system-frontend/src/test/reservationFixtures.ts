import type { MyReservation } from '../services/reservationService'

/** Reservation 42 of the HU-05 scenarios: 20 → 23/12/2026, $450, VISA •••• 4242, refundable until 18/12 15:00. */
export function reservationFixture(overrides: Partial<MyReservation> = {}): MyReservation {
  return {
    id: 42,
    status: 'CONFIRMED',
    phase: 'UPCOMING',
    checkIn: '2026-12-20',
    checkOut: '2026-12-23',
    nights: 3,
    guests: 2,
    totalAmount: 450,
    createdAt: '2026-10-28T10:30:00',
    cancelledAt: null,
    room: { id: 3, roomNumber: '203', type: 'DOBLE', image: 'https://img.example.com/203.jpg' },
    hotel: { id: 1, name: 'Hotel Mar Azul', location: 'Ciudad de Panamá' },
    payment: {
      id: 77, status: 'COMPLETED', amount: 450, refundedAmount: 0, cardBrand: 'VISA', cardLast4: '4242',
      transactionId: 'txn_abc', paymentDate: '2026-10-28T10:30:00',
    },
    cancellation: { cancellable: true, refundable: true, refundDeadline: '2026-12-18T15:00:00', refundAmountIfCancelledNow: 450 },
    ...overrides,
  }
}

export const lateReservation = (overrides: Partial<MyReservation> = {}) =>
  reservationFixture({
    cancellation: { cancellable: true, refundable: false, refundDeadline: '2026-12-18T15:00:00', refundAmountIfCancelledNow: 0 },
    ...overrides,
  })

export const cancelledReservation = (overrides: Partial<MyReservation> = {}) =>
  reservationFixture({
    status: 'CANCELLED',
    phase: 'CANCELLED',
    cancelledAt: '2026-12-01T09:15:00',
    payment: { ...reservationFixture().payment, status: 'REFUNDED', refundedAmount: 450 },
    cancellation: { cancellable: false, refundable: false, refundDeadline: '2026-12-18T15:00:00', refundAmountIfCancelledNow: 0 },
    ...overrides,
  })
