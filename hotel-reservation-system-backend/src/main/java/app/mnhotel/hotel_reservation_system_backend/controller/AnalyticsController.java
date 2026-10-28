package app.mnhotel.hotel_reservation_system_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.mnhotel.hotel_reservation_system_backend.dto.DemandForecastDTO;
import app.mnhotel.hotel_reservation_system_backend.service.AnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * HU-03: analítica del administrador. Solo ADMIN (SecurityConfig: /api/analytics/**).
 */
@RestController
@RequestMapping("/api/analytics")
@Validated
public class AnalyticsController {

	@Autowired
	private AnalyticsService analyticsService;

	// horizon fuera de 1..12 -> 400 INVALID_PARAMETER; hotelId inexistente -> 404; sin histórico -> 422
	@GetMapping("/demand-forecast")
	public ResponseEntity<DemandForecastDTO> getDemandForecast(
			@RequestParam(defaultValue = "6") @Min(1) @Max(12) int horizon,
			@RequestParam(required = false) Long hotelId) {
		return ResponseEntity.ok(analyticsService.getDemandForecast(horizon, hotelId));
	}
}
