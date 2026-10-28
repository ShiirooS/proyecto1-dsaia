package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de regresión del CRUD de hoteles que ya existía en el repositorio original.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registraYListaUnHotel() throws Exception {
		String body = """
				{"name":"Hotel Prueba","location":"Panamá","description":"Frente al mar","rating":4.5}
				""";
		mockMvc.perform(post("/api/hotel/registration-hotel").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("El Hotel se registró correctamente"));

		mockMvc.perform(get("/api/hotel/get-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItem("Hotel Prueba")));
	}

	@Test
	void hotelInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/api/hotel/get-hotel/{id}", 999999))
				.andExpect(status().isNotFound());
	}
}
