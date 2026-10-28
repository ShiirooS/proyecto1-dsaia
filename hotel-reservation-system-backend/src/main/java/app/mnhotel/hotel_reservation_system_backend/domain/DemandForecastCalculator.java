package app.mnhotel.hotel_reservation_system_backend.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import app.mnhotel.hotel_reservation_system_backend.domain.PricingRules.Recommendation;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;

/**
 * HU-03: previsión de demanda con tendencia lineal + estacionalidad mensual ajustada por mínimos cuadrados (ADR-004,
 * normativo). Lógica pura: recibe las 24 ocupaciones mensuales y devuelve el modelo y el pronóstico.
 *
 * Con exactamente dos ciclos de 12 meses el ajuste OLS de y_t = b·t + c_p(t) tiene solución cerrada:
 * b = Σ(y_t − y_{t−12}) / 144 y c_p = (y_p + y_{p+12}) / 2 − b·(p + 6).
 */
public final class DemandForecastCalculator {

	public static final int HISTORY_MONTHS = 24;
	public static final int MIN_HORIZON = 1;
	public static final int MAX_HORIZON = 12;
	private static final int SCALE = 4;

	/** Índice estacional del mes calendario (1 = enero). */
	public record SeasonalIndex(int month, BigDecimal index) {
	}

	public record ForecastPoint(YearMonth month, BigDecimal occupancyRate, int forecastRoomNights, int availableRoomNights,
			Recommendation recommendation) {
	}

	/**
	 * trendRates[t − 1] = a + b·t para t = 1..24, ya redondeados. seasonalIndices en orden de enero a diciembre.
	 */
	public record Forecast(BigDecimal intercept, BigDecimal slope, List<SeasonalIndex> seasonalIndices,
			List<BigDecimal> trendRates, List<ForecastPoint> points) {
	}

	private DemandForecastCalculator() {
	}

	/**
	 * @param occupancy    y_1..y_24: ocupación de los 24 meses completos anteriores a currentMonth (0..1)
	 * @param activeRooms  R, habitaciones activas del alcance
	 * @param averagePrice P̄, precio medio por noche de las habitaciones activas (2 decimales)
	 * @param currentMonth M_25: el pronóstico empieza aquí
	 * @param horizon      H, meses a pronosticar (1..12)
	 */
	public static Forecast forecast(double[] occupancy, int activeRooms, BigDecimal averagePrice, YearMonth currentMonth,
			int horizon) {
		if (occupancy.length != HISTORY_MONTHS) {
			throw new IllegalArgumentException("Se necesitan exactamente " + HISTORY_MONTHS + " meses de histórico");
		}
		if (horizon < MIN_HORIZON || horizon > MAX_HORIZON) {
			throw new IllegalArgumentException("El horizonte debe estar entre 1 y 12");
		}
		double total = 0;
		for (double y : occupancy) {
			total += y;
		}
		if (activeRooms <= 0 || total == 0) {
			throw insufficientHistory();
		}

		// Paso 5: pendiente = cambio interanual medio / 12
		double yearOverYear = 0;
		for (int t = 13; t <= HISTORY_MONTHS; t++) {
			yearOverYear += y(occupancy, t) - y(occupancy, t - 12);
		}
		double slope = yearOverYear / 144;

		// Pasos 6 y 7: componente por posición del ciclo, intercepto e índices (Σ S_p = 0)
		double[] component = new double[13];
		double componentSum = 0;
		for (int p = 1; p <= 12; p++) {
			component[p] = (y(occupancy, p) + y(occupancy, p + 12)) / 2 - slope * (p + 6);
			componentSum += component[p];
		}
		double intercept = componentSum / 12;
		double[] seasonal = new double[13];
		for (int p = 1; p <= 12; p++) {
			seasonal[p] = component[p] - intercept;
		}

		YearMonth firstMonth = currentMonth.minusMonths(HISTORY_MONTHS);
		SeasonalIndex[] byCalendarMonth = new SeasonalIndex[12];
		for (int p = 1; p <= 12; p++) {
			int calendarMonth = firstMonth.plusMonths(p - 1L).getMonthValue();
			byCalendarMonth[calendarMonth - 1] = new SeasonalIndex(calendarMonth, round(seasonal[p]));
		}

		// Paso 8: tendencia histórica para el gráfico
		List<BigDecimal> trendRates = new ArrayList<>(HISTORY_MONTHS);
		for (int t = 1; t <= HISTORY_MONTHS; t++) {
			trendRates.add(round(intercept + slope * t));
		}

		// Pasos 9 a 12: pronóstico acotado a [0, 1]; las reglas usan la ocupación ya redondeada
		BigDecimal price = averagePrice.setScale(2, RoundingMode.HALF_UP);
		List<ForecastPoint> points = new ArrayList<>(horizon);
		for (int h = 1; h <= horizon; h++) {
			int t = HISTORY_MONTHS + h;
			double raw = intercept + slope * t + seasonal[position(t)];
			BigDecimal occ = round(Math.min(1, Math.max(0, raw)));
			YearMonth month = currentMonth.plusMonths(h - 1L);
			int available = activeRooms * month.lengthOfMonth();
			int roomNights = occ.multiply(BigDecimal.valueOf(available)).setScale(0, RoundingMode.HALF_UP).intValueExact();
			points.add(new ForecastPoint(month, occ, roomNights, available, PricingRules.recommend(occ, activeRooms, price)));
		}

		return new Forecast(round(intercept), round(slope), List.of(byCalendarMonth), trendRates, points);
	}

	/** Paso 4: posición en el ciclo, p(t) = ((t − 1) mod 12) + 1. */
	static int position(int t) {
		return ((t - 1) % 12) + 1;
	}

	/** Tasa redondeada a 4 decimales HALF_UP (todas las salidas del modelo). */
	public static BigDecimal round(double value) {
		return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
	}

	public static BusinessException insufficientHistory() {
		return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_HISTORY",
				"No hay suficiente histórico para calcular la previsión");
	}

	private static double y(double[] occupancy, int t) {
		return occupancy[t - 1];
	}
}
