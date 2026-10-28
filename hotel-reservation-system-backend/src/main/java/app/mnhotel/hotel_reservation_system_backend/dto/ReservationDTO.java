package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

	 private Long id;
	 private LocalDate startDate;
	 private LocalDate endDate;
	 private ReservationStatus status;
	 private LocalDateTime createdAt;

	 // Información del usuario que realizó la reserva
	 private Long userId;

	 // Información de la habitación reservada
	 private Long roomId;

	 // Pagos asociados a la reserva
	 private List<PaymentDTO> payments;
}
