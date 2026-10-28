package app.mnhotel.hotel_reservation_system_backend.configuration;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import app.mnhotel.hotel_reservation_system_backend.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * HU-04: control de acceso por roles. La matriz completa está en docs/architecture/API-CONTRACT.md.
 * Las autoridades son "ADMIN" y "USER" sin prefijo ROLE_ (User.getAuthorities), por eso se usa hasAuthority.
 * Las reglas de "dueño del recurso" (ver o editar lo propio) se validan en CurrentUserService.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String ADMIN = "ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authProvider;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    // Solo el perfil dev habilita la consola H2
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        return http
            // API stateless con JWT en header: no hay cookie de sesión que proteger con CSRF
            .csrf(csrf ->
                csrf
                .disable())
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(authRequest -> {
              authRequest
              	.requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/registration-user").permitAll()
                // Catálogo público
                .requestMatchers(HttpMethod.GET,
                		"/api/hotel/get-all", "/api/hotel/get-hotel/*",
                		"/api/room/get-all", "/api/room/get-room/*", "/api/room/get-availability/*",
                		"/api/review/get-all", "/api/review/get-review/*").permitAll()
                // Cualquier autenticado: su perfil, publicar reseñas, reservar y ver/cancelar lo suyo,
                // y sus métodos de pago. La regla "solo el dueño" se aplica en los servicios (ajeno -> 404)
                .requestMatchers(HttpMethod.GET, "/api/user/current", "/api/user/get-user/*",
                		"/api/reservation/my-reservations", "/api/reservation/my-reservations/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/user/update-user/*", "/api/reservation/cancel-reservation/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/review/registration-review", "/api/reservation/book").authenticated()
                .requestMatchers("/api/payment-method/**").authenticated()
                // Todo lo demás de gestión es solo ADMIN (matriz RBAC, API-CONTRACT §13)
                .requestMatchers("/api/hotel/**", "/api/room/**", "/api/review/**", "/api/user/**",
                		"/api/reservation/**", "/api/payment/**", "/api/analytics/**").hasAuthority(ADMIN);
              if (h2ConsoleEnabled) {
                  authRequest.requestMatchers("/h2-console/**").permitAll();
              }
              authRequest
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated();
            })
            .sessionManagement(sessionManager->
                sessionManager
                  .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> writeJson(response, HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED", "Debe iniciar sesión para acceder a este recurso"))
                .accessDeniedHandler((request, response, e) -> writeJson(response, HttpStatus.FORBIDDEN,
                    "FORBIDDEN", "No tiene permisos para acceder a este recurso")))
            .authenticationProvider(authProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();


    }

    private static void writeJson(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\",\"code\":\"" + code + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
