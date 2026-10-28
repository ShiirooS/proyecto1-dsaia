package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDateTime;

import app.mnhotel.hotel_reservation_system_backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Long id;
	private Integer amount;
	private LocalDateTime paymentDate;
	private PaymentStatus status;
 	private Long idReservation;

 	// HU-01: datos no sensibles de la tarjeta usada y de la transacción
 	private String cardBrand;
 	private String cardLast4;
 	private String transactionId;
 	private Integer refundedAmount;
}
