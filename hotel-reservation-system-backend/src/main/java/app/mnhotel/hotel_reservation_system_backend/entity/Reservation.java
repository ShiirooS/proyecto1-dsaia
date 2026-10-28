package app.mnhotel.hotel_reservation_system_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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
@Table(name = "reservations")
public class Reservation {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	// Noche de llegada (incluida)
	@Column(name="check_in", nullable = false)
	private LocalDate checkIn;

	// Día de salida: esa noche NO se ocupa
	@Column(name="check_out", nullable = false)
	private LocalDate checkOut;

	@Column(name="guests")
	private Integer guests;

	@Column(name="total_amount", precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name="status", nullable = false)
	private ReservationStatus status;

	@Column(name="created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
 	@JsonBackReference
 	private User user;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
 	@JsonBackReference
 	private Room room;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Payment> payments;
}
