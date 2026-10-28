package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.service.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImp implements JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtServiceImp(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		// HS256 exige una clave de al menos 256 bits (32 bytes)
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	@Override
	public String generateToken(User user) {
		Date now = new Date();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	@Override
	public String extractValidUsername(String token) {
		try {
			return Jwts.parser().verifyWith(key).build()
					.parseSignedClaims(token)
					.getPayload()
					.getSubject();
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
