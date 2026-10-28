package app.mnhotel.hotel_reservation_system_backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import app.mnhotel.hotel_reservation_system_backend.dto.PaymentDTO;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.CurrentUserService;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentService;
import app.mnhotel.hotel_reservation_system_backend.service.ReservationService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

	 @Autowired
	 private PaymentService paymentService;

	 @Autowired
	 private ReservationService reservationService;

	 @Autowired
	 private CurrentUserService currentUserService;

	 // HU-04: un pago pertenece al dueño de su reserva; si no es suyo (ni es ADMIN) responde 404
	 private void checkReservationOwner(Long reservationId, String notFoundCode, String message) throws Exception {
		 Long ownerId = reservationId == null ? null
				 : reservationService.getReservationById(reservationId).map(r -> r.getUserId()).orElse(null);
		 currentUserService.checkOwnerOrAdmin(ownerId, notFoundCode, message);
	 }
	 
	 @PostMapping("/registration-payment")
	    public ResponseEntity<ApiResponse> createPayment(@RequestBody PaymentDTO paymentRequest) throws Exception{
	        
	        if(paymentRequest==null){
	            return ResponseEntity.badRequest().body(new ApiResponse("Error: la solicitud están vacíos"));
	        }
	        checkReservationOwner(paymentRequest.getIdReservation(), "RESERVATION_NOT_FOUND", "Reservacion no encontrada");
	        try{
	        	paymentService.createPayment(paymentRequest);
	            return ResponseEntity.ok(new ApiResponse("El pago se registro exitosamente"));
	        }catch (Exception e){
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	        }
	    }

	    @PutMapping("/cancel-payment/{id}")
	    public ResponseEntity<ApiResponse> cancelPayment(@PathVariable Long id) throws Exception{
	        try{
	        	paymentService.cancelPayment(id);
	            return ResponseEntity.ok(new ApiResponse("El pago se cancelo correctamente"));
	        }catch(RuntimeException e) {
	            return ResponseEntity.noContent().build();
	        }
	    }

	    @GetMapping("/get-all")
	    public ResponseEntity<List<PaymentDTO>> findAllReservation(){
	        try{
	            List<PaymentDTO> all= paymentService.getAllPayments();
	            if (all.isEmpty()) {
	                return ResponseEntity.noContent().build();
	            }
	            return ResponseEntity.ok(all);
	        }catch(Exception e){
	            e.printStackTrace(); // Para depuración
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	        }
	    }

	    @GetMapping("/get-payment-details/{id}")
	    public ResponseEntity<?> findReservationById(@PathVariable Long id) throws Exception{
	        PaymentDTO details = paymentService.getPaymentDetails(id);
	        checkReservationOwner(details.getIdReservation(), "PAYMENT_NOT_FOUND", "Payment no encontrado con ID: " + id);
	        try{
	            PaymentDTO payment = details;
	            if (payment!=null){
	                return ResponseEntity.ok(payment);
	            }else {
	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                        .body(new ApiResponse("Payment no encontrado con ID: " + id));
	            }
	        }catch(RuntimeException e){
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new ApiResponse("Error al obtener la payment: " + e.getMessage()));
	        }
	    }
}
