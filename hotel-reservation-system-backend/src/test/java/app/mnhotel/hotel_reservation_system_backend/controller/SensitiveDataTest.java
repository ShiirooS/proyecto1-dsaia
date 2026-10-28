package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentMethodRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-01 (CA-01.9, RN-01.1): ni el número completo ni el CVV llegan a la base de datos ni a los logs,
 * aunque Spring registre en DEBUG los errores de validación (hallazgo de la revisión del PR).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "logging.level.org.springframework.web=DEBUG")
class SensitiveDataTest {

	private static final String PAN = "4242424242424242";
	private static final String CVV = "987";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private HotelRepository hotelRepository;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private PaymentMethodRepository paymentMethodRepository;

	private User user;
	private Room room;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.builder().username("pci-user@correo.com").password(passwordEncoder.encode("Clave1234"))
				.fullName("PCI").role(RoleType.USER).createdAt(LocalDateTime.now()).build());
		Hotel hotel = hotelRepository.save(Hotel.builder().name("Hotel PCI").location("Panamá").build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("PCI-1").type("DOBLE").pricePerNight(100)
				.capacity(2).availbility(true).build());
	}

	@AfterEach
	void tearDown() {
		paymentRepository.deleteAll(paymentRepository.findAll().stream()
				.filter(p -> p.getReservation() != null && p.getReservation().getRoom().getId().equals(room.getId())).toList());
		reservationRepository.deleteAll(reservationRepository.findAll().stream()
				.filter(r -> r.getRoom().getId().equals(room.getId())).toList());
		paymentMethodRepository.deleteAll(paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(user.getId()));
		roomRepository.delete(room);
		hotelRepository.deleteById(room.getHotel().getId());
		userRepository.delete(user);
	}

	private String token() throws Exception {
		String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", user.getUsername(), "password", "Clave1234"))))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	private String body(String cardNumber, String cvv) {
		return "{\"roomId\":" + room.getId() + ",\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-22\",\"guests\":1,"
				+ "\"card\":{\"cardNumber\":\"" + cardNumber + "\",\"expMonth\":12,\"expYear\":2028,\"cvv\":\"" + cvv
				+ "\",\"holderName\":\"PCI\"},\"saveCard\":true}";
	}

	@Test
	void ningunaTablaGuardaElNumeroNiElCvv() throws Exception {
		mockMvc.perform(post("/api/reservation/book").header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON).content(body("4242 4242 4242 4242", CVV)))
				.andExpect(status().isCreated());

		for (String table : List.of("payment_method", "payments", "reservations")) {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from " + table);
			assertThat(rows).as(table).isNotEmpty();
			String dump = rows.toString();
			assertThat(dump).as(table).doesNotContain(PAN);
			assertThat(dump).as(table).doesNotContain("cvv");
		}
		// El CVV ni siquiera tiene columna; el número solo aparece como últimos 4
		assertThat(jdbcTemplate.queryForList("select last4 from payment_method", String.class)).contains("4242");
	}

	@Test
	void formatoInvalidoResponde400SinRegistrarElValor(CapturedOutput output) throws Exception {
		String malformedPan = "4242.4242.4242.4242";
		String malformedCvv = "98x";
		mockMvc.perform(post("/api/reservation/book").header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON).content(body(malformedPan, CVV)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/reservation/book").header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON).content(body(PAN, malformedCvv)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		// 12 dígitos: es formato (400), no una tarjeta inválida por Luhn (422)
		mockMvc.perform(post("/api/reservation/book").header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON).content(body("424242424242", CVV)))
				.andExpect(status().isBadRequest());

		assertThat(output.getAll()).doesNotContain(malformedPan).doesNotContain(malformedCvv).doesNotContain(PAN);
	}
}
