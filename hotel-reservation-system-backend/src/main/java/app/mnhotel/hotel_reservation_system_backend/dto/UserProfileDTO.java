package app.mnhotel.hotel_reservation_system_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos públicos del usuario autenticado (sin contraseña).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
	private Long id;
	private String username;
	private String email;
	private String fullName;
	private String phone;
	private String role;
	private LocalDateTime createdAt;
}
