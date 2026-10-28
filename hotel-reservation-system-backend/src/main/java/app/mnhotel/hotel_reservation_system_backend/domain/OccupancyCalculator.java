package app.mnhotel.hotel_reservation_system_backend.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Reglas puras de ocupación (sin Spring ni JPA). Una noche {@code d} está ocupada por una estancia
 * si {@code checkIn <= d < checkOut}: el día de salida queda libre para otra llegada.
 */
public final class OccupancyCalculator {

	public record Stay(LocalDate checkIn, LocalDate checkOut) {
	}

	private OccupancyCalculator() {
	}

	/**
	 * Noches ocupadas dentro de [from, to] (ambos inclusive), en orden ascendente y sin duplicados.
	 */
	public static List<LocalDate> occupiedNights(Collection<Stay> stays, LocalDate from, LocalDate to) {
		TreeSet<LocalDate> nights = new TreeSet<>();
		for (Stay stay : stays) {
			LocalDate first = stay.checkIn().isBefore(from) ? from : stay.checkIn();
			for (LocalDate night = first; night.isBefore(stay.checkOut()) && !night.isAfter(to); night = night.plusDays(1)) {
				nights.add(night);
			}
		}
		return List.copyOf(nights);
	}

	/**
	 * HU-03 (ADR-004 §3 paso 3): noches ocupadas en cada mes de [fromMonth, toMonth], ambos inclusive y en orden.
	 * Una estancia que cruza meses reparte sus noches; una estancia no válida (checkOut <= checkIn) no suma nada.
	 * Recibe solo estancias CONFIRMED: el filtro por estado lo hace la consulta.
	 */
	public static Map<YearMonth, Integer> nightsPerMonth(Collection<Stay> stays, YearMonth fromMonth, YearMonth toMonth) {
		Map<YearMonth, Integer> nights = new LinkedHashMap<>();
		for (YearMonth month = fromMonth; !month.isAfter(toMonth); month = month.plusMonths(1)) {
			nights.put(month, 0);
		}
		LocalDate windowStart = fromMonth.atDay(1);
		LocalDate windowEnd = toMonth.plusMonths(1).atDay(1);
		for (Stay stay : stays) {
			LocalDate first = stay.checkIn().isBefore(windowStart) ? windowStart : stay.checkIn();
			LocalDate end = stay.checkOut().isAfter(windowEnd) ? windowEnd : stay.checkOut();
			for (LocalDate night = first; night.isBefore(end); night = night.plusDays(1)) {
				nights.merge(YearMonth.from(night), 1, Integer::sum);
			}
		}
		return nights;
	}

	/**
	 * Dos estancias se solapan si comparten al menos una noche.
	 */
	public static boolean overlaps(Stay a, Stay b) {
		return a.checkIn().isBefore(b.checkOut()) && b.checkIn().isBefore(a.checkOut());
	}
}
