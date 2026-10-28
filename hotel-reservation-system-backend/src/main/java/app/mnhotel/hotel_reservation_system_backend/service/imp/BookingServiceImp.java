package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.domain.CancellationPolicy;
import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator;
import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;
import app.mnhotel.hotel_reservation_system_backend.dto.MyReservationDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Payment;
import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.PaymentStatus;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.exception.ConflictException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.request.BookingRequest;
import app.mnhotel.hotel_reservation_system_backend.request.CardRequest;
import app.mnhotel.hotel_reservation_system_backend.service.BookingService;
import app.mnhotel.hotel_reservation_system_backend.service.CurrentUserService;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentGateway;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentMethodService;

@Service
public class BookingServiceImp implements BookingService {

	// Antelación máxima para reservar
	private static final int MAX_DAYS_IN_ADVANCE = 365;

	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private PaymentMethodService paymentMethodService;
	@Autowired
	private PaymentGateway paymentGateway;
	@Autowired
	private CurrentUserService currentUserService;
	@Autowired
	private Clock clock;

	@Value("${app.reservation.check-in-time}")
	private String checkInTime;
	@Value("${app.reservation.max-nights}")
	private int maxNights;

	@Override
	@Transactional
	public MyReservationDTO book(BookingRequest request) {
		// 1. Exactamente una fuente de pago
		boolean savedCard = request.getPaymentMethodId() != null;
		boolean newCard = request.getCard() != null;
		if (savedCard == newCard || (request.isSaveCard() && !newCard)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_SOURCE",
					"Indica una tarjeta guardada o una tarjeta nueva (no ambas)");
		}

		// 2. Recursos: la habitación debe estar activa y la tarjeta guardada ser del usuario del token
		User user = currentUserService.getCurrentUser();
		Room room = roomRepository.findByIdAndAvailbilityTrue(request.getRoomId())
				.orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND", "Habitación no encontrada"));
		PaymentMethod paymentMethod = savedCard ? paymentMethodService.findOwned(user, request.getPaymentMethodId()) : null;

		// 3. Reglas de estancia y de tarjeta (422)
		long nights = validateStay(request, room);
		ValidatedCard card = null;
		if (newCard) {
			CardRequest input = request.getCard();
			card = CardValidator.validate(input.getCardNumber(), input.getExpMonth(), input.getExpYear(), input.getCvv(),
					input.getHolderName(), YearMonth.now(clock));
		} else {
			CardValidator.checkNotExpired(paymentMethod.getExpMonth(), paymentMethod.getExpYear(), YearMonth.now(clock));
		}

		// 4. Con la fila de la habitación bloqueada, ninguna reserva confirmada puede compartir noches (ADR-005)
		roomRepository.findActiveByIdForUpdate(room.getId());
		if (reservationRepository.existsOverlapping(room.getId(), request.getCheckIn(), request.getCheckOut(), null)) {
			throw new ConflictException("ROOM_NOT_AVAILABLE", "Esas fechas acaban de reservarse");
		}

		// 5. Cobro simulado: si se rechaza (402) todavía no se ha persistido nada
		int totalAmount = Math.toIntExact(nights * room.getPricePerNight());
		String cardBrand = newCard ? card.brand().name() : paymentMethod.getBrand().name();
		String cardLast4 = newCard ? card.last4() : paymentMethod.getLast4();
		String transactionId = paymentGateway.charge(cardLast4, totalAmount);

		// 6. Reserva + pago en la misma transacción; la tarjeta nueva se guarda solo si se pidió
		LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
		Reservation reservation = reservationRepository.save(Reservation.builder()
				.room(room)
				.user(user)
				.startDate(request.getCheckIn())
				.endDate(request.getCheckOut())
				.guests(request.getGuests())
				.totalAmount(totalAmount)
				.status(ReservationStatus.CONFIRMED)
				.createdAt(now)
				.build());
		Payment payment = paymentRepository.save(Payment.builder()
				.reservation(reservation)
				.amount(totalAmount)
				.refundedAmount(0)
				.status(PaymentStatus.COMPLETED)
				.cardBrand(cardBrand)
				.cardLast4(cardLast4)
				.transactionId(transactionId)
				.paymentDate(now)
				.build());
		reservation.setPayments(List.of(payment));

