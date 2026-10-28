package app.mnhotel.hotel_reservation_system_backend.controller;

import app.mnhotel.hotel_reservation_system_backend.dto.RoomAvailabilityDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import app.mnhotel.hotel_reservation_system_backend.dto.HotelDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDTO;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.RoomService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/room")
public class RoomController {
	@Autowired
    private RoomService roomService;
	
	// API para crear un hotel.
	@PostMapping("/registration-room")
	public ResponseEntity<ApiResponse> createRoom(@RequestBody RoomDTO roomRequest) {
		// HU-04: al crear, el id nunca se toma del body (con un id ajeno save() sobrescribía ese registro)
		if (roomRequest != null) {
			roomRequest.setId(null);
		}
		if (roomRequest == null) {
	    	 return ResponseEntity.badRequest().body(new ApiResponse("Error: la solicitud están vacíos"));
	    } else {
	    	    try {
	    	    	roomService.createRoom(roomRequest);
	    		        return ResponseEntity.ok(new ApiResponse("El Room se registró correctamente"));
	    		    } catch (Exception e) {
	    		        e.printStackTrace(); // Para depuración
	    		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    		    }
	    	    }
			
	}
	
	// API para elimanar un hotel by ID.
	@DeleteMapping("/delete-room/{id}")
	public ResponseEntity<ApiResponse> deleteRoom(@PathVariable Long id) throws Exception {
		 try {
			 	roomService.deleteRoom(id);
			 	return ResponseEntity.ok(new ApiResponse("El room se elimino correctamente"));
	        } catch (RuntimeException e) {
	            return ResponseEntity.noContent().build();
	        }
	}
	
	// API para editar un hotel by ID.
	@PutMapping("/update-room/{id}")
	public ResponseEntity<ApiResponse> updateRoom(@PathVariable Long id, @RequestBody RoomDTO roomDTO) {
		    try {
		    	roomDTO.setId(id);
		        return ResponseEntity.ok(roomService.updateRoomData(roomDTO));
		    } catch (Exception e) {
		        e.printStackTrace(); // Para depuración
		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse("Error al obtener el ROOM: " + e.getMessage()));
		    }
	}
	
	// API para obtener todos los hoteles.
	@GetMapping("/get-all")
	public ResponseEntity<List<RoomDTO>> findAllRoom(@RequestParam(defaultValue = "false") boolean activeOnly) {
	    try {
	        List<RoomDTO> roomAll = roomService.getAllRoom(activeOnly);
	        if (roomAll.isEmpty()) {
	            return ResponseEntity.noContent().build();
	        }
	        return ResponseEntity.ok(roomAll);
	    } catch (Exception e) {
	        e.printStackTrace(); // Para depuración
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}

	
	// API para obtener un producto by ID.
    @GetMapping("/get-room/{id}")
    public ResponseEntity<?> findRoomById(@PathVariable Long id) throws Exception {
    	
        try {
        	 Optional<RoomDTO> room = roomService.getRoom(id);
             // Verifica si el Optional contiene un valor
             if (room.isPresent()) {
                 return ResponseEntity.ok(room.get());
             } else {
                 // Si no existe el room, devuelve un mensaje indicando que no se encontró
                 return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                      .body(new ApiResponse("Room no encontrado con ID: " + id));
             }
        } catch (RuntimeException e) {
        	// Maneja cualquier excepción inesperada
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ApiResponse("Error al obtener el room: " + e.getMessage()));
        }
    }

	// API para obtener las noches ocupadas de una habitación activa en [from, to] (HU-02).
	@GetMapping("/get-availability/{id}")
	public ResponseEntity<RoomAvailabilityDTO> getAvailability(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ResponseEntity.ok(roomService.getAvailability(id, from, to));
	}
}
