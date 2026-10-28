package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HU-00: pruebas de caracterización de la API heredada de la rama developer.
 * Fijan el comportamiento actual (rutas y formas que usa el frontend) antes de modificarlo en las HU siguientes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExistingApiCharacterizationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String register(String username, String password) throws Exception {
		String body = """
				{"username":"%s","password":"%s","fullName":"Ana Pérez"}
				""".formatted(username, password);
		String json = mockMvc.perform(post("/auth/registration-user").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(emptyOrNullString())))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	@Test
	void registroYLoginDevuelvenToken() throws Exception {
		register("ana@correo.com", "Clave1234");

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"ana@correo.com\",\"password\":\"Clave1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(emptyOrNullString())));
	}

	@Test
	void registroDuplicadoDevuelve400ConMensajeEnToken() throws Exception {
		register("dup@correo.com", "Clave1234");

		// Forma heredada: el mensaje de error viaja en el campo "token" de AuthResponse
		mockMvc.perform(post("/auth/registration-user").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"dup@correo.com\",\"password\":\"Otra1234\",\"fullName\":\"X\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.token").value("El nombre de usuario ya está en uso"));
	}

	@Test
	void loginConPasswordIncorrectaEsRechazado() throws Exception {
		register("luis@correo.com", "Clave1234");

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"luis@correo.com\",\"password\":\"Incorrecta1\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void usuarioActualConToken() throws Exception {
		String token = register("marta@correo.com", "Clave1234");

		mockMvc.perform(get("/api/user/current").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("marta@correo.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void usuarioAutenticadoRegistraHotelYElListadoEsPublico() throws Exception {
		String token = register("gestor@correo.com", "Clave1234");

		mockMvc.perform(post("/api/hotel/registration-hotel").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Hotel Caracterización\",\"location\":\"Panamá\",\"description\":\"X\",\"rating\":4.5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("El Hotel se registró correctamente"));

		mockMvc.perform(get("/api/hotel/get-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItem("Hotel Caracterización")));
	}

	@Test
	void escrituraSinTokenEsRechazada() throws Exception {
		mockMvc.perform(post("/api/hotel/registration-hotel").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Anónimo\"}"))
				.andExpect(status().isForbidden());
	}
}
