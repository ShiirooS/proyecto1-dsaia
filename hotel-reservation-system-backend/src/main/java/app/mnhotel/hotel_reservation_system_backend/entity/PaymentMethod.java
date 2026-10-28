package app.mnhotel.hotel_reservation_system_backend.entity;

import java.time.LocalDateTime;

import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * HU-01: tarjeta guardada de un usuario. Solo se persiste el token de la pasarela simulada y datos no sensibles.
 * El número completo y el CVV nunca se guardan (ADR-003).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment_method")
public class PaymentMethod {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private User user;

	// Token de la pasarela (tok_...). Nunca sale en una respuesta de la API
	@Column(name = "token", unique = true, nullable = false)
	@ToString.Exclude
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(name = "brand", nullable = false)
	private CardBrand brand;

	@Column(name = "last4", nullable = false, length = 4)
	private String last4;

	@Column(name = "exp_month", nullable = false)
	private Integer expMonth;

	@Column(name = "exp_year", nullable = false)
	private Integer expYear;

	@Column(name = "holder_name", nullable = false)
	private String holderName;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
