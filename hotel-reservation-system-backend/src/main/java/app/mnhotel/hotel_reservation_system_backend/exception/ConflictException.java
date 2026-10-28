package app.mnhotel.hotel_reservation_system_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Conflicto con el estado actual del recurso (p. ej. usuario duplicado, fechas ya reservadas). Se traduce a HTTP 409.
 */
public class ConflictException extends BusinessException {

	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}
}
