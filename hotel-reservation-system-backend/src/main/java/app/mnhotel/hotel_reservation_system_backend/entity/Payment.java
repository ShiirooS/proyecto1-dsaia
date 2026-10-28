package app.mnhotel.hotel_reservation_system_backend.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import app.mnhotel.hotel_reservation_system_backend.enums.PaymentStatus;
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
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name="amount")
	private Integer amount;
	
	@Column(name="payment_date")
	private LocalDateTime paymentDate;
	
	@Column(name = "status")
	@Enumerated(EnumType.STRING)  // O EnumType.ORDINAL si prefieres almacenar el índice
	private PaymentStatus status;

	// HU-01: copia de marca y últimos 4 (no referencia a PaymentMethod: borrar una tarjeta no rompe el historial)
	@Column(name = "card_brand")
	private String cardBrand;

	@Column(name = "card_last4")
	private String cardLast4;

	// Identificador de la transacción en la pasarela simulada (txn_...)
	@Column(name = "transaction_id")
	private String transactionId;

	// HU-05: importe reembolsado al cancelar con antelación
	@Column(name = "refunded_amount")
	@Builder.Default
	private Integer refundedAmount = 0;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reservation_id")
 	@JsonBackReference
 	private Reservation reservation;
}
