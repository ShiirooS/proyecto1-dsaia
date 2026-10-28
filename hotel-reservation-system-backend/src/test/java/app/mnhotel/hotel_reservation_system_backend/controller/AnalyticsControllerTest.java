package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-03: GET /api/analytics/demand-forecast de punta a punta (CA-03.1, 03.2, 03.6, 03.7, 03.8).
 * Hoy es 28/10/2026 (FixedClockConfig): la ventana es 2024-10..2026-09 y el pronóstico empieza en 2026-10.
 *
 * Datos conocidos: el hotel tiene 2 habitaciones activas (100 y 140 por noche) y la habitación A está ocupada todas
 * las noches de la ventana mientras B está libre, así que la ocupación es exactamente 0.5 todos los meses
 * (serie constante de ADR-004 §5.2). Las reservas de una habitación inactiva y las CANCELLED no deben contar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class AnalyticsControllerTest {

	private static final String PASSWORD = "Clave1234";
	private static final String FORECAST = "/api/analytics/demand-forecast";
	private static final YearMonth HISTORY_FROM = YearMonth.of(2024, 10);

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

	private User admin;
	private User guest;
	private Hotel hotel;
	private Hotel emptyHotel;
	private Hotel closedHotel;
	private final List<Room> rooms = new ArrayList<>();

	@BeforeEach
	void setUp() {
		admin = createUser("analytics-admin@correo.com", RoleType.ADMIN);
		guest = createUser("analytics-guest@correo.com", RoleType.USER);
		hotel = hotelRepository.save(Hotel.builder().name("Hotel Previsión").location("Panamá").build());
		emptyHotel = hotelRepository.save(Hotel.builder().name("Hotel Sin Historia").location("Colón").build());
		closedHotel = hotelRepository.save(Hotel.builder().name("Hotel Cerrado").location("Coclé").build());

		Room roomA = room(hotel, "PV-A", 100, true);
		Room roomB = room(hotel, "PV-B", 140, true);
		Room inactive = room(hotel, "PV-X", 999, false);
		room(emptyHotel, "SH-1", 80, true);
		room(closedHotel, "HC-1", 80, false);

		// A ocupada las 24 × (días del mes) noches de la ventana: una reserva por mes, sin huecos
		for (YearMonth month = HISTORY_FROM; month.isBefore(YearMonth.of(2026, 10)); month = month.plusMonths(1)) {
			reserve(roomA, month.atDay(1), month.plusMonths(1).atDay(1), ReservationStatus.CONFIRMED);
		}
		// Ruido que no debe contar: reserva cancelada en B y reservas de la habitación inactiva
		reserve(roomB, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 20), ReservationStatus.CANCELLED);
		reserve(inactive, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 8, 1), ReservationStatus.CONFIRMED);
		// "Ya reservado" a futuro: noches del 29, 30 y 31 de octubre y del 1 de noviembre
		reserve(roomB, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 2), ReservationStatus.CONFIRMED);
	}

	@AfterEach
	void tearDown() {
		List<Long> roomIds = rooms.stream().map(Room::getId).toList();
		reservationRepository.deleteAll(reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && roomIds.contains(r.getRoom().getId())).toList());
		roomRepository.deleteAll(rooms);
		rooms.clear();
		hotelRepository.deleteAll(List.of(hotel, emptyHotel, closedHotel));
		userRepository.deleteAll(List.of(admin, guest));
	}

	private User createUser(String username, RoleType role) {
		return userRepository.save(User.builder().username(username).password(passwordEncoder.encode(PASSWORD))
				.fullName(username).role(role).createdAt(LocalDateTime.now()).build());
	}

	private Room room(Hotel owner, String number, int price, boolean active) {
		Room saved = roomRepository.save(Room.builder().hotel(owner).roomNumber(number).type("DOBLE").pricePerNight(price)
				.capacity(2).availbility(active).build());
		rooms.add(saved);
		return saved;
	}

	private void reserve(Room room, LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
		reservationRepository.save(Reservation.builder().room(room).user(guest).startDate(checkIn).endDate(checkOut)
				.status(status).createdAt(LocalDateTime.of(2024, 1, 1, 10, 0)).build());
	}

	private String login(User user) throws Exception {
		String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", user.getUsername(), "password", PASSWORD))))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	private ResultActions forecast(String token, String query) throws Exception {
		return mockMvc.perform(get(FORECAST + query).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	@Test
	void previsionDePuntaAPuntaConElHistoricoInsertado() throws Exception {
		forecast(login(admin), "?hotelId=" + hotel.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generatedAt").value("2026-10-28T10:00:00"))
				.andExpect(jsonPath("$.hotelId").value(hotel.getId()))
				.andExpect(jsonPath("$.horizon").value(6))
				.andExpect(jsonPath("$.activeRooms").value(2))
				.andExpect(jsonPath("$.averagePricePerNight").value(120.00))
				.andExpect(jsonPath("$.historyFrom").value("2024-10"))
				.andExpect(jsonPath("$.historyTo").value("2026-09"))
				.andExpect(jsonPath("$.model.type").value("LINEAR_TREND_MONTHLY_SEASONALITY"))
				.andExpect(jsonPath("$.model.slope").value(0.0))
				.andExpect(jsonPath("$.model.intercept").value(0.5))
				.andExpect(jsonPath("$.model.seasonalIndices", hasSize(12)))
				.andExpect(jsonPath("$.model.seasonalIndices[0].month").value(1))
				.andExpect(jsonPath("$.model.seasonalIndices[*].index", everyItem(is(0.0))))
				// 24 meses: la inactiva y la cancelada no suman (ocupación 0.5 también en marzo y julio de 2025)
				.andExpect(jsonPath("$.history", hasSize(24)))
				.andExpect(jsonPath("$.history[0].month").value("2024-10"))
				.andExpect(jsonPath("$.history[0].occupiedRoomNights").value(31))
				.andExpect(jsonPath("$.history[0].availableRoomNights").value(62))
				.andExpect(jsonPath("$.history[*].occupancyRate", everyItem(is(0.5))))
				.andExpect(jsonPath("$.history[16].month").value("2026-02"))
				.andExpect(jsonPath("$.history[16].availableRoomNights").value(56))
				.andExpect(jsonPath("$.history[23].trendRate").value(0.5))
				// Pronóstico desde el mes actual
				.andExpect(jsonPath("$.forecast", hasSize(6)))
				.andExpect(jsonPath("$.forecast[0].month").value("2026-10"))
				.andExpect(jsonPath("$.forecast[0].forecastOccupancyRate").value(0.5))
				.andExpect(jsonPath("$.forecast[0].forecastRoomNights").value(31))
				.andExpect(jsonPath("$.forecast[0].availableRoomNights").value(62))
				.andExpect(jsonPath("$.forecast[0].bookedRoomNights").value(3))
				.andExpect(jsonPath("$.forecast[0].bookedOccupancyRate").value(0.0484))
				.andExpect(jsonPath("$.forecast[1].month").value("2026-11"))
				.andExpect(jsonPath("$.forecast[1].bookedRoomNights").value(1))
				.andExpect(jsonPath("$.forecast[1].bookedOccupancyRate").value(0.0167))
				.andExpect(jsonPath("$.forecast[0].priceAdjustmentPct").value(0))
				.andExpect(jsonPath("$.forecast[0].suggestedPricePerNight").value(120))
				.andExpect(jsonPath("$.forecast[0].inventoryAction").value("KEEP"))
				.andExpect(jsonPath("$.forecast[0].recommendedActiveRooms").value(2));
	}

	@Test
	void lasTasasSeSerializanConCuatroDecimales() throws Exception {
		String json = forecast(login(admin), "?hotelId=" + hotel.getId()).andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(json)
				.contains("\"slope\":0.0000", "\"intercept\":0.5000", "\"forecastOccupancyRate\":0.5000",
						"\"averagePricePerNight\":120.00");
	}

	@Test
	void horizonteDoceDevuelveDoceMeses() throws Exception {
		forecast(login(admin), "?horizon=12&hotelId=" + hotel.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horizon").value(12))
				.andExpect(jsonPath("$.forecast", hasSize(12)))
				.andExpect(jsonPath("$.forecast[11].month").value("2027-09"));
	}

	@Test
	void parametrosInvalidos() throws Exception {
		String token = login(admin);
		for (String query : List.of("?horizon=0", "?horizon=13", "?horizon=abc", "?hotelId=abc")) {
			forecast(token, query)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
		}
		forecast(token, "?hotelId=999999")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
	}

	@Test
	void sinHistoricoOSinHabitacionesActivasResponde422() throws Exception {
		String token = login(admin);
		forecast(token, "?hotelId=" + emptyHotel.getId())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_HISTORY"));
		forecast(token, "?hotelId=" + closedHotel.getId())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_HISTORY"));
	}

	@Test
	void soloElAdministradorConsultaLaPrevision() throws Exception {
		forecast(login(guest), "")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get(FORECAST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
