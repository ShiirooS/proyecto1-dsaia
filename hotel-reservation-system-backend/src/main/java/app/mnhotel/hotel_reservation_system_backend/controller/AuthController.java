package app.mnhotel.hotel_reservation_system_backend.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.mnhotel.hotel_reservation_system_backend.dto.LoginRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.RegisterRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.UserProfileDTO;
import app.mnhotel.hotel_reservation_system_backend.response.AuthResponse;
import app.mnhotel.hotel_reservation_system_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// API para registrar un huésped (rol USER) y devolver su token.
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	// API para iniciar sesión con usuario o email.
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	// API para obtener el perfil del usuario autenticado.
	@GetMapping("/me")
	public ResponseEntity<UserProfileDTO> me(Principal principal) {
		return ResponseEntity.ok(authService.getProfile(principal.getName()));
	}
}
