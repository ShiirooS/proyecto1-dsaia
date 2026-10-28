package app.mnhotel.hotel_reservation_system_backend.controller;

import app.mnhotel.hotel_reservation_system_backend.service.BookingService;
import app.mnhotel.hotel_reservation_system_backend.request.BookingRequest;
import app.mnhotel.hotel_reservation_system_backend.dto.MyReservationDTO;
import jakarta.validation.Valid;
import java.net.URI;
import app.mnhotel.hotel_reservation_system_backend.dto.ReservationDTO;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private BookingService bookingService;

    @PostMapping("/registration-reservation")
    public ResponseEntity<ApiResponse> createReservation(@RequestBody ReservationDTO reservationRequest){
    	// HU-04: al crear, el id nunca se toma del body (con un id ajeno save() sobrescribía ese registro)
    	if (reservationRequest != null) {
    		reservationRequest.setId(null);
    	}
        System.out.println("paso x aqui-controller" +reservationRequest.getUserId());
        if(reservationRequest==null){
            return ResponseEntity.badRequest().body(new ApiResponse("Error: la solicitud están vacíos"));
        }
        try{
            reservationService.createReservation(reservationRequest);
            return ResponseEntity.ok(new ApiResponse("La reservacion se registro exitosamente"));
        }catch (BusinessException e){
            // 4xx de negocio (p. ej. 409 ROOM_NOT_AVAILABLE): no convertir en 500
            throw e;
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/delete-reservation/{id}")
    public ResponseEntity<ApiResponse> deleteReservation(@PathVariable Long id) throws Exception{
        try{
            reservationService.deleteReservation(id);
            return ResponseEntity.ok(new ApiResponse("La reservacion se elimino correctamente"));
        }catch(RuntimeException e) {
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/update-reservation/{id}")
    public ResponseEntity<ApiResponse> updateReservation(@PathVariable Long id, @RequestBody ReservationDTO reservationDTO){
        try{
            return ResponseEntity.ok(reservationService.updateReservation(id,reservationDTO));
        }catch(BusinessException e){
            throw e;
        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al actualizar la reservacion: " + e.getMessage()));
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<ReservationDTO>> findAllReservation(){
        try{
            List<ReservationDTO> reservationAll= reservationService.getAllReservations();
            if (reservationAll.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(reservationAll);
        }catch(Exception e){
            e.printStackTrace(); // Para depuración
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/get-reservation/{id}")
    public ResponseEntity<?> findReservationById(@PathVariable Long id) throws Exception{
        try{
            Optional<ReservationDTO> reservation = reservationService.getReservationById(id);
            if (reservation.isPresent()){
                return ResponseEntity.ok(reservation);
            }else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Reservacion no encontrado con ID: " + id));
            }
        }catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error al obtener la reservacion: " + e.getMessage()));
        }
    }



    // HU-01: reservar y pagar en un paso (tarjeta guardada sin CVV o tarjeta nueva con opción de guardarla).
    @PostMapping("/book")
    public ResponseEntity<MyReservationDTO> book(@Valid @RequestBody BookingRequest request) {
        MyReservationDTO created = bookingService.book(request);
        return ResponseEntity.created(URI.create("/api/reservation/my-reservations/" + created.getId())).body(created);
    }

    // HU-05: historial del huésped autenticado (lista vacía -> 200 []).
    @GetMapping("/my-reservations")
    public ResponseEntity<List<MyReservationDTO>> findMyReservations() {
        return ResponseEntity.ok(bookingService.listMyReservations());
    }

    // HU-01: detalle de una reserva propia (ajena o inexistente: 404).
    @GetMapping("/my-reservations/{id}")
    public ResponseEntity<MyReservationDTO> findMyReservation(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getMyReservation(id));
    }

    // HU-05: cancelar una reserva propia según la política de 48 h (409 si ya estaba cancelada o pasó el check-in).
    @PutMapping("/cancel-reservation/{id}")
    public ResponseEntity<MyReservationDTO> cancelMyReservation(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelMyReservation(id));
    }
}
