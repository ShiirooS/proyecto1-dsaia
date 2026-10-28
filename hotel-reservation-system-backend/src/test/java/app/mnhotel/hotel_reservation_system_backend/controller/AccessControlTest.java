package app.mnhotel.hotel_reservation_system_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.Review;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.enums.RoleType;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReviewRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * HU-04: control de acceso por roles (matriz RBAC, recursos propios y tokens inválidos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessControlTest {

	private static final String PASSWORD = "Clave1234";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private HotelRepository hotelRepository;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ReviewRepository reviewRepository;

	@Value("${app.jwt.secret}")
	private String jwtSecret;

	private User admin;
	private User ana;
	private User beto;
	private Room room;

	@BeforeEach
	void setUp() {
		admin = createUser("rbac-admin@hotelmn.com", RoleType.ADMIN);
		ana = createUser("rbac-ana@correo.com", RoleType.USER);
		beto = createUser("rbac-beto@correo.com", RoleType.USER);
		Hotel hotel = hotelRepository.save(Hotel.builder().name("Hotel RBAC").location("Panamá").build());
		room = roomRepository.save(Room.builder().hotel(hotel).roomNumber("101").type("DOBLE").pricePerNight(100)
				.capacity(2).availbility(true).build());
	}

	@AfterEach
	void tearDown() {
		reviewRepository.deleteAll(reviewRepository.findAll().stream()
				.filter(r -> r.getHotel() != null && r.getHotel().getId().equals(room.getHotel().getId())).toList());
		reservationRepository.deleteAll(reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId())).toList());
		roomRepository.delete(room);
		hotelRepository.deleteById(room.getHotel().getId());
		userRepository.deleteAll(userRepository.findAll().stream()
				.filter(u -> u.getUsername().startsWith("rbac-")).toList());
	}

	private User createUser(String username, RoleType role) {
		return userRepository.save(User.builder().username(username).password(passwordEncoder.encode(PASSWORD))
				.fullName(username).role(role).createdAt(LocalDateTime.now()).build());
	}

	private String login(User user) throws Exception {
		String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("username", user.getUsername(), "password", PASSWORD))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).get("token").asText();
	}

	private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private String hotelJson() {
		return "{\"name\":\"Hotel X\",\"location\":\"Panamá\",\"description\":\"X\",\"rating\":4}";
	}

	@Test
	void anonimoSoloAccedeAlCatalogoPublico() throws Exception {
		mockMvc.perform(get("/api/hotel/get-all")).andExpect(status().is2xxSuccessful());
		mockMvc.perform(get("/api/room/get-all")).andExpect(status().is2xxSuccessful());
		mockMvc.perform(get("/api/room/get-room/{id}", room.getId())).andExpect(status().isOk());

		mockMvc.perform(get("/api/user/get-all"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/user/current")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/reservation/get-all")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/hotel/registration-hotel").contentType(MediaType.APPLICATION_JSON).content(hotelJson()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioNormalNoPuedeGestionarInventarioNiVerDatosGlobales() throws Exception {
		String token = login(ana);

		mockMvc.perform(as(token, post("/api/hotel/registration-hotel")).contentType(MediaType.APPLICATION_JSON)
				.content(hotelJson()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(as(token, put("/api/room/update-room/{id}", room.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"pricePerNight\":1}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(token, delete("/api/room/delete-room/{id}", room.getId()))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, get("/api/user/get-all"))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, delete("/api/user/delete-user/{id}", beto.getId()))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, get("/api/reservation/get-all"))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, get("/api/payment/get-all"))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, get("/api/reservation/get-reservation/{id}", 1))).andExpect(status().isForbidden());
		mockMvc.perform(as(token, put("/api/review/update-review/{id}", 1)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":1}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(token, get("/api/analytics/demand-forecast"))).andExpect(status().isForbidden());

		assertThat(roomRepository.findById(room.getId()).orElseThrow().getPricePerNight()).isEqualTo(100);
	}

	@Test
	void adminGestionaInventarioYUsuarios() throws Exception {
		String token = login(admin);

		mockMvc.perform(as(token, post("/api/hotel/registration-hotel")).contentType(MediaType.APPLICATION_JSON)
				.content(hotelJson()))
				.andExpect(status().isOk());
		mockMvc.perform(as(token, get("/api/user/get-all"))).andExpect(status().isOk());
		mockMvc.perform(as(token, get("/api/user/get-user/{id}", beto.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(beto.getUsername()));

		hotelRepository.findAll().stream().filter(h -> "Hotel X".equals(h.getName())).forEach(hotelRepository::delete);
	}

	@Test
	void cadaUsuarioSoloVeYEditaSuPropioPerfil() throws Exception {
		String token = login(ana);

		mockMvc.perform(as(token, get("/api/user/get-user/{id}", ana.getId()))).andExpect(status().isOk());
		mockMvc.perform(as(token, get("/api/user/get-user/{id}", beto.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
		mockMvc.perform(as(token, put("/api/user/update-user/{id}", beto.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"Hackeado\"}"))
				.andExpect(status().isNotFound());

		assertThat(userRepository.findById(beto.getId()).orElseThrow().getFullName()).isEqualTo(beto.getUsername());
	}

	@Test
	void endpointsHeredadosDeReservasSonSoloDeAdmin() throws Exception {
		String body = """
				{"startDate":"2026-12-20","endDate":"2026-12-23","status":"CONFIRMED",
				 "roomId":%d,"userId":%d}
				""".formatted(room.getId(), beto.getId());

		// El huésped reserva por los flujos nuevos (/api/reservation/book, HU-01), no por el CRUD administrativo
		mockMvc.perform(as(login(ana), post("/api/reservation/registration-reservation")).contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isForbidden());

		mockMvc.perform(as(login(admin), post("/api/reservation/registration-reservation")).contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
		Reservation saved = reservationRepository.findAll().stream()
				.filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId())).findFirst().orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(beto.getId());
	}

	@Test
	void unUsuarioNoPuedeCambiarSuUsernameYElAdminNoPuedeDuplicarlo() throws Exception {
		mockMvc.perform(as(login(ana), put("/api/user/update-user/{id}", ana.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"rbac-otro@correo.com\",\"fullName\":\"Ana Actualizada\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk());
		User updated = userRepository.findById(ana.getId()).orElseThrow();
		assertThat(updated.getUsername()).isEqualTo(ana.getUsername());
		assertThat(updated.getFullName()).isEqualTo("Ana Actualizada");
		assertThat(updated.getRole()).isEqualTo(RoleType.USER);

		mockMvc.perform(as(login(admin), put("/api/user/update-user/{id}", beto.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + ana.getUsername() + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
	}

	@Test
	void tokenFirmadoConElSecretoHeredadoDelCodigoEsRechazado() throws Exception {
		// Valor que developer tenía fijo en JwtServiceImp: ya no debe servir para firmar tokens
		var legacyKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("586E3272357538782F413F4428472B4B6250655368566B597033733676397924"));
		String forged = Jwts.builder().setSubject(admin.getUsername()).setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
				.signWith(legacyKey, SignatureAlgorithm.HS256).compact();

		mockMvc.perform(as(forged, get("/api/user/get-all"))).andExpect(status().isUnauthorized());
	}

	@Test
	void registroPublicoNuncaCreaAdmins() throws Exception {
		mockMvc.perform(post("/auth/registration-user").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"rbac-intruso@correo.com\",\"password\":\"Clave1234\",\"fullName\":\"X\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk());
		assertThat(userRepository.findByUsername("rbac-intruso@correo.com").orElseThrow().getRole()).isEqualTo(RoleType.USER);
	}

	@Test
	void tokensInvalidosSonRechazadosCon401() throws Exception {
		var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
		var otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("eUrsWQqoB4V3cuvDXvT6Hm9t1SXpAGTRx3h3tJSqUvA="));
		long now = System.currentTimeMillis();

		String expired = Jwts.builder().setSubject(ana.getUsername()).setIssuedAt(new Date(now - 7_200_000))
				.setExpiration(new Date(now - 3_600_000)).signWith(key, SignatureAlgorithm.HS256).compact();
		String wrongSignature = Jwts.builder().setSubject(admin.getUsername()).setExpiration(new Date(now + 3_600_000))
				.signWith(otherKey, SignatureAlgorithm.HS256).compact();
		String ghostUser = Jwts.builder().setSubject("rbac-no-existe@correo.com").setExpiration(new Date(now + 3_600_000))
				.signWith(key, SignatureAlgorithm.HS256).compact();

		for (String token : new String[] { expired, wrongSignature, ghostUser, "no-es-un-jwt" }) {
			mockMvc.perform(as(token, get("/api/user/current")))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		}
		// En un endpoint público, un token inválido se ignora y la petición se atiende como anónima
		mockMvc.perform(as(expired, get("/api/room/get-room/{id}", room.getId()))).andExpect(status().isOk());
	}

	@Test
	void unClaimDeRolEnElTokenNoOtorgaPermisos() throws Exception {
		var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
		// Aunque el token diga role=ADMIN, las autoridades salen de la base de datos (Ana es USER)
		String token = Jwts.builder().setClaims(Map.of("role", "ADMIN", "uid", ana.getId())).setSubject(ana.getUsername())
				.setExpiration(new Date(System.currentTimeMillis() + 3_600_000)).signWith(key, SignatureAlgorithm.HS256)
				.compact();

		mockMvc.perform(as(token, get("/api/user/get-all"))).andExpect(status().isForbidden());
	}

	@Test
	void elIdDelBodySeIgnoraAlCrearYNoSobrescribeRegistrosAjenos() throws Exception {
		Review betoReview = reviewRepository.save(Review.builder().rating(5).comment("Excelente").user(beto)
				.hotel(room.getHotel()).createdAt(LocalDateTime.now()).build());
		String body = """
				{"id":%d,"rating":1,"comment":"HACKED","hotelId":%d}
				""".formatted(betoReview.getId(), room.getHotel().getId());

		mockMvc.perform(as(login(ana), post("/api/review/registration-review")).contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());

		Review untouched = reviewRepository.findById(betoReview.getId()).orElseThrow();
		assertThat(untouched.getComment()).isEqualTo("Excelente");
		assertThat(untouched.getUser().getId()).isEqualTo(beto.getId());
		assertThat(reviewRepository.findAll()).anyMatch(r -> "HACKED".equals(r.getComment())
				&& r.getUser().getId().equals(ana.getId()) && !r.getId().equals(betoReview.getId()));
	}

	@Test
	void detalleDePagoInexistenteResponde404() throws Exception {
		mockMvc.perform(as(login(admin), get("/api/payment/get-payment-details/{id}", 999999)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
	}

	@Test
	void unTokenViejoNoSirveParaOtraCuentaConElMismoEmail() throws Exception {
		String oldAnaToken = login(ana);
		String oldUsername = ana.getUsername();

		// Un ADMIN renombra a Ana y otra persona se registra con el email anterior
		mockMvc.perform(as(login(admin), put("/api/user/update-user/{id}", ana.getId())).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"rbac-ana-nueva@correo.com\"}"))
				.andExpect(status().isOk());
		createUser(oldUsername, RoleType.USER);

		// El token de Ana tiene sub = email anterior, pero su uid no coincide con la cuenta nueva
		mockMvc.perform(as(oldAnaToken, get("/api/user/current")))
				.andExpect(status().isUnauthorized());
	}
}
