package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.dto.MyReservationDTO;
import app.mnhotel.hotel_reservation_system_backend.request.BookingRequest;

/**
 * Flujos de reserva del huésped autenticado (HU-01 y HU-05). El CRUD administrativo sigue en ReservationService.
 */
public interface BookingService {

	/** Reserva y cobra en una sola transacción (API-CONTRACT §6.1). */
	public MyReservationDTO book(BookingRequest request);

	/** Reserva del usuario autenticado (ajena o inexistente: 404 RESERVATION_NOT_FOUND). */
	public MyReservationDTO getMyReservation(Long id);
}
