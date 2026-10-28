package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityDTO {
	private Long roomId;

	// Rango consultado, ambos inclusive
	private LocalDate from;
	private LocalDate to;

	// "Hoy" según la zona horaria del hotel: el calendario deshabilita fechas anteriores
	private LocalDate today;
	private String checkInTime;
	private Integer maxNights;

	// Noches ocupadas por reservas CONFIRMED dentro de [from, to]
	private List<LocalDate> occupiedDates;
}
