package app.mnhotel.hotel_reservation_system_backend.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
	 * Dos estancias se solapan si comparten al menos una noche.
	 */
	public static boolean overlaps(Stay a, Stay b) {
		return a.checkIn().isBefore(b.checkOut()) && b.checkIn().isBefore(a.checkOut());
	}
}
