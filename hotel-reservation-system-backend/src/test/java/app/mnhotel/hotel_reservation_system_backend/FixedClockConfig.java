package app.mnhotel.hotel_reservation_system_backend;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Reloj fijo para los tests: "hoy" es 28/10/2026 a las 10:00 en la zona del hotel (ADR-006).
 */
@TestConfiguration
public class FixedClockConfig {

	public static final ZoneId ZONE = ZoneId.of("America/Panama");
	public static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 10, 0);

	@Bean
	@Primary
	public Clock fixedClock() {
		return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
	}
}
