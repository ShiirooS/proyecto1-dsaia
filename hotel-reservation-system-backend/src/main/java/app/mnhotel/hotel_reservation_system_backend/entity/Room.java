package app.mnhotel.hotel_reservation_system_backend.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "rooms")
public class Room {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id")
 	@JsonBackReference
 	private Hotel hotel;

	@Column(name="room_number")
    private String roomNumber;

	@Column(name="type")
	private String type;

	@Column(name="price_per_night", precision = 10, scale = 2)
	private BigDecimal pricePerNight;

	// Huéspedes máximos
	@Column(name="capacity")
	private Integer capacity;

	// Habitación a la venta. NO indica disponibilidad: esa se deriva de las reservas (ADR-005)
	@Column(name="active")
	@Builder.Default
	private Boolean active = true;

	@Column(name="description")
	private String description;

	@Column(name="image_url")
	private String imageUrl;

}
