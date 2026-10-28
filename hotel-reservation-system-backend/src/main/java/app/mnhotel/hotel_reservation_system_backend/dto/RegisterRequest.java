package app.mnhotel.hotel_reservation_system_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

	@NotBlank
	@Size(min = 3, max = 30)
	@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo letras, números, punto, guion y guion bajo")
	private String username;

	@NotBlank
	@Email
	private String email;

	// Mínimo 8 caracteres con al menos una letra y un número
	@NotBlank
	@Size(min = 8, max = 72)
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$", message = "debe contener al menos una letra y un número")
	private String password;

	@NotBlank
	@Size(max = 100)
	private String fullName;

	@Size(max = 30)
	private String phone;
}
