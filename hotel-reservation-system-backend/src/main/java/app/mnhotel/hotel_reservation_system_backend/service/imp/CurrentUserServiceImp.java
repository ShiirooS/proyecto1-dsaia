package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import app.mnhotel.hotel_reservation_system_backend.service.CurrentUserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImp implements CurrentUserService {

	private final UserRepository userRepository;

	@Override
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Debe iniciar sesión para acceder a este recurso");
		}
		return userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
						"Debe iniciar sesión para acceder a este recurso"));
	}

	@Override
	public boolean isAdmin() {
		return getCurrentUser().getRole() == RoleType.ADMIN;
	}

	@Override
	public void checkOwnerOrAdmin(Long ownerId, String notFoundCode, String notFoundMessage) {
		User current = getCurrentUser();
		if (current.getRole() != RoleType.ADMIN && !Objects.equals(current.getId(), ownerId)) {
			throw new ResourceNotFoundException(notFoundCode, notFoundMessage);
		}
	}
}
