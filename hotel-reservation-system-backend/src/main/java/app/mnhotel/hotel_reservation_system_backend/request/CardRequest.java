package app.mnhotel.hotel_reservation_system_backend.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * HU-01: datos de una tarjeta nueva. El número y el CVV se excluyen de toString para que nunca lleguen a un log.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardRequest {

	// 13 a 19 dígitos; se aceptan espacios y guiones, que se eliminan al validar
	@NotBlank
	// Sin @Pattern: Bean Validation registra en DEBUG el valor rechazado. El formato se valida en
	// CardValidator.checkFormat con un mensaje fijo que nunca incluye el número
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
	@ToString.Exclude
	private String cvv;

	@NotBlank
	@Size(min = 2, max = 60)
	private String holderName;
}
