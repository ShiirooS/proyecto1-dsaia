package app.mnhotel.hotel_reservation_system_backend.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sobre de respuesta/error. {@code code} y {@code errors} solo se envían cuando aplican
 * (compatible con las respuestas {message} existentes del CRUD de hoteles).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

	String message;

	// Código estable para que el frontend decida su lógica (ver docs/architecture/API-CONTRACT.md §0.2)
	String code;

	// Errores por campo, solo con code = VALIDATION_ERROR
	Map<String, String> errors;

	public ApiResponse(String message) {
		this.message = message;
	}

	public ApiResponse(String message, String code) {
		this.message = message;
		this.code = code;
	}
}
