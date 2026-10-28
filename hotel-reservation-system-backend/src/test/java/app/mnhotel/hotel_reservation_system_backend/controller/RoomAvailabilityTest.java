package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
 * HU-02: disponibilidad derivada de las reservas para el calendario dinámico (CA-02.1, 02.9 a 02.13).
 * "Hoy" es 28/10/2026 (FixedClockConfig) y hay una reserva CONFIRMED del 10 al 13 de noviembre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class RoomAvailabilityTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private HotelRepository hotelRepository;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private UserRepository userRepository;

	private Hotel hotel;
	private Room room;
	private Room inactiveRoom;
	private User guest;

	@BeforeEach
	void setUp() {
		hotel = hotelRepository.save(Hotel.builder().name("Hotel Calendario").location("Panamá").build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("CAL-3").type("DOBLE").pricePerNight(150)
				.capacity(2).availbility(true).build());
		inactiveRoom = roomRepository.save(Room.builder().hotel(hotel).roomNumber("CAL-9").type("SIMPLE").pricePerNight(50)
				.capacity(1).availbility(false).build());
		guest = userRepository.save(User.builder().username("cal-guest@correo.com").password("x").fullName("Huésped")
				.role(RoleType.USER).createdAt(LocalDateTime.now()).build());
		reserve(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), ReservationStatus.CONFIRMED);
	}

	@AfterEach
	void tearDown() {
		reservationRepository.deleteAll(reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getHotel().getId().equals(hotel.getId())).toList());
		roomRepository.deleteAll(List.of(room, inactiveRoom));
		hotelRepository.delete(hotel);
		userRepository.delete(guest);
	}

	private void reserve(LocalDate start, LocalDate end, ReservationStatus status) {
		reservationRepository.save(Reservation.builder().room(room).user(guest).startDate(start).endDate(end)
				.status(status).createdAt(LocalDateTime.of(2026, 10, 1, 9, 0)).build());
	}

	private String availability(Long roomId, String from, String to) {
		return "/api/room/get-availability/" + roomId + "?from=" + from + "&to=" + to;
	}

	@Test
	void listadoPublicoFiltraInactivasYElDeAdminLasMantiene() throws Exception {
		mockMvc.perform(get("/api/room/get-all").param("activeOnly", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].roomNumber", hasItem("CAL-3")))
				.andExpect(jsonPath("$[*].roomNumber", not(hasItem("CAL-9"))));

		// Sin el parámetro la respuesta no cambia respecto a developer (la usa RoomTable del panel)
		mockMvc.perform(get("/api/room/get-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].roomNumber", hasItem("CAL-9")));
	}

	@Test
	void devuelveNochesOcupadasSinElDiaDeSalidaNiLasCanceladas() throws Exception {
		reserve(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), ReservationStatus.CANCELLED);

		mockMvc.perform(get(availability(room.getId(), "2026-11-01", "2026-11-30")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomId").value(room.getId()))
				.andExpect(jsonPath("$.today").value("2026-10-28"))
				.andExpect(jsonPath("$.checkInTime").value("15:00"))
				.andExpect(jsonPath("$.maxNights").value(30))
				.andExpect(jsonPath("$.occupiedDates", contains("2026-11-10", "2026-11-11", "2026-11-12")));
	}

	@Test
	void recortaLasNochesAlRangoConsultado() throws Exception {
		mockMvc.perform(get(availability(room.getId(), "2026-11-11", "2026-11-11")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.occupiedDates", contains("2026-11-11")));
	}

	@Test
	void validaElRangoYLosParametros() throws Exception {
		mockMvc.perform(get(availability(room.getId(), "2026-11-30", "2026-11-01")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		mockMvc.perform(get(availability(room.getId(), "2026-01-01", "2027-01-02")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		mockMvc.perform(get("/api/room/get-availability/" + room.getId() + "?from=2026-11-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
		mockMvc.perform(get(availability(room.getId(), "2026-13-01", "2026-12-01")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
	}

	@Test
	void habitacionInactivaOInexistenteResponde404() throws Exception {
		mockMvc.perform(get(availability(inactiveRoom.getId(), "2026-11-01", "2026-11-30")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
		mockMvc.perform(get(availability(987654L, "2026-11-01", "2026-11-30")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
	}

	@Test
	@WithMockUser(authorities = "ADMIN")
	void reservaManualDelAdminQueSolapaResponde409() throws Exception {
		String overlapping = """
				{"roomId":%d,"userId":%d,"startDate":"2026-11-11","endDate":"2026-11-12"}
				""".formatted(room.getId(), guest.getId());
		mockMvc.perform(post("/api/reservation/registration-reservation").contentType(MediaType.APPLICATION_JSON)
				.content(overlapping))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));

		// Entrar el mismo día de salida de la otra reserva es válido
		String sameDayAsCheckout = """
				{"roomId":%d,"userId":%d,"startDate":"2026-11-13","endDate":"2026-11-15"}
				""".formatted(room.getId(), guest.getId());
		mockMvc.perform(post("/api/reservation/registration-reservation").contentType(MediaType.APPLICATION_JSON)
				.content(sameDayAsCheckout))
				.andExpect(status().isOk());

		String invertedDates = """
				{"roomId":%d,"userId":%d,"startDate":"2026-11-20","endDate":"2026-11-18"}
				""".formatted(room.getId(), guest.getId());
		mockMvc.perform(post("/api/reservation/registration-reservation").contentType(MediaType.APPLICATION_JSON)
				.content(invertedDates))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("INVALID_STAY_DATES"));

		// Las fechas se devuelven como YYYY-MM-DD
		mockMvc.perform(get("/api/reservation/get-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].startDate", hasItem("2026-11-13")));
	}

	@Test
	@WithMockUser(authorities = "ADMIN")
	void editarUnaReservaParaQueSolapeTambienResponde409() throws Exception {
		reserve(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), ReservationStatus.CONFIRMED);
		Reservation later = reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId())
						&& r.getStartDate().equals(LocalDate.of(2026, 11, 20)))
				.findFirst().orElseThrow();

		mockMvc.perform(put("/api/reservation/update-reservation/{id}", later.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"startDate\":\"2026-11-12\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));

		// Moverla sin solapar (ignorándose a sí misma) es válido
		mockMvc.perform(put("/api/reservation/update-reservation/{id}", later.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"startDate\":\"2026-11-21\"}"))
				.andExpect(status().isOk());
	}
}
