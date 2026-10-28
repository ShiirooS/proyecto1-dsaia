package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HU-01: tarjeta guardada tal como la ve el usuario. Nunca incluye el token de la pasarela.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDTO {
	private Long id;
	private CardBrand brand;
	private String last4;
	private Integer expMonth;
	private Integer expYear;
	private String holderName;

	// Sin @JsonProperty, Jackson lo serializaría como "default"
	@JsonProperty("isDefault")
	private boolean isDefault;

	private boolean expired;
	private LocalDateTime createdAt;
}
