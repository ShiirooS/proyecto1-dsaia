package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import app.mnhotel.hotel_reservation_system_backend.domain.CancellationPolicy;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HU-01/HU-05: reserva tal como la ve su dueño (API-CONTRACT §6.1 a §6.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyReservationDTO {

	private Long id;
	private ReservationStatus status;
	private CancellationPolicy.Phase phase;
	private LocalDate checkIn;
	private LocalDate checkOut;
	private long nights;
	private Integer guests;
	private Integer totalAmount;
	private LocalDateTime createdAt;
	private LocalDateTime cancelledAt;
	private RoomInfo room;
	private HotelInfo hotel;
	private PaymentInfo payment;
	private CancellationPolicy.Cancellation cancellation;

	// Solo en la respuesta de POST /api/reservation/book: id de la tarjeta guardada (o reutilizada)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long savedPaymentMethodId;

	public record RoomInfo(Long id, String roomNumber, String type, String image) {
	}

	public record HotelInfo(Long id, String name, String location) {
	}

	public record PaymentInfo(Long id, String status, Integer amount, Integer refundedAmount, String cardBrand,
			String cardLast4, String transactionId, LocalDateTime paymentDate) {
	}
}
