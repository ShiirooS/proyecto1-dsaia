package app.mnhotel.hotel_reservation_system_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.mnhotel.hotel_reservation_system_backend.dto.RoomAvailabilityDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDetailDTO;
import app.mnhotel.hotel_reservation_system_backend.service.RoomService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	// API para listar habitaciones activas, opcionalmente de un hotel.
	@GetMapping
	public ResponseEntity<List<RoomDTO>> findRooms(@RequestParam(required = false) Long hotelId) {
		return ResponseEntity.ok(roomService.getRooms(hotelId));
	}

	// API para obtener el detalle de una habitación by ID.
	@GetMapping("/{id}")
	public ResponseEntity<RoomDetailDTO> findRoomById(@PathVariable Long id) {
		return ResponseEntity.ok(roomService.getRoom(id));
	}

	// API para obtener las noches ocupadas de una habitación en [from, to] (HU-02).
	@GetMapping("/{id}/availability")
	public ResponseEntity<RoomAvailabilityDTO> getAvailability(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ResponseEntity.ok(roomService.getAvailability(id, from, to));
	}
}
