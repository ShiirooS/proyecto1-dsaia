package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.entity.Role;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-04: registro e inicio de sesión con JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private String registerBody(String username, String email, String password) {
		return """
				{"username":"%s","email":"%s","password":"%s","fullName":"Ana Pérez","phone":"+507 6111-2222"}
				""".formatted(username, email, password);
	}

	private String register(String username, String email) throws Exception {
		String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody(username, email, "Clave1234")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	@Test
	void registroDevuelveTokenYPerfilSinPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("ana", "ana@correo.com", "Clave1234")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token", not(emptyOrNullString())))
				.andExpect(jsonPath("$.user.username").value("ana"))
				.andExpect(jsonPath("$.user.role").value("USER"))
				.andExpect(jsonPath("$.user.password").doesNotExist());

		// La contraseña se guarda con hash BCrypt, nunca en texto plano
		assertThat(userRepository.findByUsername("ana").orElseThrow().getPassword()).startsWith("$2");
	}

	@Test
	void registroIgnoraIntentoDeAutoAsignarseAdmin() throws Exception {
		String body = """
				{"username":"intruso","email":"intruso@correo.com","password":"Clave1234","fullName":"X","role":"ADMIN"}
				""";
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.role").value("USER"));
		assertThat(userRepository.findByUsername("intruso").orElseThrow().getRole()).isEqualTo(Role.USER);
	}

	@Test
	void registroDuplicadoDevuelve409() throws Exception {
		register("duplicado", "dup@correo.com");
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("duplicado", "otro@correo.com", "Clave1234")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El nombre de usuario ya está registrado"));
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("otro", "dup@correo.com", "Clave1234")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El email ya está registrado"));
	}

	@Test
	void registroConPasswordDebilDevuelve400() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("debil", "debil@correo.com", "corta")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("debil", "no-es-email", "Clave1234")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginConUsuarioOEmailYCredencialesInvalidas() throws Exception {
		register("luis", "luis@correo.com");

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"luis\",\"password\":\"Clave1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(emptyOrNullString())));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"LUIS@correo.com\",\"password\":\"Clave1234\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"luis\",\"password\":\"Incorrecta1\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"noexiste\",\"password\":\"Clave1234\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
	}

	@Test
	void perfilRequiereTokenValido() throws Exception {
		String token = register("marta", "marta@correo.com");

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("marta"))
				.andExpect(jsonPath("$.email").value("marta@correo.com"));
	}

	@Test
	void usuarioNormalNoPuedeUsarOperacionesDeAdmin() throws Exception {
		String token = register("pedro", "pedro@correo.com");
		JsonNode hotel = objectMapper.readTree("{\"name\":\"Hack\",\"location\":\"X\",\"description\":\"X\",\"rating\":1}");

		mockMvc.perform(post("/api/hotel/registration-hotel").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(hotel.toString()))
				.andExpect(status().isForbidden());
	}
}
