package app.mnhotel.hotel_reservation_system_backend.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * HU-01: alta de una tarjeta guardada (POST /api/payment-method/registration-payment-method).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodRequest {

	@NotBlank
	@Pattern(regexp = "^[0-9 -]{13,23}$", message = "debe tener entre 13 y 19 dígitos")
	@ToString.Exclude
	private String cardNumber;

	@NotNull
	@Min(1)
	@Max(12)
	private Integer expMonth;

	@NotNull
	@Min(2000)
	@Max(2099)
	private Integer expYear;

	@NotBlank
	@Pattern(regexp = "^[0-9]{3,4}$", message = "debe tener 3 o 4 dígitos")
	@ToString.Exclude
	private String cvv;

	@NotBlank
	@Size(min = 2, max = 60)
	private String holderName;

	private boolean makeDefault;
}
