package app.mnhotel.hotel_reservation_system_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.mnhotel.hotel_reservation_system_backend.domain.DemandForecastCalculator.Forecast;
import app.mnhotel.hotel_reservation_system_backend.domain.DemandForecastCalculator.ForecastPoint;
import app.mnhotel.hotel_reservation_system_backend.domain.DemandForecastCalculator.SeasonalIndex;
import app.mnhotel.hotel_reservation_system_backend.domain.PricingRules.InventoryAction;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;

/**
 * HU-03: vectores normativos de ADR-004 §5.1 y §5.2 (CA-03.4). Las salidas redondeadas se comparan exactas.
 */
class DemandForecastCalculatorTest {

	private static final YearMonth JAN_2027 = YearMonth.of(2027, 1);
	private static final BigDecimal PRICE = new BigDecimal("100.00");

	/** Ventana 2025-01..2026-12: el año 2 es el año 1 + 0.12. */
	private static final double[] NORMATIVE = {
			0.45, 0.45, 0.55, 0.55, 0.65, 0.75, 0.85, 0.85, 0.65, 0.55, 0.45, 0.45,
			0.57, 0.57, 0.67, 0.67, 0.77, 0.87, 0.97, 0.97, 0.77, 0.67, 0.57, 0.57 };

	private static BigDecimal rate(String value) {
		return new BigDecimal(value);
	}

	@Test
	void casoNormativoReproduceLaTablaDelAdr004() {
		Forecast forecast = DemandForecastCalculator.forecast(NORMATIVE, 10, PRICE, JAN_2027, 12);

		assertThat(forecast.slope()).isEqualTo(rate("0.0100"));
		assertThat(forecast.intercept()).isEqualTo(rate("0.5350"));
		assertThat(forecast.trendRates().get(0)).isEqualTo(rate("0.5450"));
		assertThat(forecast.trendRates().get(23)).isEqualTo(rate("0.7750"));
		assertThat(forecast.seasonalIndices()).extracting(SeasonalIndex::index).containsExactly(
				rate("-0.0950"), rate("-0.1050"), rate("-0.0150"), rate("-0.0250"), rate("0.0650"), rate("0.1550"),
				rate("0.2450"), rate("0.2350"), rate("0.0250"), rate("-0.0850"), rate("-0.1950"), rate("-0.2050"));
		assertThat(forecast.seasonalIndices()).extracting(SeasonalIndex::month)
				.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

		// mes, occ, noches, % precio, precio, acción, habitaciones
		Object[][] expected = {
				{ "2027-01", "0.6900", 214, 0, 100, InventoryAction.KEEP, 10 },
				{ "2027-02", "0.6900", 193, 0, 100, InventoryAction.KEEP, 10 },
				{ "2027-03", "0.7900", 245, 8, 108, InventoryAction.KEEP, 10 },
				{ "2027-04", "0.7900", 237, 8, 108, InventoryAction.KEEP, 10 },
				{ "2027-05", "0.8900", 276, 15, 115, InventoryAction.INCREASE, 12 },
				{ "2027-06", "0.9900", 297, 15, 115, InventoryAction.INCREASE, 13 },
				{ "2027-07", "1.0000", 310, 15, 115, InventoryAction.INCREASE, 13 },
				{ "2027-08", "1.0000", 310, 15, 115, InventoryAction.INCREASE, 13 },
				{ "2027-09", "0.8900", 267, 15, 115, InventoryAction.INCREASE, 12 },
				{ "2027-10", "0.7900", 245, 8, 108, InventoryAction.KEEP, 10 },
				{ "2027-11", "0.6900", 207, 0, 100, InventoryAction.KEEP, 10 },
				{ "2027-12", "0.6900", 214, 0, 100, InventoryAction.KEEP, 10 } };
		List<ForecastPoint> points = forecast.points();
		assertThat(points).hasSize(12);
		for (int i = 0; i < expected.length; i++) {
			ForecastPoint point = points.get(i);
			Object[] row = expected[i];
			assertThat(point.month()).as("mes %d", i + 1).isEqualTo(YearMonth.parse((String) row[0]));
			assertThat(point.occupancyRate()).as("occ %s", row[0]).isEqualTo(rate((String) row[1]));
			assertThat(point.forecastRoomNights()).as("noches %s", row[0]).isEqualTo(row[2]);
			assertThat(point.recommendation().priceAdjustmentPct()).as("pct %s", row[0]).isEqualTo(row[3]);
			assertThat(point.recommendation().suggestedPricePerNight()).as("precio %s", row[0]).isEqualTo(row[4]);
			assertThat(point.recommendation().inventoryAction()).as("acción %s", row[0]).isEqualTo(row[5]);
			assertThat(point.recommendation().recommendedActiveRooms()).as("habitaciones %s", row[0]).isEqualTo(row[6]);
		}
	}

	@Test
	void horizontePorDefectoDevuelveSoloLasSeisPrimerasFilas() {
		List<ForecastPoint> points = DemandForecastCalculator.forecast(NORMATIVE, 10, PRICE, JAN_2027, 6).points();
		assertThat(points).extracting(ForecastPoint::month).containsExactly(YearMonth.of(2027, 1), YearMonth.of(2027, 2),
				YearMonth.of(2027, 3), YearMonth.of(2027, 4), YearMonth.of(2027, 5), YearMonth.of(2027, 6));
		assertThat(points.get(0).availableRoomNights()).isEqualTo(310);
	}

