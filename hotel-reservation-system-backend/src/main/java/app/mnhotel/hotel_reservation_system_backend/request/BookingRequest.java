package app.mnhotel.hotel_reservation_system_backend.request;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HU-01: POST /api/reservation/book. Exactamente una fuente de pago: paymentMethodId (tarjeta guardada, sin CVV)
 * o card (tarjeta nueva, con saveCard opcional). El usuario sale del token, nunca del body.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {

	@NotNull
	private Long roomId;

	@NotNull
	private LocalDate checkIn;

	@NotNull
	private LocalDate checkOut;

	@NotNull
	@Min(1)
	@Max(10)
	private Integer guests;

	private Long paymentMethodId;

	@Valid
	private CardRequest card;

	private boolean saveCard;
}
