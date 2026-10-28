package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj inyectable con la zona horaria del hotel (ADR-006). En los tests se reemplaza por un Clock fijo.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock(@Value("${app.time-zone}") String timeZone) {
		return Clock.system(ZoneId.of(timeZone));
	}
}
