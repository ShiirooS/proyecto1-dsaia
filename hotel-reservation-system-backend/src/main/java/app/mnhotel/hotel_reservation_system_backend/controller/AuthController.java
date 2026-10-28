package app.mnhotel.hotel_reservation_system_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import app.mnhotel.hotel_reservation_system_backend.exception.ConflictException;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import app.mnhotel.hotel_reservation_system_backend.request.LoginRequest;
import app.mnhotel.hotel_reservation_system_backend.request.RegisterRequest;
import app.mnhotel.hotel_reservation_system_backend.response.AuthResponse;
import app.mnhotel.hotel_reservation_system_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173") // Reemplaza esto con el dominio de tu frontend
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	 @Autowired
	 private AuthService authService;
	 
	 @Autowired
	 private UserRepository userRepository;

	    
	    // API que autentica el usuario que se loguea.
	    @PostMapping("/login")
	    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
	    	return ResponseEntity.ok(authService.loginUser(request));
	    }
	    
	    // API que registra un nuevo usuario.
	    @PostMapping("/registration-user")
	    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request)
		{	
	    	request.setUsername(request.getUsername().trim().toLowerCase());
	    	if (userRepository.existsByUsername(request.getUsername())) {
	            // HU-04: 409 con código estable (antes 400 con el mensaje dentro del campo "token")
	            throw new ConflictException("USERNAME_ALREADY_EXISTS", "El nombre de usuario ya está en uso");
	        }
	        return ResponseEntity.ok(authService.registerUser(request));
	    }

}
