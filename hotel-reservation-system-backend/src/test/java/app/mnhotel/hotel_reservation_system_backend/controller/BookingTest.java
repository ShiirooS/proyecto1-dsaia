package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentMethodRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-01: reservar con los datos de pago guardados (CA-01.1 a CA-01.12 y CA-01.15).
 * Hoy es 28/10/2026 (FixedClockConfig); la habitación cuesta 150 por noche y admite 2 huéspedes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class BookingTest {

	private static final String PASSWORD = "Clave1234";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
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

	private User ana;
	private User beto;
	private User admin;
	private Room room;
	private PaymentMethod anaVisa;

	@BeforeEach
	void setUp() {
		ana = createUser("book-ana@correo.com", RoleType.USER);
		beto = createUser("book-beto@correo.com", RoleType.USER);
		admin = createUser("book-admin@correo.com", RoleType.ADMIN);
		Hotel hotel = hotelRepository.save(Hotel.builder().name("Hotel Booking").location("Panamá").build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("BK-3").type("DOBLE").pricePerNight(150)
				.capacity(2).availbility(true).image("https://img/bk3.jpg").build());
		anaVisa = paymentMethodRepository.save(PaymentMethod.builder().user(ana).token("tok_ana_visa_000000000000")
				.brand(CardBrand.VISA).last4("4242").expMonth(12).expYear(2028).holderName("ANA").isDefault(true)
				.createdAt(LocalDateTime.of(2026, 10, 1, 9, 0)).build());
	}

	@AfterEach
	void tearDown() {
		List<Reservation> reservations = reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId())).toList();
		paymentRepository.deleteAll(paymentRepository.findAll().stream()
				.filter(p -> p.getReservation() != null && reservations.stream().anyMatch(r -> r.getId().equals(p.getReservation().getId())))
				.toList());
		reservationRepository.deleteAll(reservations);
		for (User user : List.of(ana, beto, admin)) {
			paymentMethodRepository.deleteAll(paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(user.getId()));
		}
		roomRepository.delete(room);
		hotelRepository.deleteById(room.getHotel().getId());
		userRepository.deleteAll(List.of(ana, beto, admin));
	}

	private User createUser(String username, RoleType role) {
		return userRepository.save(User.builder().username(username).password(passwordEncoder.encode(PASSWORD))
				.fullName(username).role(role).createdAt(LocalDateTime.now()).build());
	}

	private String login(User user) throws Exception {
		String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", user.getUsername(), "password", PASSWORD))))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private ResultActions book(String token, String body) throws Exception {
		return mockMvc.perform(as(token, post("/api/reservation/book")).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String stay(String extra) {
		return "{\"roomId\":" + room.getId() + ",\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-23\",\"guests\":2," + extra + "}";
	}

	private String card(String number, int month, int year, String cvv) {
		return "\"card\":{\"cardNumber\":\"" + number + "\",\"expMonth\":" + month + ",\"expYear\":" + year
				+ ",\"cvv\":\"" + cvv + "\",\"holderName\":\"Ana Pérez\"}";
	}

	private long reservationsInRoom() {
		return reservationRepository.findAll().stream().filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId()))
				.count();
	}

	@Test
	void reservaEnUnClicConLaTarjetaGuardada() throws Exception {
		String response = book(login(ana), stay("\"paymentMethodId\":" + anaVisa.getId()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/api/reservation/my-reservations/")))
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.phase").value("UPCOMING"))
				.andExpect(jsonPath("$.nights").value(3))
				.andExpect(jsonPath("$.totalAmount").value(450))
				.andExpect(jsonPath("$.payment.status").value("COMPLETED"))
				.andExpect(jsonPath("$.payment.cardBrand").value("VISA"))
				.andExpect(jsonPath("$.payment.cardLast4").value("4242"))
				.andExpect(jsonPath("$.payment.transactionId").value(containsString("txn_")))
				.andExpect(jsonPath("$.cancellation.refundable").value(true))
				.andExpect(jsonPath("$.cancellation.refundDeadline").value("2026-12-18T15:00:00"))
				.andExpect(jsonPath("$.savedPaymentMethodId").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		long id = objectMapper.readTree(response).get("id").asLong();
		Reservation saved = reservationRepository.findById(id).orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(ana.getId());
		assertThat(saved.getGuests()).isEqualTo(2);
	}

	@Test
	void tarjetaNuevaGuardadaQuedaPredeterminadaParaLaProxima() throws Exception {
		String token = login(beto);
		String response = book(token, stay(card("5555 5555 5555 4444", 6, 2029, "321") + ",\"saveCard\":true"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.payment.cardBrand").value("MASTERCARD"))
				.andExpect(jsonPath("$.savedPaymentMethodId").isNumber())
				.andReturn().getResponse().getContentAsString();
		long savedId = objectMapper.readTree(response).get("savedPaymentMethodId").asLong();

		mockMvc.perform(as(token, get("/api/payment-method/get-all")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(savedId))
				.andExpect(jsonPath("$[0].last4").value("4444"))
				.andExpect(jsonPath("$[0].isDefault").value(true))
				.andExpect(jsonPath("$[0].token").doesNotExist());
	}

	@Test
	void tarjetaNuevaSinGuardarNoQuedaRegistrada() throws Exception {
		String token = login(beto);
		book(token, stay(card("5555555555554444", 6, 2029, "321"))).andExpect(status().isCreated());

		mockMvc.perform(as(token, get("/api/payment-method/get-all")))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void pagoRechazadoNoPersisteNada() throws Exception {
		long paymentsBefore = paymentRepository.count();
		book(login(beto), stay(card("4000000000000002", 1, 2030, "123") + ",\"saveCard\":true"))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value("PAYMENT_DECLINED"));

		assertThat(reservationsInRoom()).isZero();
		assertThat(paymentRepository.count()).isEqualTo(paymentsBefore);
		assertThat(paymentMethodRepository.countByUserId(beto.getId())).isZero();
	}

	@Test
	void tarjetaInvalidaOVencida() throws Exception {
		String token = login(beto);
		book(token, stay(card("4242424242424241", 12, 2028, "123")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("CARD_INVALID"));
		book(token, stay(card("4242424242424242", 9, 2026, "123")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("CARD_EXPIRED"));
	}

	@Test
	void fechasOcupadasMientrasPagabaResponde409SinCobrar() throws Exception {
		reservationRepository.save(Reservation.builder().room(room).user(beto).startDate(LocalDate.of(2026, 12, 21))
				.endDate(LocalDate.of(2026, 12, 22)).status(ReservationStatus.CONFIRMED).createdAt(LocalDateTime.now()).build());
		long paymentsBefore = paymentRepository.count();

		book(login(ana), stay("\"paymentMethodId\":" + anaVisa.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));
		assertThat(paymentRepository.count()).isEqualTo(paymentsBefore);
	}

	@Test
	void dobleClicSoloCreaUnaReserva() throws Exception {
		String token = login(ana);
		String body = stay("\"paymentMethodId\":" + anaVisa.getId());
		book(token, body).andExpect(status().isCreated());
		book(token, body).andExpect(status().isConflict());
		assertThat(reservationsInRoom()).isEqualTo(1);
	}

	@Test
	void fuenteDePagoAmbigua() throws Exception {
		String token = login(ana);
		book(token, stay("\"paymentMethodId\":" + anaVisa.getId() + "," + card("5555555555554444", 6, 2029, "321")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAYMENT_SOURCE"));
		book(token, stay("\"saveCard\":false"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAYMENT_SOURCE"));
		book(token, stay("\"paymentMethodId\":" + anaVisa.getId() + ",\"saveCard\":true"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAYMENT_SOURCE"));
	}

	@Test
	void reglasDeEstancia() throws Exception {
		String token = login(ana);
		String pm = ",\"paymentMethodId\":" + anaVisa.getId() + "}";
		String base = "{\"roomId\":" + room.getId() + ",";
		book(token, base + "\"checkIn\":\"2026-10-27\",\"checkOut\":\"2026-10-29\",\"guests\":1" + pm)
				.andExpect(jsonPath("$.code").value("CHECKIN_IN_PAST"));
		book(token, base + "\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-20\",\"guests\":1" + pm)
				.andExpect(jsonPath("$.code").value("INVALID_STAY_DATES"));
		book(token, base + "\"checkIn\":\"2026-12-01\",\"checkOut\":\"2027-01-01\",\"guests\":1" + pm)
				.andExpect(jsonPath("$.code").value("MAX_NIGHTS_EXCEEDED"));
		book(token, base + "\"checkIn\":\"2027-12-01\",\"checkOut\":\"2027-12-02\",\"guests\":1" + pm)
				.andExpect(jsonPath("$.code").value("BOOKING_TOO_FAR"));
		book(token, base + "\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-23\",\"guests\":3" + pm)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("GUESTS_EXCEED_CAPACITY"));
	}

	@Test
	void noSeAlmacenanNiExponenDatosSensibles() throws Exception {
		String token = login(beto);
		String response = book(token, stay(card("4242 4242 4242 4242", 12, 2028, "987") + ",\"saveCard\":true"))
				.andExpect(status().isCreated())
				.andExpect(content().string(not(containsString("tok_"))))
				.andReturn().getResponse().getContentAsString();

		PaymentMethod saved = paymentMethodRepository.findById(objectMapper.readTree(response).get("savedPaymentMethodId").asLong())
				.orElseThrow();
		assertThat(saved.toString()).doesNotContain("4242424242424242").doesNotContain("987").doesNotContain("tok_");
		assertThat(saved.getLast4()).isEqualTo("4242");
		assertThat(saved.getToken()).startsWith("tok_");
		paymentRepository.findAll().forEach(payment -> assertThat(payment.toString()).doesNotContain("4242424242424242"));

		mockMvc.perform(as(token, get("/api/payment-method/get-all")))
				.andExpect(content().string(not(containsString("tok_"))));
	}

	@Test
	void gestionDeTarjetasPredeterminadaYBorrado() throws Exception {
		String token = login(ana);
		String created = mockMvc.perform(as(token, post("/api/payment-method/registration-payment-method"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardNumber\":\"5555555555554444\",\"expMonth\":6,\"expYear\":2029,\"cvv\":\"321\",\"holderName\":\"Ana\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.isDefault").value(false))
				.andReturn().getResponse().getContentAsString();
		long mastercardId = objectMapper.readTree(created).get("id").asLong();

		mockMvc.perform(as(token, put("/api/payment-method/set-default/{id}", mastercardId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isDefault").value(true));
		JsonNode list = objectMapper.readTree(mockMvc.perform(as(token, get("/api/payment-method/get-all")))
				.andReturn().getResponse().getContentAsString());
		assertThat(list).hasSize(2);
		assertThat(list.get(0).get("id").asLong()).isEqualTo(mastercardId);
		assertThat(list.get(1).get("isDefault").asBoolean()).isFalse();

		mockMvc.perform(as(token, delete("/api/payment-method/delete-payment-method/{id}", mastercardId)))
				.andExpect(status().isOk());
		mockMvc.perform(as(token, get("/api/payment-method/get-all")))
				.andExpect(jsonPath("$[0].id").value(anaVisa.getId()))
				.andExpect(jsonPath("$[0].isDefault").value(true));
	}

	@Test
	void tarjetaDuplicadaResponde409PeroAlReservarSeReutiliza() throws Exception {
		String token = login(ana);
		mockMvc.perform(as(token, post("/api/payment-method/registration-payment-method")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardNumber\":\"4242424242424242\",\"expMonth\":12,\"expYear\":2028,\"cvv\":\"123\",\"holderName\":\"Ana\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT_METHOD"));

		book(token, stay(card("4242424242424242", 12, 2028, "123") + ",\"saveCard\":true"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.savedPaymentMethodId").value(anaVisa.getId()));
	}

	@Test
	void tarjetaOReservaDeOtroUsuarioResponden404() throws Exception {
		String betoToken = login(beto);
		book(betoToken, stay("\"paymentMethodId\":" + anaVisa.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_FOUND"));
		mockMvc.perform(as(betoToken, delete("/api/payment-method/delete-payment-method/{id}", anaVisa.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_FOUND"));

		String response = book(login(ana), stay("\"paymentMethodId\":" + anaVisa.getId()))
				.andReturn().getResponse().getContentAsString();
		long id = objectMapper.readTree(response).get("id").asLong();
		mockMvc.perform(as(betoToken, get("/api/reservation/my-reservations/{id}", id)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
	}

	@Test
	void sinSesionNoSePuedeReservar() throws Exception {
		mockMvc.perform(post("/api/reservation/book").contentType(MediaType.APPLICATION_JSON)
				.content(stay("\"paymentMethodId\":1")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void elAdminVeElPagoConMarcaYUltimos4() throws Exception {
		book(login(ana), stay("\"paymentMethodId\":" + anaVisa.getId())).andExpect(status().isCreated());

		JsonNode payments = objectMapper.readTree(mockMvc.perform(as(login(admin), get("/api/payment/get-all")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
		assertThat(payments).anyMatch(p -> p.get("amount").asInt() == 450 && "COMPLETED".equals(p.get("status").asText())
				&& "VISA".equals(p.get("cardBrand").asText()) && "4242".equals(p.get("cardLast4").asText()));
	}
}
