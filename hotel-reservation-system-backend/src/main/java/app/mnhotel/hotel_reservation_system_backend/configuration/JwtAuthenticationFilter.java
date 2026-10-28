package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import app.mnhotel.hotel_reservation_system_backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Lee el header "Authorization: Bearer {jwt}" y, si el token es válido, autentica la petición.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String username = jwtService.extractValidUsername(header.substring(BEARER.length()));
			if (username != null) {
				try {
					UserDetails user = userDetailsService.loadUserByUsername(username);
					var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} catch (UsernameNotFoundException e) {
					// Token de un usuario que ya no existe: la petición sigue como anónima
				}
			}
		}
		chain.doFilter(request, response);
	}
}
