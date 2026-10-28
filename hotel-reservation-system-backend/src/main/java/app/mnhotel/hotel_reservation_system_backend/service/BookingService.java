package app.mnhotel.hotel_reservation_system_backend.service;

import java.util.List;

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

	/** HU-05: todas las reservas del usuario autenticado, orden checkIn DESC, id DESC (API-CONTRACT §6.2). */
	public List<MyReservationDTO> listMyReservations();

	/** HU-05: cancela una reserva propia aplicando la política de 48 h en este instante (API-CONTRACT §6.4). */
	public MyReservationDTO cancelMyReservation(Long id);
}
