package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.dto.LoginRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.RegisterRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.UserProfileDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.Role;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.exception.ConflictException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import app.mnhotel.hotel_reservation_system_backend.response.AuthResponse;
import app.mnhotel.hotel_reservation_system_backend.service.AuthService;
import app.mnhotel.hotel_reservation_system_backend.service.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService{

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	// Convertir de entidad a DTO (sin exponer la contraseña)
	public static UserProfileDTO convertEntityToDTO(User user) {
		return UserProfileDTO.builder()
				.id(user.getId())
				.username(user.getUsername())
				.email(user.getEmail())
				.fullName(user.getFullName())
				.phone(user.getPhone())
				.role(user.getRole().name())
				.createdAt(user.getCreatedAt())
				.build();
	}

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String username = request.getUsername().trim().toLowerCase();
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("USERNAME_ALREADY_EXISTS", "El nombre de usuario ya está registrado");
		}
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("EMAIL_ALREADY_EXISTS", "El email ya está registrado");
		}
		// El rol siempre es USER: un cliente nunca puede auto-asignarse ADMIN
		User user = User.builder()
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(request.getPassword()))
				.fullName(request.getFullName().trim())
				.phone(request.getPhone())
				.role(Role.USER)
				.createdAt(LocalDateTime.now())
				.build();
		userRepository.save(user);
		return new AuthResponse(jwtService.generateToken(user), convertEntityToDTO(user));
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String login = request.getUsername().trim().toLowerCase();
		User user = userRepository.findByUsernameOrEmail(login, login)
				.filter(found -> passwordEncoder.matches(request.getPassword(), found.getPassword()))
				// Mismo mensaje para usuario inexistente y contraseña errónea (no revela qué cuentas existen)
				.orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
		return new AuthResponse(jwtService.generateToken(user), convertEntityToDTO(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileDTO getProfile(String username) {
		return userRepository.findByUsername(username)
				.map(AuthServiceImp::convertEntityToDTO)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
	}

}
