package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HU-04: alta del primer ADMIN en PostgreSQL. Ningún endpoint crea administradores, así que la cuenta
 * se crea al arrancar a partir de ADMIN_USERNAME y ADMIN_PASSWORD. Es idempotente: si ya existe, no hace nada.
 */
@Slf4j
@Component
@Profile("postgres")
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${ADMIN_USERNAME:}")
	private String adminUsername;

	@Value("${ADMIN_PASSWORD:}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
			log.info("AdminBootstrap: ADMIN_USERNAME/ADMIN_PASSWORD no definidos, no se crea administrador");
			return;
		}
		String username = adminUsername.trim().toLowerCase();
		if (userRepository.existsByUsername(username)) {
			return;
		}
		userRepository.save(User.builder()
				.username(username)
				.password(passwordEncoder.encode(adminPassword))
				.fullName("Administrador")
				.role(RoleType.ADMIN)
				.createdAt(LocalDateTime.now())
				.build());
		log.info("AdminBootstrap: administrador {} creado", username);
	}
}
