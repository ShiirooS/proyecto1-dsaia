package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.entity.Role;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;

/**
 * HU-02: catálogo de habitaciones y disponibilidad para el calendario dinámico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class RoomControllerTest {

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
		hotel = hotelRepository.save(Hotel.builder().name("Hotel Calendario").location("Panamá")
				.description("Hotel de pruebas").rating(4.0).build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("203").type("DOBLE")
				.pricePerNight(new BigDecimal("150.00")).capacity(2).active(true).description("Vista al mar").build());
		inactiveRoom = roomRepository.save(Room.builder().hotel(hotel).roomNumber("999").type("SIMPLE")
				.pricePerNight(new BigDecimal("50.00")).capacity(1).active(false).build());
		guest = userRepository.save(User.builder().username("cal-guest").email("cal-guest@correo.com")
				.password("x").fullName("Huésped").role(Role.USER).build());
	}

	@AfterEach
	void tearDown() {
		reservationRepository.deleteAll(reservationRepository.findAll().stream()
				.filter(reservation -> reservation.getRoom().getHotel().getId().equals(hotel.getId())).toList());
		roomRepository.deleteAll(java.util.List.of(room, inactiveRoom));
		hotelRepository.delete(hotel);
		userRepository.delete(guest);
	}

	private void reserve(LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
		reservationRepository.save(Reservation.builder().room(room).user(guest).checkIn(checkIn).checkOut(checkOut)
				.guests(2).totalAmount(new BigDecimal("450.00")).status(status)
				.createdAt(LocalDateTime.of(2026, 10, 1, 9, 0)).build());
	}

	@Test
	void listaSoloHabitacionesActivasConSuHotel() throws Exception {
		mockMvc.perform(get("/api/rooms").param("hotelId", hotel.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].roomNumber", contains("203")))
				.andExpect(jsonPath("$[0].pricePerNight").value(150.00))
				.andExpect(jsonPath("$[0].capacity").value(2))
				.andExpect(jsonPath("$[0].hotel.name").value("Hotel Calendario"));

		mockMvc.perform(get("/api/rooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].roomNumber", not(hasItem("999"))));
	}

	@Test
	void detalleDeHabitacionYHabitacionInexistenteOInactiva() throws Exception {
		mockMvc.perform(get("/api/rooms/{id}", room.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Vista al mar"))
				.andExpect(jsonPath("$.hotel.rating").value(4.0));

		mockMvc.perform(get("/api/rooms/{id}", inactiveRoom.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
	}

	@Test
	void disponibilidadDevuelveNochesOcupadasSinContarCanceladas() throws Exception {
		reserve(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13), ReservationStatus.CONFIRMED);
		reserve(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), ReservationStatus.CANCELLED);

		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId())
				.param("from", "2026-11-01").param("to", "2026-11-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomId").value(room.getId()))
				.andExpect(jsonPath("$.today").value("2026-10-28"))
				.andExpect(jsonPath("$.checkInTime").value("15:00"))
				.andExpect(jsonPath("$.maxNights").value(30))
				.andExpect(jsonPath("$.occupiedDates", contains("2026-11-10", "2026-11-11", "2026-11-12")));
	}

	@Test
	void disponibilidadEsPublicaSinToken() throws Exception {
		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId())
				.param("from", "2026-11-01").param("to", "2026-11-02"))
				.andExpect(status().isOk());
	}

	@Test
	void disponibilidadValidaParametros() throws Exception {
		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId())
				.param("from", "2026-11-30").param("to", "2026-11-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId())
				.param("from", "2026-01-01").param("to", "2027-01-02"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId()).param("from", "2026-11-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));

		mockMvc.perform(get("/api/rooms/{id}/availability", room.getId())
				.param("from", "2026-13-01").param("to", "2026-12-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

		mockMvc.perform(get("/api/rooms/{id}/availability", 987654)
				.param("from", "2026-11-01").param("to", "2026-11-30"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
	}
}
