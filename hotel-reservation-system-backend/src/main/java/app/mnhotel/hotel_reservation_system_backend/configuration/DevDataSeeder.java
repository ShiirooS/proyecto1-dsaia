package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.entity.Role;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Datos semilla para el perfil dev (H2 en memoria). Nunca se ejecuta con el perfil postgres ni en tests.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "Admin1234";
	public static final String GUEST_USERNAME = "huesped";
	public static final String GUEST_PASSWORD = "Huesped123";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser(ADMIN_USERNAME, "admin@hotelmn.com", ADMIN_PASSWORD, "Administrador Hotel MN", Role.ADMIN);
		createUser(GUEST_USERNAME, "huesped@correo.com", GUEST_PASSWORD, "Huésped Recurrente", Role.USER);
		log.info("Datos semilla cargados. Admin: {} / {} - Huésped: {} / {}",
				ADMIN_USERNAME, ADMIN_PASSWORD, GUEST_USERNAME, GUEST_PASSWORD);
	}

	private User createUser(String username, String email, String password, String fullName, Role role) {
		return userRepository.save(User.builder()
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(password))
				.fullName(fullName)
				.phone("+507 6000-0000")
				.role(role)
				.createdAt(LocalDateTime.now())
				.build());
	}
}
