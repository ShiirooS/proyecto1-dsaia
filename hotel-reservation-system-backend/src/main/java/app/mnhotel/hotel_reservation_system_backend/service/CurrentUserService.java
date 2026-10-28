package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.entity.User;

/**
 * HU-04: identidad del usuario autenticado y reglas de "dueño del recurso o ADMIN".
 */
public interface CurrentUserService {

	public User getCurrentUser();
	public boolean isAdmin();

	/**
	 * Lanza 404 si el usuario autenticado no es el dueño (ownerId) ni ADMIN.
	 * Se responde 404 y no 403 para no revelar que el recurso existe (API-CONTRACT §0).
	 */
	public void checkOwnerOrAdmin(Long ownerId, String notFoundCode, String notFoundMessage);
}
