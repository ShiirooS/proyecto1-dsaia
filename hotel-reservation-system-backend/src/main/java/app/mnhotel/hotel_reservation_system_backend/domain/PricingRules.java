package app.mnhotel.hotel_reservation_system_backend.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * HU-03: reglas de precio e inventario a partir de la ocupación prevista (ADR-004 §3 pasos 11 y 12).
 * Recibe la ocupación ya redondeada a 4 decimales y trabaja en BigDecimal para que los umbrales sean exactos.
 */
public final class PricingRules {

	public static final BigDecimal TARGET_OCCUPANCY = new BigDecimal("0.80");
	private static final BigDecimal HIGH = new BigDecimal("0.85");
	private static final BigDecimal MEDIUM = new BigDecimal("0.70");
	private static final BigDecimal LOW = new BigDecimal("0.40");
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public enum InventoryAction { INCREASE, KEEP, REDUCE }

	public record Recommendation(int priceAdjustmentPct, int suggestedPricePerNight, InventoryAction inventoryAction,
			int recommendedActiveRooms) {
	}

	private PricingRules() {
	}

	public static Recommendation recommend(BigDecimal occupancy, int activeRooms, BigDecimal averagePricePerNight) {
		int pct = priceAdjustmentPct(occupancy);
		// Room.pricePerNight es Integer: el precio sugerido se redondea a unidades (HALF_UP)
		int price = averagePricePerNight.multiply(BigDecimal.valueOf(100L + pct))
				.divide(HUNDRED, 0, RoundingMode.HALF_UP).intValueExact();

		InventoryAction action;
		int rooms;
		if (occupancy.compareTo(HIGH) > 0) {
			action = InventoryAction.INCREASE;
			rooms = roomsForTarget(occupancy, activeRooms);
		} else if (occupancy.compareTo(LOW) < 0) {
			action = InventoryAction.REDUCE;
			rooms = Math.max(1, roomsForTarget(occupancy, activeRooms));
		} else {
			action = InventoryAction.KEEP;
			rooms = activeRooms;
		}
		return new Recommendation(pct, price, action, rooms);
	}

	static int priceAdjustmentPct(BigDecimal occupancy) {
		if (occupancy.compareTo(HIGH) > 0) {
			return 15;
		}
		if (occupancy.compareTo(MEDIUM) >= 0) {
			return 8;
		}
		return occupancy.compareTo(LOW) >= 0 ? 0 : -10;
	}

	/** ceil(R × occ / 0.80) calculado en decimal exacto: 10 × 0.88 / 0.80 = 11 no sube a 12 (ADR-004 §5.3). */
	private static int roomsForTarget(BigDecimal occupancy, int activeRooms) {
		return BigDecimal.valueOf(activeRooms).multiply(occupancy)
				.divide(TARGET_OCCUPANCY, 0, RoundingMode.CEILING).intValueExact();
	}
}