		Long savedPaymentMethodId = null;
		if (newCard && request.isSaveCard()) {
			savedPaymentMethodId = paymentMethodService.saveOrReuse(user, card, false).getId();
		}
		MyReservationDTO dto = toMyReservation(reservation);
		dto.setSavedPaymentMethodId(savedPaymentMethodId);
		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public MyReservationDTO getMyReservation(Long id) {
		return toMyReservation(findOwnedReservation(id));
	}

	Reservation findOwnedReservation(Long id) {
		User user = currentUserService.getCurrentUser();
		return reservationRepository.findById(id)
				.filter(reservation -> reservation.getUser() != null && reservation.getUser().getId().equals(user.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("RESERVATION_NOT_FOUND", "Reserva no encontrada"));
	}

	private long validateStay(BookingRequest request, Room room) {
		LocalDate today = LocalDate.now(clock);
		LocalDate checkIn = request.getCheckIn();
		LocalDate checkOut = request.getCheckOut();
		if (checkIn.isBefore(today)) {
			throw unprocessable("CHECKIN_IN_PAST", "La fecha de llegada ya pasó");
		}
		if (!checkOut.isAfter(checkIn)) {
			throw unprocessable("INVALID_STAY_DATES", "La fecha de salida debe ser posterior a la de llegada");
		}
		long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
		if (nights > maxNights) {
			throw unprocessable("MAX_NIGHTS_EXCEEDED", "Máximo " + maxNights + " noches por reserva");
		}
		if (checkIn.isAfter(today.plusDays(MAX_DAYS_IN_ADVANCE))) {
			throw unprocessable("BOOKING_TOO_FAR", "Solo se puede reservar con hasta un año de antelación");
		}
		if (room.getCapacity() != null && request.getGuests() > room.getCapacity()) {
			throw unprocessable("GUESTS_EXCEED_CAPACITY", "La habitación admite como máximo " + room.getCapacity() + " huéspedes");
		}
		return nights;
	}

	private static BusinessException unprocessable(String code, String message) {
		return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
	}

	MyReservationDTO toMyReservation(Reservation reservation) {
		Room room = reservation.getRoom();
		Hotel hotel = room.getHotel();
		Payment payment = reservation.getPayments() == null ? null : reservation.getPayments().stream()
				.max(Comparator.comparing(Payment::getId))
				.orElse(null);
		int total = reservation.getTotalAmount() == null ? 0 : reservation.getTotalAmount();
		return MyReservationDTO.builder()
				.id(reservation.getId())
				.status(reservation.getStatus())
				.phase(CancellationPolicy.phase(reservation.getStatus(), reservation.getStartDate(), reservation.getEndDate(),
						LocalDate.now(clock)))
				.checkIn(reservation.getStartDate())
				.checkOut(reservation.getEndDate())
				.nights(ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate()))
				.guests(reservation.getGuests())
				.totalAmount(reservation.getTotalAmount())
				.createdAt(reservation.getCreatedAt())
				.cancelledAt(reservation.getCancelledAt())
				.room(new MyReservationDTO.RoomInfo(room.getId(), room.getRoomNumber(), room.getType(), room.getImage()))
				.hotel(hotel == null ? null : new MyReservationDTO.HotelInfo(hotel.getId(), hotel.getName(), hotel.getLocation()))
				.payment(payment == null ? null : new MyReservationDTO.PaymentInfo(payment.getId(), payment.getStatus().name(),
						payment.getAmount(), payment.getRefundedAmount(), payment.getCardBrand(), payment.getCardLast4(),
						payment.getTransactionId(), payment.getPaymentDate()))
				.cancellation(CancellationPolicy.evaluate(reservation.getStatus(), reservation.getStartDate(), total,
						LocalDateTime.now(clock), LocalTime.parse(checkInTime)))
				.build();
	}
}
