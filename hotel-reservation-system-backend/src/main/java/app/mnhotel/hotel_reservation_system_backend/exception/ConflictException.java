package app.mnhotel.hotel_reservation_system_backend.exception;

/**
 * Conflicto con el estado actual del recurso (p. ej. usuario duplicado, fechas ya reservadas). Se traduce a HTTP 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
