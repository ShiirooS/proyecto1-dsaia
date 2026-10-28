package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-04 (CA-04.11): el login no permite averiguar qué cuentas existen.
 * Mismo cuerpo de respuesta y el mismo trabajo de BCrypt para usuario inexistente y contraseña errónea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginEnumerationTest {

	private static final String EXPECTED = "{\"message\":\"Usuario o contraseña incorrectos\",\"code\":\"INVALID_CREDENTIALS\"}";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@SpyBean
	private PasswordEncoder passwordEncoder;

	@Test
	void usuarioInexistenteYPasswordErroneaSonIndistinguibles() throws Exception {
		userRepository.save(User.builder().username("enum-real@correo.com").password(passwordEncoder.encode("Clave1234"))
				.fullName("Real").role(RoleType.USER).createdAt(LocalDateTime.now()).build());

		clearInvocations(passwordEncoder);
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"enum-real@correo.com\",\"password\":\"Incorrecta1\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().json(EXPECTED, true));
		verify(passwordEncoder, times(1)).matches(any(), any());

		clearInvocations(passwordEncoder);
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"enum-no-existe@correo.com\",\"password\":\"Incorrecta1\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().json(EXPECTED, true));
		// DaoAuthenticationProvider compara contra un hash ficticio aunque el usuario no exista
		verify(passwordEncoder, times(1)).matches(any(), any());
	}
}
