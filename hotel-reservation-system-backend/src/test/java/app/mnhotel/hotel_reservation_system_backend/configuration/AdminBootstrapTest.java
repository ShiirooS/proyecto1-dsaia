package app.mnhotel.hotel_reservation_system_backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-04 (CA-04.13): alta idempotente del primer ADMIN en el perfil postgres.
 */
class AdminBootstrapTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final AdminBootstrap bootstrap = new AdminBootstrap(userRepository, new BCryptPasswordEncoder());

	private void configure(String username, String password) {
		ReflectionTestUtils.setField(bootstrap, "adminUsername", username);
		ReflectionTestUtils.setField(bootstrap, "adminPassword", password);
	}

	@Test
	void creaElAdminConPasswordHasheadaYUsernameEnMinusculas() {
		configure("Jefe@HotelMN.com", "Admin12345");
		when(userRepository.existsByUsername("jefe@hotelmn.com")).thenReturn(false);

		bootstrap.run(null);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getUsername()).isEqualTo("jefe@hotelmn.com");
		assertThat(saved.getValue().getRole()).isEqualTo(RoleType.ADMIN);
		assertThat(saved.getValue().getPassword()).startsWith("$2").isNotEqualTo("Admin12345");
	}

	@Test
	void esIdempotenteSiElAdminYaExiste() {
		configure("jefe@hotelmn.com", "Admin12345");
		when(userRepository.existsByUsername("jefe@hotelmn.com")).thenReturn(true);

		bootstrap.run(null);

		verify(userRepository, never()).save(any());
	}

	@Test
	void sinVariablesNoCreaNada() {
		configure("", "");

		bootstrap.run(null);

		verify(userRepository, never()).save(any());
	}
}
