package app.mnhotel.hotel_reservation_system_backend.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Error de negocio con estado HTTP y código estable (catálogo en docs/architecture/API-CONTRACT.md §0.2).
 */
@Getter
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public BusinessException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
