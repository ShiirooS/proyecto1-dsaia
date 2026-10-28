package app.mnhotel.hotel_reservation_system_backend.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reservations", indexes = @Index(name = "idx_reservation_room_status_dates",
		columnList = "room_id, status, start_date, end_date"))
public class Reservation {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	// HU-02: noche de llegada (incluida). Fecha sin hora: la hora de check-in es fija (app.reservation.check-in-time)
	@Column(name="start_date")
	private LocalDate startDate;
	
	// Día de salida: esa noche NO se ocupa (otra reserva puede entrar ese mismo día)
	@Column(name="end_date")
	private LocalDate endDate;
	
	@Enumerated(EnumType.STRING)
	@Column(name="status")
	private ReservationStatus status;
	
	// HU-01: huéspedes y total calculado en el servidor (noches x precio por noche)
	@Column(name="guests")
	private Integer guests;
	
	@Column(name="total_amount")
	private Integer totalAmount;
	
	// HU-05: momento de la cancelación (null si sigue confirmada)
	@Column(name="cancelled_at")
	private LocalDateTime cancelledAt;
	
	@Column(name="created_at")
	private LocalDateTime createdAt;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
 	@JsonBackReference
 	@ToString.Exclude
 	@EqualsAndHashCode.Exclude
 	private User user;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
 	@JsonBackReference
 	@ToString.Exclude
 	@EqualsAndHashCode.Exclude
 	private Room room;
	
	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Payment> payments;
}
