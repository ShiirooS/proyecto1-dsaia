package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator.Stay;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Datos semilla del perfil dev (H2 en memoria). Nunca corre con el perfil postgres ni en tests.
 * Es determinista (Random con semilla fija) y las fechas son relativas a "hoy" según el Clock del hotel.
 * Credenciales de demostración documentadas en docs/user-stories/HU-04.md (no se escriben en el log).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	public static final String ADMIN_USERNAME = "admin@hotel.com";
	public static final String GUEST_USERNAME = "demo@hotel.com";

	private static final String IMG_SIMPLE = "https://img.freepik.com/free-photo/luxury-bedroom-suite-resort-high-rise-hotel-with-working-table_105762-1783.jpg";
	private static final String IMG_DOBLE = "https://img.freepik.com/foto-gratis/suite-dormitorio-moderno-clasico-lujo-hotel_105762-1787.jpg";
	private static final String IMG_SUITE = "https://img.freepik.com/free-photo/3d-rendering-beautiful-luxury-bedroom-suite-hotel-with-tv_105762-2063.jpg";

	private final UserRepository userRepository;
	private final HotelRepository hotelRepository;
	private final RoomRepository roomRepository;
	private final ReservationRepository reservationRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentMethodRepository paymentMethodRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}
		createUser(ADMIN_USERNAME, "Admin12345", "Administrador Hotel MN", RoleType.ADMIN);
		User demo = createUser(GUEST_USERNAME, "Demo12345", "Huésped Demo", RoleType.USER);
		List<User> syntheticGuests = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			syntheticGuests.add(createUser("cliente" + i + "@correo.com", "Cliente1234", "Cliente " + i, RoleType.USER));
		}

		List<Room> rooms = seedCatalog();
		List<Reservation> demoReservations = seedDemoReservations(rooms, demo);
		seedUpcomingReservations(rooms, syntheticGuests, demoReservations);
		seedSavedCard(demo);
		log.info("Datos semilla cargados (usuarios {} y {}, {} habitaciones)", ADMIN_USERNAME, GUEST_USERNAME, rooms.size());
	}

	private User createUser(String username, String password, String fullName, RoleType role) {
		return userRepository.save(User.builder()
				.username(username)
				.password(passwordEncoder.encode(password))
				.fullName(fullName)
				.role(role)
				.createdAt(LocalDateTime.now(clock))
				.build());
	}

	private List<Room> seedCatalog() {
		Hotel marAzul = hotelRepository.save(Hotel.builder().name("Hotel Mar Azul").location("Ciudad de Panamá")
				.province("Panamá").image(IMG_DOBLE).rating(4.5)
				.description("Frente a la bahía, a 10 minutos del Casco Antiguo.").build());
		Hotel montana = hotelRepository.save(Hotel.builder().name("Boquete Mountain Lodge").location("Boquete")
				.province("Chiriquí").image(IMG_SUITE).rating(4.7)
				.description("Cabañas entre cafetales con vista al volcán Barú.").build());

		List<Room> rooms = new ArrayList<>();
		rooms.add(room(marAzul, "101", "SIMPLE", 80, 1, IMG_SIMPLE, "Cama individual, escritorio y wifi."));
		rooms.add(room(marAzul, "201", "DOBLE", 120, 2, IMG_DOBLE, "Cama queen y balcón."));
		rooms.add(room(marAzul, "202", "DOBLE", 130, 3, IMG_DOBLE, "Cama queen + sofá cama, vista al mar."));
		rooms.add(room(marAzul, "301", "SUITE", 220, 4, IMG_SUITE, "Suite con sala, jacuzzi y vista panorámica."));
		rooms.add(room(montana, "A1", "SIMPLE", 90, 1, IMG_SIMPLE, "Cabaña individual junto al jardín."));
		rooms.add(room(montana, "A2", "DOBLE", 140, 2, IMG_DOBLE, "Cabaña doble con chimenea."));
		rooms.add(room(montana, "B1", "DOBLE", 150, 3, IMG_DOBLE, "Cabaña familiar con terraza."));
		rooms.add(room(montana, "B2", "SUITE", 200, 4, IMG_SUITE, "Suite con vista al volcán."));
		return roomRepository.saveAll(rooms);
	}

	private Room room(Hotel hotel, String number, String type, int price, int capacity, String image, String description) {
		return Room.builder().hotel(hotel).roomNumber(number).type(type).pricePerNight(price).capacity(capacity)
				.image(image).description(description).availbility(true)
				.amenities(List.of("Wifi", "Aire acondicionado", "TV")).build();
	}

	/**
	 * HU-02: ocupación salteada en los próximos 60 días para que el calendario muestre días ocupados.
	 */
	private void seedUpcomingReservations(List<Room> rooms, List<User> guests, List<Reservation> alreadySeeded) {
		Random random = new Random(42);
		LocalDate today = LocalDate.now(clock);
		List<Reservation> reservations = new ArrayList<>();
		for (Room room : rooms) {
			LocalDate cursor = today.plusDays(1 + random.nextInt(4));
			while (cursor.isBefore(today.plusDays(60))) {
				int nights = 1 + random.nextInt(4);
				// No pisar las reservas del usuario demo (dos CONFIRMED no pueden compartir noches, ADR-005)
				if (isFree(alreadySeeded, room, cursor, cursor.plusDays(nights))) {
					reservations.add(Reservation.builder()
							.room(room)
							.user(guests.get(random.nextInt(guests.size())))
							.startDate(cursor)
							.endDate(cursor.plusDays(nights))
							.guests(1)
							.totalAmount(nights * room.getPricePerNight())
							.status(ReservationStatus.CONFIRMED)
							.createdAt(LocalDateTime.now(clock))
							.build());
				}
				cursor = cursor.plusDays(nights + 2L + random.nextInt(6));
			}
		}
		reservationRepository.saveAll(reservations);
	}

	private static boolean isFree(List<Reservation> seeded, Room room, LocalDate checkIn, LocalDate checkOut) {
		Stay candidate = new Stay(checkIn, checkOut);
		return seeded.stream()
				.filter(r -> r.getRoom() == room && r.getStatus() == ReservationStatus.CONFIRMED)
				.noneMatch(r -> OccupancyCalculator.overlaps(candidate, new Stay(r.getStartDate(), r.getEndDate())));
	}

	/**
	 * HU-05: historial del usuario demo para probar "Mis reservas": una próxima reembolsable, una que empieza mañana
	 * (cancelable sin reembolso), una pasada y una cancelada con reembolso. Todas pagadas con su VISA ••••4242.
	 */
	private List<Reservation> seedDemoReservations(List<Room> rooms, User demo) {
		LocalDate today = LocalDate.now(clock);
		List<Reservation> seeded = new ArrayList<>();
		seeded.add(demoReservation(demo, rooms.get(1), today.plusDays(20), 3, ReservationStatus.CONFIRMED, today.minusDays(5)));
		seeded.add(demoReservation(demo, rooms.get(0), today.plusDays(1), 2, ReservationStatus.CONFIRMED, today.minusDays(10)));
		seeded.add(demoReservation(demo, rooms.get(2), today.minusDays(30), 3, ReservationStatus.CONFIRMED, today.minusDays(60)));
		seeded.add(demoReservation(demo, rooms.get(5), today.plusDays(10), 2, ReservationStatus.CANCELLED, today.minusDays(20)));
		return seeded;
	}

	private Reservation demoReservation(User demo, Room room, LocalDate checkIn, int nights, ReservationStatus status,
			LocalDate bookedOn) {
		int total = nights * room.getPricePerNight();
		LocalDateTime bookedAt = bookedOn.atTime(11, 30);
		boolean cancelled = status == ReservationStatus.CANCELLED;
		Reservation reservation = reservationRepository.save(Reservation.builder()
				.room(room).user(demo).startDate(checkIn).endDate(checkIn.plusDays(nights)).guests(1).totalAmount(total)
				.status(status).createdAt(bookedAt).cancelledAt(cancelled ? bookedAt.plusDays(3) : null).build());
		paymentRepository.save(Payment.builder().reservation(reservation).amount(total)
				.status(cancelled ? PaymentStatus.REFUNDED : PaymentStatus.COMPLETED).refundedAmount(cancelled ? total : 0)
				.cardBrand(CardBrand.VISA.name()).cardLast4("4242").transactionId("txn_demo" + reservation.getId())
				.paymentDate(bookedAt).build());
		return reservation;
	}

	/**
	 * HU-01: VISA ••••4242 predeterminada para el usuario demo (solo token simulado y datos no sensibles).
	 */
	private void seedSavedCard(User demo) {
		paymentMethodRepository.save(PaymentMethod.builder().user(demo).token("tok_demo0000000000000000visa")
				.brand(CardBrand.VISA).last4("4242").expMonth(12).expYear(LocalDate.now(clock).getYear() + 3)
				.holderName("HUESPED DEMO").isDefault(true).createdAt(LocalDateTime.now(clock)).build());
	}
}