	/**
	 * Anti-regresión (ADR-004 §5.1): la descomposición ingenua daría b = 0.007513 y enero 0.6039.
	 */
	@Test
	void noUsaLaDescomposicionIngenuaQueSubestimaLaPendiente() {
		double naiveSlope = naiveDecompositionSlope(NORMATIVE);
		assertThat(naiveSlope).isCloseTo(0.007513, org.assertj.core.data.Offset.offset(1e-6));

		Forecast forecast = DemandForecastCalculator.forecast(NORMATIVE, 10, PRICE, JAN_2027, 1);
		assertThat(forecast.slope()).isEqualTo(rate("0.0100"));
		assertThat(forecast.points().get(0).occupancyRate()).isEqualTo(rate("0.6900"));
	}

	@Test
	void serieConstante() {
		double[] constant = new double[24];
		Arrays.fill(constant, 0.50);
		Forecast forecast = DemandForecastCalculator.forecast(constant, 10, PRICE, JAN_2027, 12);

		assertThat(forecast.slope()).isEqualByComparingTo("0");
		assertThat(forecast.intercept()).isEqualTo(rate("0.5000"));
		assertThat(forecast.seasonalIndices()).allSatisfy(s -> assertThat(s.index()).isEqualByComparingTo("0"));
		assertThat(forecast.points()).allSatisfy(p -> assertThat(p.occupancyRate()).isEqualTo(rate("0.5000")));
	}

	@Test
	void serieLinealPura() {
		double[] linear = new double[24];
		for (int t = 1; t <= 24; t++) {
			linear[t - 1] = 0.20 + 0.01 * t;
		}
		Forecast forecast = DemandForecastCalculator.forecast(linear, 10, PRICE, JAN_2027, 1);

		assertThat(forecast.slope()).isEqualTo(rate("0.0100"));
		assertThat(forecast.intercept()).isEqualTo(rate("0.2000"));
		assertThat(forecast.seasonalIndices()).allSatisfy(s -> assertThat(s.index()).isEqualByComparingTo("0"));
		assertThat(forecast.points().get(0).occupancyRate()).isEqualTo(rate("0.4500"));
	}

	@Test
	void tendenciaNegativaSeAcotaEnCero() {
		double[] falling = new double[24];
		for (int t = 1; t <= 24; t++) {
			falling[t - 1] = Math.max(0, 0.60 - 0.03 * t);
		}
		ForecastPoint last = DemandForecastCalculator.forecast(falling, 10, PRICE, JAN_2027, 12).points().get(11);

		assertThat(last.occupancyRate()).isEqualTo(rate("0.0000"));
		assertThat(last.recommendation().inventoryAction()).isEqualTo(InventoryAction.REDUCE);
		assertThat(last.recommendation().recommendedActiveRooms()).isEqualTo(1);
	}

	@Test
	void sinOcupacionOSinInventarioNoHayHistoricoSuficiente() {
		assertThatThrownBy(() -> DemandForecastCalculator.forecast(new double[24], 10, PRICE, JAN_2027, 6))
				.isInstanceOf(BusinessException.class)
				.extracting("code").isEqualTo("INSUFFICIENT_HISTORY");
		assertThatThrownBy(() -> DemandForecastCalculator.forecast(NORMATIVE, 0, PRICE, JAN_2027, 6))
				.isInstanceOf(BusinessException.class)
				.extracting("code").isEqualTo("INSUFFICIENT_HISTORY");
	}

	@Test
	void indicesPorMesCalendarioAunqueLaVentanaNoEmpieceEnEnero() {
		// Con el reloj en abril de 2027 la ventana es 2025-04..2027-03: la posición 1 es abril
		Forecast forecast = DemandForecastCalculator.forecast(NORMATIVE, 10, PRICE, YearMonth.of(2027, 4), 1);
		assertThat(forecast.seasonalIndices()).extracting(SeasonalIndex::month)
				.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
		// El primer valor de la serie (posición 1) es el índice de abril
		assertThat(forecast.seasonalIndices().get(3).index()).isEqualTo(rate("-0.0950"));
		assertThat(forecast.points().get(0).month()).isEqualTo(YearMonth.of(2027, 4));
	}

	/** Método rechazado en ADR-004: medias por mes → desestacionalizar → regresión lineal simple. */
	private static double naiveDecompositionSlope(double[] y) {
		double mean = Arrays.stream(y).average().orElseThrow();
		double[] index = new double[12];
		for (int p = 0; p < 12; p++) {
			index[p] = (y[p] + y[p + 12]) / 2 - mean;
		}
		double sumT = 0, sumY = 0, sumTT = 0, sumTY = 0;
		for (int t = 1; t <= 24; t++) {
			double deseasonalized = y[t - 1] - index[(t - 1) % 12];
			sumT += t;
			sumY += deseasonalized;
			sumTT += (double) t * t;
			sumTY += t * deseasonalized;
		}
		return (24 * sumTY - sumT * sumY) / (24 * sumTT - sumT * sumT);
	}
}
