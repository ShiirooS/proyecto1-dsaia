package app.mnhotel.hotel_reservation_system_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

	// Acepta nombre de usuario o email
	@NotBlank
	private String username;

	@NotBlank
	private String password;
}
