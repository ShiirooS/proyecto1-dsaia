package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.MutableClockConfig;
import app.mnhotel.hotel_reservation_system_backend.MutableClockConfig.MutableClock;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Payment;
import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import app.mnhotel.hotel_reservation_system_backend.enums.PaymentStatus;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentMethodRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-05: historial de mis reservas y cancelación según la política de 48 h (CA-05.1 a CA-05.8, lado API).
 * El reloj arranca el 28/10/2026 10:00 y cada test lo mueve para cancelar en el instante que necesita.
 * Contexto común de la HU: reserva del 20 al 23/12/2026 a 150 por noche ($450) con VISA ••••4242; check-in a las 15:00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MutableClockConfig.class)
class MyReservationsTest {

	private static final String PASSWORD = "Clave1234";
	private static final LocalDateTime REFUND_DEADLINE = LocalDateTime.of(2026, 12, 18, 15, 0);

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private MutableClock clock;
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
		clock.setTo(FixedClockConfig.NOW);
		ana = createUser("mine-ana@correo.com", RoleType.USER);
		beto = createUser("mine-beto@correo.com", RoleType.USER);
		admin = createUser("mine-admin@correo.com", RoleType.ADMIN);
		Hotel hotel = hotelRepository.save(Hotel.builder().name("Hotel Mar Azul").location("Ciudad de Panamá").build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("MR-3").type("DOBLE").pricePerNight(150)
				.capacity(2).availbility(true).image("https://img/mr3.jpg").build());
		anaVisa = paymentMethodRepository.save(PaymentMethod.builder().user(ana).token("tok_mine_ana_visa_0000000000")
				.brand(CardBrand.VISA).last4("4242").expMonth(12).expYear(2028).holderName("ANA").isDefault(true)
				.createdAt(LocalDateTime.of(2026, 10, 1, 9, 0)).build());
	}

	@AfterEach
	void tearDown() {
		clock.setTo(FixedClockConfig.NOW);
		List<Reservation> reservations = reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId())).toList();
		paymentRepository.deleteAll(paymentRepository.findAll().stream()
				.filter(p -> p.getReservation() != null
						&& reservations.stream().anyMatch(r -> r.getId().equals(p.getReservation().getId())))
				.toList());
		reservationRepository.deleteAll(reservations);
		paymentMethodRepository.deleteAll(paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(ana.getId()));
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

	/** Reserva pagada con la VISA guardada de Ana; devuelve el id. */
	private long bookAsAna(String token, String checkIn, String checkOut) throws Exception {
		String body = "{\"roomId\":" + room.getId() + ",\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut
				+ "\",\"guests\":2,\"paymentMethodId\":" + anaVisa.getId() + "}";
		String response = mockMvc.perform(as(token, post("/api/reservation/book")).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("id").asLong();
	}

	/** Reserva histórica insertada directamente (no se puede reservar en el pasado por la API). */
	private Reservation insertReservation(User owner, LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
		Reservation reservation = reservationRepository.save(Reservation.builder().room(room).user(owner).startDate(checkIn)
				.endDate(checkOut).guests(1).totalAmount(300).status(status).createdAt(checkIn.minusDays(30).atStartOfDay())
				.cancelledAt(status == ReservationStatus.CANCELLED ? checkIn.minusDays(10).atStartOfDay() : null).build());
		paymentRepository.save(Payment.builder().reservation(reservation).amount(300)
				.status(status == ReservationStatus.CANCELLED ? PaymentStatus.REFUNDED : PaymentStatus.COMPLETED)
				.refundedAmount(status == ReservationStatus.CANCELLED ? 300 : 0).cardBrand("VISA").cardLast4("4242")
				.transactionId("txn_hist" + checkIn).paymentDate(checkIn.minusDays(30).atStartOfDay()).build());
		return reservation;
	}

	private ResultActions cancel(String token, long id) throws Exception {
		return mockMvc.perform(as(token, put("/api/reservation/cancel-reservation/{id}", id)));
	}

	private Payment paymentOf(long reservationId) {
		return paymentRepository.findAll().stream()
				.filter(p -> p.getReservation() != null && p.getReservation().getId().equals(reservationId))
				.findFirst().orElseThrow();
	}

	@Test
	void historialPropioOrdenadoPorLlegadaConFaseYDatosDePago() throws Exception {
		String anaToken = login(ana);
		long december = bookAsAna(anaToken, "2026-12-20", "2026-12-23");
		long november = bookAsAna(anaToken, "2026-11-10", "2026-11-12");
		Reservation past = insertReservation(ana, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), ReservationStatus.CONFIRMED);
		Reservation inStay = insertReservation(ana, LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 30), ReservationStatus.CONFIRMED);
		Reservation cancelled = insertReservation(ana, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), ReservationStatus.CANCELLED);
		Reservation betos = insertReservation(beto, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), ReservationStatus.CONFIRMED);

		mockMvc.perform(as(anaToken, get("/api/reservation/my-reservations")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[*].id", contains((int) december, (int) november, cancelled.getId().intValue(),
						inStay.getId().intValue(), past.getId().intValue())))
				.andExpect(jsonPath("$[*].phase", contains("UPCOMING", "UPCOMING", "CANCELLED", "IN_STAY", "COMPLETED")))
				.andExpect(jsonPath("$[*].id", not(hasItem(betos.getId().intValue()))))
				// Cada tarjeta tiene hotel, habitación, fechas, noches, huéspedes, total, tarjeta usada y política
				.andExpect(jsonPath("$[0].hotel.name").value("Hotel Mar Azul"))
				.andExpect(jsonPath("$[0].room.roomNumber").value("MR-3"))
				.andExpect(jsonPath("$[0].checkIn").value("2026-12-20"))
				.andExpect(jsonPath("$[0].nights").value(3))
				.andExpect(jsonPath("$[0].guests").value(2))
				.andExpect(jsonPath("$[0].totalAmount").value(450))
				.andExpect(jsonPath("$[0].payment.cardBrand").value("VISA"))
				.andExpect(jsonPath("$[0].payment.cardLast4").value("4242"))
				.andExpect(jsonPath("$[0].cancellation.cancellable").value(true))
				.andExpect(jsonPath("$[0].cancellation.refundDeadline").value("2026-12-18T15:00:00"))
				.andExpect(jsonPath("$[0].cancellation.refundAmountIfCancelledNow").value(450))
				.andExpect(jsonPath("$[2].cancelledAt").isNotEmpty())
				.andExpect(jsonPath("$[2].payment.status").value("REFUNDED"))
				.andExpect(jsonPath("$[3].cancellation.cancellable").value(false))
				.andExpect(jsonPath("$[4].cancellation.cancellable").value(false));
	}

	@Test
	void sinReservasDevuelveListaVacia() throws Exception {
		mockMvc.perform(as(login(beto), get("/api/reservation/my-reservations")))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void cancelarExactamente48HorasAntesReembolsaElTotal() throws Exception {
		String token = login(ana);
		long id = bookAsAna(token, "2026-12-20", "2026-12-23");
		clock.setTo(REFUND_DEADLINE);

		mockMvc.perform(as(token, get("/api/reservation/my-reservations/{id}", id)))
				.andExpect(jsonPath("$.cancellation.refundable").value(true))
				.andExpect(jsonPath("$.cancellation.refundAmountIfCancelledNow").value(450));

		cancel(token, id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.phase").value("CANCELLED"))
				.andExpect(jsonPath("$.cancelledAt").value("2026-12-18T15:00:00"))
				.andExpect(jsonPath("$.payment.status").value("REFUNDED"))
				.andExpect(jsonPath("$.payment.refundedAmount").value(450))
				.andExpect(jsonPath("$.cancellation.cancellable").value(false))
				.andExpect(jsonPath("$.cancellation.refundAmountIfCancelledNow").value(0));

		assertThat(reservationRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		Payment payment = paymentOf(id);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		assertThat(payment.getRefundedAmount()).isEqualTo(450);
	}

	@Test
	void cancelarUnSegundoDespuesDelLimiteNoReembolsa() throws Exception {
		String token = login(ana);
		long id = bookAsAna(token, "2026-12-20", "2026-12-23");
		clock.setTo(REFUND_DEADLINE.plusSeconds(1));

		mockMvc.perform(as(token, get("/api/reservation/my-reservations/{id}", id)))
				.andExpect(jsonPath("$.cancellation.cancellable").value(true))
				.andExpect(jsonPath("$.cancellation.refundable").value(false))
				.andExpect(jsonPath("$.cancellation.refundAmountIfCancelledNow").value(0));

		cancel(token, id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.payment.status").value("COMPLETED"))
				.andExpect(jsonPath("$.payment.refundedAmount").value(0));

		assertThat(paymentOf(id).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	void desdeLaHoraDeCheckInYaNoSePuedeCancelar() throws Exception {
		String token = login(ana);
		long id = bookAsAna(token, "2026-12-20", "2026-12-23");
		clock.setTo(LocalDateTime.of(2026, 12, 20, 15, 0));

		mockMvc.perform(as(token, get("/api/reservation/my-reservations/{id}", id)))
				.andExpect(jsonPath("$.phase").value("IN_STAY"))
				.andExpect(jsonPath("$.cancellation.cancellable").value(false));
		cancel(token, id)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_CANCELLABLE"));

		assertThat(reservationRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(paymentOf(id).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	void cancelarDosVecesResponde409YNoReembolsaDosVeces() throws Exception {
		String token = login(ana);
		long id = bookAsAna(token, "2026-12-20", "2026-12-23");
		cancel(token, id).andExpect(status().isOk());

		cancel(token, id)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION_ALREADY_CANCELLED"));
		assertThat(paymentOf(id).getRefundedAmount()).isEqualTo(450);
	}

	@Test
	void laReservaDeOtroUsuarioNoSeVeNiSeCancela() throws Exception {
		long id = bookAsAna(login(ana), "2026-12-20", "2026-12-23");
		String betoToken = login(beto);

		mockMvc.perform(as(betoToken, get("/api/reservation/my-reservations/{id}", id)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
		cancel(betoToken, id)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
		// Solo el dueño cancela, también frente a un ADMIN (matriz RBAC "D")
		cancel(login(admin), id)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
		mockMvc.perform(as(betoToken, get("/api/reservation/my-reservations")))
				.andExpect(content().json("[]"));

		assertThat(reservationRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}

	@Test
	void idInexistenteOInvalido() throws Exception {
		String token = login(ana);
		cancel(token, 999_999L)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
		mockMvc.perform(as(token, put("/api/reservation/cancel-reservation/abc")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
	}

	@Test
	void cancelarLiberaLasNochesParaOtroCliente() throws Exception {
		String anaToken = login(ana);
		long id = bookAsAna(anaToken, "2026-12-20", "2026-12-23");
		String availability = "/api/room/get-availability/{id}?from=2026-12-01&to=2026-12-31";
		mockMvc.perform(get(availability, room.getId()))
				.andExpect(jsonPath("$.occupiedDates", hasItems("2026-12-20", "2026-12-21", "2026-12-22")));

		cancel(anaToken, id).andExpect(status().isOk());

		mockMvc.perform(get(availability, room.getId()))
				.andExpect(jsonPath("$.occupiedDates", not(hasItem("2026-12-20"))))
				.andExpect(jsonPath("$.occupiedDates", not(hasItem("2026-12-21"))))
				.andExpect(jsonPath("$.occupiedDates", not(hasItem("2026-12-22"))));
		String betoBooking = "{\"roomId\":" + room.getId() + ",\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-23\",\"guests\":2,"
				+ "\"card\":{\"cardNumber\":\"5555555555554444\",\"expMonth\":6,\"expYear\":2029,\"cvv\":\"321\",\"holderName\":\"Beto\"}}";
		mockMvc.perform(as(login(beto), post("/api/reservation/book")).contentType(MediaType.APPLICATION_JSON).content(betoBooking))
				.andExpect(status().isCreated());
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mockMvc.perform(get("/api/reservation/my-reservations")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/reservation/cancel-reservation/1")).andExpect(status().isUnauthorized());
	}
}
