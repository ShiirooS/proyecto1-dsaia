package app.mnhotel.hotel_reservation_system_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator.Stay;

/**
 * HU-02: reglas de noches ocupadas (ejemplos de API-CONTRACT §3.3).
 */
class OccupancyCalculatorTest {

	private static LocalDate nov(int day) {
		return LocalDate.of(2026, 11, day);
	}

	// Reserva existente del contrato: check-in 10/11, check-out 13/11
	private final Stay existing = new Stay(nov(10), nov(13));

	@Test
	void elDiaDeCheckOutNoQuedaOcupado() {
		assertThat(OccupancyCalculator.occupiedNights(List.of(existing), nov(1), nov(30)))
				.containsExactly(nov(10), nov(11), nov(12));
	}

	@Test
	void recortaAlRangoConsultadoYOrdenaSinDuplicados() {
		Stay other = new Stay(nov(11), nov(15));
		assertThat(OccupancyCalculator.occupiedNights(List.of(other, existing), nov(11), nov(13)))
				.containsExactly(nov(11), nov(12), nov(13));
	}

	@Test
	void sinReservasNoHayNochesOcupadas() {
		assertThat(OccupancyCalculator.occupiedNights(List.of(), nov(1), nov(30))).isEmpty();
	}

	@Test
	void solapamientoSegunTablaDelContrato() {
		// Válidos: el check-out cae en un día ocupado o el check-in coincide con la salida de la otra
		assertThat(OccupancyCalculator.overlaps(new Stay(nov(8), nov(10)), existing)).isFalse();
		assertThat(OccupancyCalculator.overlaps(new Stay(nov(13), nov(15)), existing)).isFalse();
		// Inválidos: comparten al menos una noche
		assertThat(OccupancyCalculator.overlaps(new Stay(nov(9), nov(11)), existing)).isTrue();
		assertThat(OccupancyCalculator.overlaps(new Stay(nov(10), nov(11)), existing)).isTrue();
		assertThat(OccupancyCalculator.overlaps(new Stay(nov(1), nov(30)), existing)).isTrue();
	}

	@Test
	void nochesPorMesSegunTablaDelAdr004() {
		// §5.4: una estancia que cruza meses reparte sus noches y la no válida no suma
		Map<YearMonth, Integer> nights = OccupancyCalculator.nightsPerMonth(List.of(
				new Stay(LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 2)),
				new Stay(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10))),
				YearMonth.of(2026, 1), YearMonth.of(2026, 3));
		assertThat(nights).containsExactly(
				Map.entry(YearMonth.of(2026, 1), 2),
				Map.entry(YearMonth.of(2026, 2), 1),
				Map.entry(YearMonth.of(2026, 3), 0));
	}

	@Test
	void nochesPorMesEnAnioBisiestoYFueraDeLaVentana() {
		Map<YearMonth, Integer> nights = OccupancyCalculator.nightsPerMonth(List.of(
				new Stay(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1)),
				// Empieza antes de la ventana y termina después: solo cuentan las noches de febrero
				new Stay(LocalDate.of(2023, 12, 20), LocalDate.of(2024, 4, 5))),
				YearMonth.of(2024, 2), YearMonth.of(2024, 2));
		assertThat(nights).containsExactly(Map.entry(YearMonth.of(2024, 2), 2 + 29));
	}
}
