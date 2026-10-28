package app.mnhotel.hotel_reservation_system_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import app.mnhotel.hotel_reservation_system_backend.domain.PricingRules.InventoryAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HU-03: previsión de demanda para el administrador (API-CONTRACT §10.1). Tasas entre 0 y 1 con 4 decimales;
 * los meses se serializan como "YYYY-MM".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandForecastDTO {

	private LocalDateTime generatedAt;
	private Long hotelId;
	private int horizon;
	private int activeRooms;
	private BigDecimal averagePricePerNight;
	private YearMonth historyFrom;
	private YearMonth historyTo;
	private Model model;
	private List<HistoryPoint> history;
	private List<ForecastPoint> forecast;

	public record Model(String type, BigDecimal intercept, BigDecimal slope, List<SeasonalIndex> seasonalIndices) {
	}

	public record SeasonalIndex(int month, BigDecimal index) {
	}

	public record HistoryPoint(YearMonth month, int occupiedRoomNights, int availableRoomNights, BigDecimal occupancyRate,
			BigDecimal trendRate) {
	}

	public record ForecastPoint(YearMonth month, BigDecimal forecastOccupancyRate, int forecastRoomNights,
			int availableRoomNights, int bookedRoomNights, BigDecimal bookedOccupancyRate, int priceAdjustmentPct,
			int suggestedPricePerNight, InventoryAction inventoryAction, int recommendedActiveRooms) {
	}
}
