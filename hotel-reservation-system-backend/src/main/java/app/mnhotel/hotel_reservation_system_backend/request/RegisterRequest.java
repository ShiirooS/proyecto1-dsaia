package app.mnhotel.hotel_reservation_system_backend.request;


import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
	private Long id;

	// Aquí se almacenará el email (se guarda en minúsculas)
	@NotBlank
	@Email
	@Size(max = 120)
    private String username;

	// 8 a 72 caracteres (límite de BCrypt) con al menos una letra y un número
	@NotBlank
	@Size(min = 8, max = 72)
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$", message = "debe contener al menos una letra y un número")
	@ToString.Exclude
    private String password;

	@Size(max = 100)
    private String fullName; // Cambiado de `fullname` a `fullName` para seguir el estándar de Java
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
