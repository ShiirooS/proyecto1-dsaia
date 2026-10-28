package app.mnhotel.hotel_reservation_system_backend.enums;

/**
 * Estado persistido de una reserva (HU-02). Solo CONFIRMED ocupa noches en el calendario.
 */
public enum ReservationStatus {
	CONFIRMED,
	CANCELLED
}
