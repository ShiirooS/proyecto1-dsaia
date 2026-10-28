package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Datos semilla del perfil dev (H2 en memoria). Nunca corre con el perfil postgres ni en tests.
 * Credenciales de demostración documentadas en docs/user-stories/HU-04.md (no se escriben en el log).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	public static final String ADMIN_USERNAME = "admin@hotel.com";
	public static final String GUEST_USERNAME = "demo@hotel.com";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser(ADMIN_USERNAME, "Admin12345", "Administrador Hotel MN", RoleType.ADMIN);
		createUser(GUEST_USERNAME, "Demo12345", "Huésped Demo", RoleType.USER);
		log.info("Datos semilla cargados (usuarios {} y {})", ADMIN_USERNAME, GUEST_USERNAME);
	}

	private User createUser(String username, String password, String fullName, RoleType role) {
		return userRepository.save(User.builder()
				.username(username)
				.password(passwordEncoder.encode(password))
				.fullName(fullName)
				.role(role)
				.createdAt(LocalDateTime.now())
				.build());
	}
}
