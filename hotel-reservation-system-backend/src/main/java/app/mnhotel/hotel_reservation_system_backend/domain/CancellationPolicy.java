package app.mnhotel.hotel_reservation_system_backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;

/**
 * Fases y política de cancelación de una reserva (API-CONTRACT §6.2). Lógica pura; la amplía HU-05.
 * Reembolso total si se cancela al menos 48 h antes del check-in (a la hora de check-in del hotel).
 */
public final class CancellationPolicy {

	public static final long REFUND_HOURS_BEFORE_CHECK_IN = 48;

	public enum Phase { UPCOMING, IN_STAY, COMPLETED, CANCELLED }

	public record Cancellation(boolean cancellable, boolean refundable, LocalDateTime refundDeadline,
			int refundAmountIfCancelledNow) {
	}

	private CancellationPolicy() {
	}

	public static Phase phase(ReservationStatus status, LocalDate checkIn, LocalDate checkOut, LocalDate today) {
		if (status == ReservationStatus.CANCELLED) {
			return Phase.CANCELLED;
		}
		if (today.isBefore(checkIn)) {
			return Phase.UPCOMING;
		}
		return today.isBefore(checkOut) ? Phase.IN_STAY : Phase.COMPLETED;
	}

	public static Cancellation evaluate(ReservationStatus status, LocalDate checkIn, int totalAmount, LocalDateTime now,
			LocalTime checkInTime) {
		LocalDateTime checkInAt = checkIn.atTime(checkInTime);
		LocalDateTime refundDeadline = checkInAt.minusHours(REFUND_HOURS_BEFORE_CHECK_IN);
		boolean cancellable = status == ReservationStatus.CONFIRMED && now.isBefore(checkInAt);
		// Límite inclusivo: cancelar exactamente 48 h antes todavía se reembolsa
		boolean refundable = cancellable && !now.isAfter(refundDeadline);
		return new Cancellation(cancellable, refundable, refundDeadline, refundable ? totalAmount : 0);
	}
}
