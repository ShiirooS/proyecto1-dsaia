package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.entity.User;

public interface JwtService {

	public String generateToken(User user);

	/**
	 * Devuelve el username del token si la firma y la expiración son válidas; null en caso contrario.
	 */
	public String extractValidUsername(String token);
}
