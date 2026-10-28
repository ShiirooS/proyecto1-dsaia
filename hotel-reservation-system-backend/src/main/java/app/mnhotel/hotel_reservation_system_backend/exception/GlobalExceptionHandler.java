package app.mnhotel.hotel_reservation_system_backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Traduce las excepciones a respuestas HTTP con el sobre {@link ApiResponse} (API-CONTRACT.md §0.1 y §0.2).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse> handleBusiness(BusinessException e) {
		return ResponseEntity.status(e.getStatus()).body(new ApiResponse(e.getMessage(), e.getCode()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiResponse("Usuario o contraseña incorrectos", "INVALID_CREDENTIALS"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ApiResponse("Datos inválidos", "VALIDATION_ERROR", errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse> handleMalformed(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(new ApiResponse("La solicitud tiene un formato inválido", "MALFORMED_REQUEST"));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse> handleMissingParameter(MissingServletRequestParameterException e) {
		return ResponseEntity.badRequest()
				.body(new ApiResponse("Falta el parámetro obligatorio: " + e.getParameterName(), "MISSING_PARAMETER"));
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
			HandlerMethodValidationException.class })
	public ResponseEntity<ApiResponse> handleInvalidParameter(Exception e) {
		String detail = e instanceof MethodArgumentTypeMismatchException mismatch ? ": " + mismatch.getName() : "";
		return ResponseEntity.badRequest().body(new ApiResponse("Parámetro inválido" + detail, "INVALID_PARAMETER"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse> handleNoResource(NoResourceFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse("Recurso no encontrado", "NOT_FOUND"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(new ApiResponse("Método HTTP no soportado", "METHOD_NOT_ALLOWED"));
	}

	// p. ej. @PreAuthorize: debe ser 403, no 500
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiResponse("No tiene permisos para acceder a este recurso", "FORBIDDEN"));
	}

	// Resto de errores de Spring MVC con estado propio (415, 406, ...): se respeta su código 4xx en lugar de un 500
	@ExceptionHandler({ HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class,
			ServletRequestBindingException.class })
	public ResponseEntity<ApiResponse> handleFrameworkError(Exception e) {
		HttpStatus status = HttpStatus.resolve(((ErrorResponse) e).getStatusCode().value());
		if (status == null || status.is5xxServerError()) {
			return handleUnexpected(e);
		}
		return ResponseEntity.status(status).body(new ApiResponse(status.getReasonPhrase(), "MALFORMED_REQUEST"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse> handleUnexpected(Exception e) {
		log.error("Error no controlado", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponse("Ocurrió un error inesperado", "INTERNAL_ERROR"));
	}
}
