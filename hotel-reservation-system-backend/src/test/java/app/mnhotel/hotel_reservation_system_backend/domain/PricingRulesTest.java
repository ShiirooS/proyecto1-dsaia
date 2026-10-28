package app.mnhotel.hotel_reservation_system_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import app.mnhotel.hotel_reservation_system_backend.domain.PricingRules.InventoryAction;
import app.mnhotel.hotel_reservation_system_backend.domain.PricingRules.Recommendation;

/**
 * HU-03: umbrales de precio e inventario de ADR-004 §5.3 con R = 10 y P̄ = 100.00 (CA-03.5).
 */
class PricingRulesTest {

	@ParameterizedTest(name = "occ {0} -> {1} %, {2}, {3} habitaciones")
	@CsvSource({
			"0.3000, -10, REDUCE,    4",
			"0.3999, -10, REDUCE,    5",
			"0.4000,   0, KEEP,     10",
			"0.6999,   0, KEEP,     10",
			"0.7000,   8, KEEP,     10",
			"0.8500,   8, KEEP,     10",
			"0.8501,  15, INCREASE, 11",
			// Frontera exacta: 10 × 0.88 / 0.80 = 11.00 -> ceil no debe subir a 12
			"0.8800,  15, INCREASE, 11",
			"0.0000, -10, REDUCE,    1" })
	void umbralesDelAdr004(String occupancy, int pct, InventoryAction action, int rooms) {
		Recommendation recommendation = PricingRules.recommend(new BigDecimal(occupancy), 10, new BigDecimal("100.00"));

		assertThat(recommendation.priceAdjustmentPct()).isEqualTo(pct);
		assertThat(recommendation.inventoryAction()).isEqualTo(action);
		assertThat(recommendation.recommendedActiveRooms()).isEqualTo(rooms);
		assertThat(recommendation.suggestedPricePerNight()).isEqualTo(100 + pct);
	}

	@ParameterizedTest(name = "P̄ {0} con {1} % -> {2}")
	@CsvSource({
			"137.50,  8, 149",  // 148.50 -> HALF_UP
			"137.50, 15, 158",  // 158.125
			"137.50,-10, 124",  // 123.75
			"99.99,   0, 100" })
	void precioSugeridoEnteroConHalfUp(String averagePrice, int pct, int expected) {
		BigDecimal occupancy = switch (pct) {
			case 15 -> new BigDecimal("0.9000");
			case 8 -> new BigDecimal("0.7500");
			case 0 -> new BigDecimal("0.5000");
			default -> new BigDecimal("0.2000");
		};
		assertThat(PricingRules.recommend(occupancy, 10, new BigDecimal(averagePrice)).suggestedPricePerNight())
				.isEqualTo(expected);
	}
}
