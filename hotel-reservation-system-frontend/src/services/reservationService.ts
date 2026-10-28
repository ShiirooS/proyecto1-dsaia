import api from './axiosConfig';
import type { CardBrand, CardInput } from './paymentMethodService';
import type { IsoDate } from './roomService';

interface BookStay {
  roomId: number;
  checkIn: IsoDate;
  checkOut: IsoDate;
  guests: number;
}

/** Exactly one payment source: a saved card id, or a new card (optionally saved). */
export type BookRequest =
  | (BookStay & { paymentMethodId: number })
  | (BookStay & { card: CardInput; saveCard?: boolean });

export interface MyReservation {
  id: number;
  status: 'CONFIRMED' | 'CANCELLED';
  phase: 'UPCOMING' | 'IN_STAY' | 'COMPLETED' | 'CANCELLED';
  checkIn: IsoDate;
  checkOut: IsoDate;
  nights: number;
  guests: number;
  totalAmount: number;
  createdAt: string;
  cancelledAt: string | null;
  room: { id: number; roomNumber: string; type: string; image: string };
  hotel: { id: number; name: string; location: string };
  payment: {
    id: number;
    status: 'COMPLETED' | 'REFUNDED' | string;
    amount: number;
    refundedAmount: number;
    cardBrand: CardBrand;
    cardLast4: string;
    transactionId: string;
    paymentDate: string;
  };
  cancellation: {
    cancellable: boolean;
    refundable: boolean;
    refundDeadline: string;
    refundAmountIfCancelledNow: number;
  };
  savedPaymentMethodId?: number | null;
}

export async function bookReservation(request: BookRequest): Promise<MyReservation> {
  const response = await api.post('/api/reservation/book', request);
  return response.data as MyReservation;
}

export async function getMyReservation(id: number | string): Promise<MyReservation> {
  const response = await api.get(`/api/reservation/my-reservations/${id}`);
  return response.data as MyReservation;
}
