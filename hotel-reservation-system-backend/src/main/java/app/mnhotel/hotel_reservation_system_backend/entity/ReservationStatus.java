package app.mnhotel.hotel_reservation_system_backend.entity;

/**
 * Estado persistido de una reserva. Las fases (próxima, en curso, completada) se derivan de las fechas.
 */
public enum ReservationStatus {
	CONFIRMED,
	CANCELLED
}
