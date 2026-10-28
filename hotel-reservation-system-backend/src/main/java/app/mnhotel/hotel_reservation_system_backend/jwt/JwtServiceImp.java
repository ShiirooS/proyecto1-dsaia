package app.mnhotel.hotel_reservation_system_backend.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.jwt.JwtService;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImp implements JwtService{

	// El secreto ya no está en el código: se lee de app.jwt.secret (variable JWT_SECRET, Base64 de >= 32 bytes).
	// Sin valor por defecto fuera de dev/test: la aplicación no arranca si falta (HU-04).
	private final Key key;
	private final long expirationMs;

	public JwtServiceImp(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		// hmacShaKeyFor lanza WeakKeyException si la clave tiene menos de 256 bits
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.expirationMs = expirationMs;
	}

    // Claim con el id inmutable del usuario: el username (email) puede cambiar
    private static final String UID_CLAIM = "uid";

    public String getToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        if (user instanceof User appUser) {
            claims.put(UID_CLAIM, appUser.getId());
        }
        return getToken(claims, user);
    }

    private String getToken(Map<String,Object> extraClaims, UserDetails user) {
        return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(user.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis()+expirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username=getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && sameUser(token, userDetails));
    }

    // HU-04: si un ADMIN renombra una cuenta y otra persona se registra con el email anterior,
    // el token viejo (mismo sub) no debe servir para la cuenta nueva: se compara el id inmutable
    private boolean sameUser(String token, UserDetails userDetails) {
        if (!(userDetails instanceof User appUser)) {
            return true;
        }
        Object uid = getAllClaims(token).get(UID_CLAIM);
        return uid != null && appUser.getId() != null && appUser.getId().equals(((Number) uid).longValue());
    }

    private Claims getAllClaims(String token)
    {
        return Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public <T> T getClaim(String token, Function<Claims,T> claimsResolver)
    {
        final Claims claims=getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Date getExpiration(String token)
    {
        return getClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token)
    {
        return getExpiration(token).before(new Date());
    }

}
