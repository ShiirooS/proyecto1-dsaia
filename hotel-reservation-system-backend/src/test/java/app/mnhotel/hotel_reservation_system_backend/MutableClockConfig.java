package app.mnhotel.hotel_reservation_system_backend;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Reloj que el test puede mover (HU-05: probar la cancelación justo en el límite de 48 h o tras el check-in).
 * Arranca en el mismo "hoy" que FixedClockConfig: 28/10/2026 a las 10:00 en la zona del hotel.
 */
@TestConfiguration
public class MutableClockConfig {

	@Bean
	@Primary
	public MutableClock mutableClock() {
		return new MutableClock(FixedClockConfig.NOW);
	}

	public static class MutableClock extends Clock {

		private volatile Instant instant;

		public MutableClock(LocalDateTime start) {
			setTo(start);
		}

		/** Mueve el reloj a esa fecha y hora en la zona del hotel. */
		public void setTo(LocalDateTime dateTime) {
			this.instant = dateTime.atZone(FixedClockConfig.ZONE).toInstant();
		}

		@Override
		public ZoneId getZone() {
			return FixedClockConfig.ZONE;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
