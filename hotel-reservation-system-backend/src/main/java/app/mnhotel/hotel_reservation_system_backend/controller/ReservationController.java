package app.mnhotel.hotel_reservation_system_backend.controller;

import app.mnhotel.hotel_reservation_system_backend.dto.ReservationDTO;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.CurrentUserService;
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
    private CurrentUserService currentUserService;

    @PostMapping("/registration-reservation")
    public ResponseEntity<ApiResponse> createReservation(@RequestBody ReservationDTO reservationRequest){
        if(reservationRequest==null){
            return ResponseEntity.badRequest().body(new ApiResponse("Error: la solicitud están vacíos"));
        }
        // HU-04: un USER solo reserva a su nombre; el userId del body se ignora (solo ADMIN puede indicar otro)
        if (!currentUserService.isAdmin()) {
            reservationRequest.setUserId(currentUserService.getCurrentUser().getId());
        }
        try{
            reservationService.createReservation(reservationRequest);
            return ResponseEntity.ok(new ApiResponse("La reservacion se registro exitosamente"));
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
        Optional<ReservationDTO> reservation = reservationService.getReservationById(id);
        // HU-04: una reserva ajena responde 404, igual que una inexistente
        if (reservation.isPresent()) {
            currentUserService.checkOwnerOrAdmin(reservation.get().getUserId(), "RESERVATION_NOT_FOUND",
                    "Reservacion no encontrado con ID: " + id);
        }
        try{
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


}
