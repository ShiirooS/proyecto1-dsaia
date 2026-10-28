package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.dto.LoginRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.RegisterRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.UserProfileDTO;
import app.mnhotel.hotel_reservation_system_backend.response.AuthResponse;

public interface AuthService {

	public AuthResponse register(RegisterRequest request);
	public AuthResponse login(LoginRequest request);
	public UserProfileDTO getProfile(String username);
}
