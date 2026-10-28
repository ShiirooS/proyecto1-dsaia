package app.mnhotel.hotel_reservation_system_backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import app.mnhotel.hotel_reservation_system_backend.FixedClockConfig;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator.Stay;
import app.mnhotel.hotel_reservation_system_backend.dto.DemandForecastDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.DemandForecastDTO.HistoryPoint;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import app.mnhotel.hotel_reservation_system_backend.service.AnalyticsService;

/**
 * Seed del perfil dev con el reloj fijo del 28/10/2026: histórico de HU-03 (CA-03.9) y reservas de demo de HU-05.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(FixedClockConfig.class)
class DevDataSeederTest {

	@Autowired
	private AnalyticsService analyticsService;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void elHistoricoTiene24MesesConOcupacionYEstacionalidadVisible() {
		DemandForecastDTO forecast = analyticsService.getDemandForecast(6, null);

		assertThat(forecast.getHistory()).hasSize(24)
				.allSatisfy(point -> assertThat(point.occupancyRate()).isGreaterThan(BigDecimal.ZERO));
		Map<Integer, Double> averageByMonth = forecast.getHistory().stream().collect(Collectors.groupingBy(
				point -> point.month().getMonthValue(), Collectors.averagingDouble(point -> point.occupancyRate().doubleValue())));
		double summerPeak = (averageByMonth.get(7) + averageByMonth.get(8)) / 2;
		assertThat(summerPeak).isGreaterThan(averageByMonth.get(5)).isGreaterThan(averageByMonth.get(9));
		// Diciembre también es temporada alta frente a octubre
		assertThat(averageByMonth.get(12)).isGreaterThan(averageByMonth.get(10));

		assertThat(forecast.getForecast()).hasSize(6);
		assertThat(forecast.getForecast().get(0).month()).isEqualTo(YearMonth.of(2026, 10));
		assertThat(forecast.getModel().slope().doubleValue()).isPositive();
		assertThat(forecast.getHistory()).extracting(HistoryPoint::month).first().isEqualTo(YearMonth.of(2024, 10));
	}

	@Test
	void ningunaHabitacionTieneDosReservasConfirmadasEnLaMismaNoche() {
		Map<Long, List<Reservation>> byRoom = reservationRepository.findAll().stream()
				.filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
				.collect(Collectors.groupingBy(r -> r.getRoom().getId()));
		byRoom.forEach((roomId, reservations) -> {
			for (int i = 0; i < reservations.size(); i++) {
				for (int j = i + 1; j < reservations.size(); j++) {
					Stay a = new Stay(reservations.get(i).getStartDate(), reservations.get(i).getEndDate());
					Stay b = new Stay(reservations.get(j).getStartDate(), reservations.get(j).getEndDate());
					assertThat(OccupancyCalculator.overlaps(a, b)).as("habitación %d: %s y %s", roomId, a, b).isFalse();
				}
			}
		});
	}

	@Test
	void elUsuarioDemoTieneUnaReservaDeCadaTipo() {
		Long demoId = userRepository.findAll().stream()
				.filter(u -> DevDataSeeder.GUEST_USERNAME.equals(u.getUsername())).findFirst().orElseThrow().getId();
		List<Reservation> mine = reservationRepository.findByUserIdOrderByStartDateDescIdDesc(demoId);

		assertThat(mine).hasSize(4);
		assertThat(mine).filteredOn(r -> r.getStatus() == ReservationStatus.CANCELLED).hasSize(1);
		assertThat(mine).extracting(Reservation::getStartDate).contains(FixedClockConfig.NOW.toLocalDate().plusDays(1),
				FixedClockConfig.NOW.toLocalDate().plusDays(20), FixedClockConfig.NOW.toLocalDate().minusDays(30));
	}
}
