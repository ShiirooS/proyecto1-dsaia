package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.dto.DemandForecastDTO;

/**
 * HU-03: analítica para el administrador. La autoridad ADMIN la impone SecurityConfig.
 */
public interface AnalyticsService {

	/**
	 * Previsión de demanda de los próximos {@code horizon} meses a partir de los 24 meses completos anteriores
	 * (ADR-004). hotelId null = todos los hoteles. 404 HOTEL_NOT_FOUND, 422 INSUFFICIENT_HISTORY.
	 */
	public DemandForecastDTO getDemandForecast(int horizon, Long hotelId);
}
