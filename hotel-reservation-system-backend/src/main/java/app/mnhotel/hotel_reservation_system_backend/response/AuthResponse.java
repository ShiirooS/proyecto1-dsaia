package app.mnhotel.hotel_reservation_system_backend.response;

import app.mnhotel.hotel_reservation_system_backend.dto.UserProfileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
	private String token;
	private UserProfileDTO user;
}
