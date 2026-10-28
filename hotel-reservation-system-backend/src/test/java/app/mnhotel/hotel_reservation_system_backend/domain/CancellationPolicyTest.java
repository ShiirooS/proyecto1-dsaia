package app.mnhotel.hotel_reservation_system_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import app.mnhotel.hotel_reservation_system_backend.domain.CancellationPolicy.Cancellation;
import app.mnhotel.hotel_reservation_system_backend.domain.CancellationPolicy.Phase;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;

/**
 * Fases y política de cancelación (API-CONTRACT §6.2): reembolso si faltan al menos 48 h para el check-in de las 15:00.
 */
class CancellationPolicyTest {

	private static final LocalTime CHECK_IN_TIME = LocalTime.of(15, 0);
	private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 20);
	private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 23);

	@Test
	void faseSegunLasFechas() {
		assertThat(CancellationPolicy.phase(ReservationStatus.CONFIRMED, CHECK_IN, CHECK_OUT, LocalDate.of(2026, 12, 19)))
				.isEqualTo(Phase.UPCOMING);
		assertThat(CancellationPolicy.phase(ReservationStatus.CONFIRMED, CHECK_IN, CHECK_OUT, CHECK_IN)).isEqualTo(Phase.IN_STAY);
		assertThat(CancellationPolicy.phase(ReservationStatus.CONFIRMED, CHECK_IN, CHECK_OUT, CHECK_OUT))
				.isEqualTo(Phase.COMPLETED);
		assertThat(CancellationPolicy.phase(ReservationStatus.CANCELLED, CHECK_IN, CHECK_OUT, LocalDate.of(2026, 1, 1)))
				.isEqualTo(Phase.CANCELLED);
	}

	@Test
	void reembolsoTotalHastaExactamente48HorasAntes() {
		LocalDateTime deadline = LocalDateTime.of(2026, 12, 18, 15, 0);

		Cancellation onTime = CancellationPolicy.evaluate(ReservationStatus.CONFIRMED, CHECK_IN, 450, deadline, CHECK_IN_TIME);
		assertThat(onTime.cancellable()).isTrue();
		assertThat(onTime.refundable()).isTrue();
		assertThat(onTime.refundDeadline()).isEqualTo(deadline);
		assertThat(onTime.refundAmountIfCancelledNow()).isEqualTo(450);

		Cancellation late = CancellationPolicy.evaluate(ReservationStatus.CONFIRMED, CHECK_IN, 450, deadline.plusMinutes(1),
				CHECK_IN_TIME);
		assertThat(late.cancellable()).isTrue();
		assertThat(late.refundable()).isFalse();
		assertThat(late.refundAmountIfCancelledNow()).isZero();
	}

	@Test
	void noSePuedeCancelarDesdeLaHoraDeCheckInNiDosVeces() {
		LocalDateTime checkInAt = CHECK_IN.atTime(CHECK_IN_TIME);
		assertThat(CancellationPolicy.evaluate(ReservationStatus.CONFIRMED, CHECK_IN, 450, checkInAt, CHECK_IN_TIME).cancellable())
				.isFalse();
		assertThat(CancellationPolicy.evaluate(ReservationStatus.CANCELLED, CHECK_IN, 450, checkInAt.minusDays(10), CHECK_IN_TIME)
				.cancellable()).isFalse();
	}

	@Test
	void unSegundoDespuesDelLimiteYaNoSeReembolsaPeroSiSeCancela() {
		// CA-05.3: 2026-12-18T15:00:01
		Cancellation late = CancellationPolicy.evaluate(ReservationStatus.CONFIRMED, CHECK_IN, 450,
				LocalDateTime.of(2026, 12, 18, 15, 0, 1), CHECK_IN_TIME);
		assertThat(late.cancellable()).isTrue();
		assertThat(late.refundable()).isFalse();
		assertThat(late.refundAmountIfCancelledNow()).isZero();

		// Un instante antes del check-in todavía se puede cancelar (sin reembolso)
		Cancellation lastMoment = CancellationPolicy.evaluate(ReservationStatus.CONFIRMED, CHECK_IN, 450,
				CHECK_IN.atTime(CHECK_IN_TIME).minusSeconds(1), CHECK_IN_TIME);
		assertThat(lastMoment.cancellable()).isTrue();
		assertThat(lastMoment.refundable()).isFalse();
	}
}
