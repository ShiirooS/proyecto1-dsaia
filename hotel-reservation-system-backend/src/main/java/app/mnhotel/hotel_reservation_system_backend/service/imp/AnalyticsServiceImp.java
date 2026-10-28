package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.domain.DemandForecastCalculator;
import app.mnhotel.hotel_reservation_system_backend.domain.DemandForecastCalculator.Forecast;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator.Stay;
import app.mnhotel.hotel_reservation_system_backend.dto.DemandForecastDTO;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.service.AnalyticsService;

/**
 * Carga los datos de la previsión (inventario, precio medio y noches por mes) y delega el cálculo en
 * DemandForecastCalculator, que es puro (ADR-004 §6).
 */
@Service
public class AnalyticsServiceImp implements AnalyticsService {

	private static final String MODEL_TYPE = "LINEAR_TREND_MONTHLY_SEASONALITY";

	@Autowired
	private HotelRepository hotelRepository;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private Clock clock;

	@Override
	@Transactional(readOnly = true)
	public DemandForecastDTO getDemandForecast(int horizon, Long hotelId) {
		if (hotelId != null && !hotelRepository.existsById(hotelId)) {
			throw new ResourceNotFoundException("HOTEL_NOT_FOUND", "Hotel no encontrado");
		}

		// Paso 1: ventana de 24 meses completos; el pronóstico empieza en el mes actual
		YearMonth currentMonth = YearMonth.now(clock);
		YearMonth historyFrom = currentMonth.minusMonths(DemandForecastCalculator.HISTORY_MONTHS);
		YearMonth historyTo = currentMonth.minusMonths(1);
		YearMonth lastForecastMonth = currentMonth.plusMonths(horizon - 1L);

		// Paso 2: inventario R (se supone constante en todo el histórico, limitación aceptada en el ADR)
		int activeRooms = Math.toIntExact(roomRepository.countActive(hotelId));
		if (activeRooms == 0) {
			throw DemandForecastCalculator.insufficientHistory();
		}
		BigDecimal averagePrice = BigDecimal.valueOf(roomRepository.averageActivePricePerNight(hotelId))
				.setScale(2, RoundingMode.HALF_UP);

		// Paso 3 (y 13): noches CONFIRMED por mes en el histórico y en los meses pronosticados ("ya reservadas")
		List<Stay> stays = reservationRepository
				.findConfirmedStaysIntersectingPeriod(hotelId, historyFrom.atDay(1), lastForecastMonth.plusMonths(1).atDay(1))
				.stream()
				.map(s -> new Stay(s.getCheckIn(), s.getCheckOut()))
				.toList();
		Map<YearMonth, Integer> nights = OccupancyCalculator.nightsPerMonth(stays, historyFrom, lastForecastMonth);

		double[] occupancy = new double[DemandForecastCalculator.HISTORY_MONTHS];
		for (int t = 1; t <= occupancy.length; t++) {
			YearMonth month = historyFrom.plusMonths(t - 1L);
			occupancy[t - 1] = Math.min(1, nights.get(month) / (double) capacity(activeRooms, month));
		}
		Forecast forecast = DemandForecastCalculator.forecast(occupancy, activeRooms, averagePrice, currentMonth, horizon);

		List<DemandForecastDTO.HistoryPoint> history = new ArrayList<>();
		for (int t = 1; t <= occupancy.length; t++) {
			YearMonth month = historyFrom.plusMonths(t - 1L);
			history.add(new DemandForecastDTO.HistoryPoint(month, nights.get(month), capacity(activeRooms, month),
					DemandForecastCalculator.round(occupancy[t - 1]), forecast.trendRates().get(t - 1)));
		}

		List<DemandForecastDTO.ForecastPoint> points = forecast.points().stream().map(point -> {
			int booked = nights.get(point.month());
			return new DemandForecastDTO.ForecastPoint(point.month(), point.occupancyRate(), point.forecastRoomNights(),
					point.availableRoomNights(), booked,
					DemandForecastCalculator.round(booked / (double) point.availableRoomNights()),
					point.recommendation().priceAdjustmentPct(), point.recommendation().suggestedPricePerNight(),
					point.recommendation().inventoryAction(), point.recommendation().recommendedActiveRooms());
		}).toList();

		return DemandForecastDTO.builder()
				.generatedAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS))
				.hotelId(hotelId)
				.horizon(horizon)
				.activeRooms(activeRooms)
				.averagePricePerNight(averagePrice)
				.historyFrom(historyFrom)
				.historyTo(historyTo)
				.model(new DemandForecastDTO.Model(MODEL_TYPE, forecast.intercept(), forecast.slope(),
						forecast.seasonalIndices().stream()
								.map(s -> new DemandForecastDTO.SeasonalIndex(s.month(), s.index()))
								.toList()))
				.history(history)
				.forecast(points)
				.build();
	}

	private static int capacity(int activeRooms, YearMonth month) {
		return activeRooms * month.lengthOfMonth();
	}
}
