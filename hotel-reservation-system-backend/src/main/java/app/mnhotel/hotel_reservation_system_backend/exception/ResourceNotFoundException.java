package app.mnhotel.hotel_reservation_system_backend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

	  public ResourceNotFoundException(String message) {
	        this("NOT_FOUND", message);
	    }

	  public ResourceNotFoundException(String code, String message) {
	        super(HttpStatus.NOT_FOUND, code, message);
	    }
}
