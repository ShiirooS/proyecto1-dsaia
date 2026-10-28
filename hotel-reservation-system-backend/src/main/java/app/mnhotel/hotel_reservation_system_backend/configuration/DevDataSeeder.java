package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Datos semilla para el perfil dev (H2 en memoria). Nunca se ejecuta con el perfil postgres ni en tests.
 * Es determinista (Random con semilla fija) y las fechas son relativas a "hoy" según el Clock del hotel.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "Admin1234";
	public static final String GUEST_USERNAME = "huesped";
	public static final String GUEST_PASSWORD = "Huesped123";

	private static final String IMG_SIMPLE = "https://img.freepik.com/free-photo/luxury-bedroom-suite-resort-high-rise-hotel-with-working-table_105762-1783.jpg";
	private static final String IMG_DOBLE = "https://img.freepik.com/foto-gratis/suite-dormitorio-moderno-clasico-lujo-hotel_105762-1787.jpg";
	private static final String IMG_SUITE = "https://img.freepik.com/free-photo/3d-rendering-beautiful-luxury-bedroom-suite-hotel-with-tv_105762-2063.jpg";

	private final UserRepository userRepository;
	private final HotelRepository hotelRepository;
	private final RoomRepository roomRepository;
	private final ReservationRepository reservationRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser(ADMIN_USERNAME, "admin@hotelmn.com", ADMIN_PASSWORD, "Administrador Hotel MN", Role.ADMIN);
		createUser(GUEST_USERNAME, "huesped@correo.com", GUEST_PASSWORD, "Huésped Recurrente", Role.USER);
		List<User> syntheticGuests = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			syntheticGuests.add(createUser("cliente" + i, "cliente" + i + "@correo.com", "Cliente1234",
					"Cliente Sintético " + i, Role.USER));
		}

		List<Room> rooms = seedCatalog();
		seedUpcomingReservations(rooms, syntheticGuests);

		log.info("Datos semilla cargados. Admin: {} / {} - Huésped: {} / {}",
				ADMIN_USERNAME, ADMIN_PASSWORD, GUEST_USERNAME, GUEST_PASSWORD);
	}

	private User createUser(String username, String email, String password, String fullName, Role role) {
		return userRepository.save(User.builder()
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(password))
				.fullName(fullName)
				.phone("+507 6000-0000")
				.role(role)
				.createdAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS))
				.build());
	}

	private List<Room> seedCatalog() {
		Hotel marAzul = hotelRepository.save(Hotel.builder().name("Hotel Mar Azul").location("Ciudad de Panamá")
				.description("Frente a la bahía, a 10 minutos del Casco Antiguo.").rating(4.5).build());
		Hotel montana = hotelRepository.save(Hotel.builder().name("Boquete Mountain Lodge").location("Boquete, Chiriquí")
				.description("Cabañas entre cafetales con vista al volcán Barú.").rating(4.7).build());

		List<Room> rooms = new ArrayList<>();
		rooms.add(room(marAzul, "101", "SIMPLE", "80.00", 1, IMG_SIMPLE, "Cama individual, escritorio y wifi."));
		rooms.add(room(marAzul, "102", "SIMPLE", "85.00", 1, IMG_SIMPLE, "Cama individual con vista a la ciudad."));
		rooms.add(room(marAzul, "201", "DOBLE", "120.00", 2, IMG_DOBLE, "Cama queen y balcón."));
		rooms.add(room(marAzul, "202", "DOBLE", "130.00", 3, IMG_DOBLE, "Cama queen + sofá cama, vista al mar."));
		rooms.add(room(marAzul, "301", "SUITE", "220.00", 4, IMG_SUITE, "Suite con sala, jacuzzi y vista panorámica."));
		rooms.add(room(montana, "A1", "SIMPLE", "90.00", 1, IMG_SIMPLE, "Cabaña individual junto al jardín."));
		rooms.add(room(montana, "A2", "DOBLE", "140.00", 2, IMG_DOBLE, "Cabaña doble con chimenea."));
		rooms.add(room(montana, "B1", "DOBLE", "150.00", 3, IMG_DOBLE, "Cabaña familiar con terraza."));
		rooms.add(room(montana, "B2", "SUITE", "200.00", 4, IMG_SUITE, "Suite con vista al volcán."));
		rooms.add(room(montana, "C1", "SUITE", "210.00", 4, IMG_SUITE, "Suite premium con tina al aire libre."));
		return roomRepository.saveAll(rooms);
	}

	private Room room(Hotel hotel, String number, String type, String price, int capacity, String imageUrl,
			String description) {
		return Room.builder().hotel(hotel).roomNumber(number).type(type).pricePerNight(new BigDecimal(price))
				.capacity(capacity).imageUrl(imageUrl).description(description).active(true).build();
	}

	/**
	 * Ocupación salteada en los próximos 60 días para que el calendario muestre días ocupados (HU-02).
	 */
	private void seedUpcomingReservations(List<Room> rooms, List<User> guests) {
		Random random = new Random(42);
		LocalDate today = LocalDate.now(clock);
		List<Reservation> reservations = new ArrayList<>();
		for (Room room : rooms) {
			LocalDate cursor = today.plusDays(1 + random.nextInt(4));
			while (cursor.isBefore(today.plusDays(60))) {
				int nights = 1 + random.nextInt(4);
				reservations.add(Reservation.builder()
						.room(room)
						.user(guests.get(random.nextInt(guests.size())))
						.checkIn(cursor)
						.checkOut(cursor.plusDays(nights))
						.guests(1)
						.totalAmount(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
						.status(ReservationStatus.CONFIRMED)
						.createdAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS))
						.build());
				cursor = cursor.plusDays(nights + 2L + random.nextInt(6));
			}
		}
		reservationRepository.saveAll(reservations);
	}
}
