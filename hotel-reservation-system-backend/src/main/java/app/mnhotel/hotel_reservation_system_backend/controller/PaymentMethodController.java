package app.mnhotel.hotel_reservation_system_backend.controller;

import java.util.List;

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

import app.mnhotel.hotel_reservation_system_backend.dto.PaymentMethodDTO;
import app.mnhotel.hotel_reservation_system_backend.request.PaymentMethodRequest;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentMethodService;
import jakarta.validation.Valid;

/**
 * HU-01: tarjetas guardadas del usuario autenticado. Solo opera sobre las del token (ajena: 404).
 */
@RestController
@RequestMapping("/api/payment-method")
public class PaymentMethodController {

	@Autowired
	private PaymentMethodService paymentMethodService;

	// API para listar mis tarjetas (predeterminada primero).
	@GetMapping("/get-all")
	public ResponseEntity<List<PaymentMethodDTO>> findMyPaymentMethods() {
		return ResponseEntity.ok(paymentMethodService.getMyPaymentMethods());
	}

	// API para guardar una tarjeta (se tokeniza; nunca se guarda el número ni el CVV).
	@PostMapping("/registration-payment-method")
	public ResponseEntity<PaymentMethodDTO> createPaymentMethod(@Valid @RequestBody PaymentMethodRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethodService.createPaymentMethod(request));
	}

	// API para marcar una tarjeta como predeterminada.
	@PutMapping("/set-default/{id}")
	public ResponseEntity<PaymentMethodDTO> setDefault(@PathVariable Long id) {
		return ResponseEntity.ok(paymentMethodService.setDefault(id));
	}

	// API para eliminar una tarjeta guardada.
	@DeleteMapping("/delete-payment-method/{id}")
	public ResponseEntity<ApiResponse> deletePaymentMethod(@PathVariable Long id) {
		paymentMethodService.deletePaymentMethod(id);
		return ResponseEntity.ok(new ApiResponse("El método de pago se eliminó correctamente"));
	}
}
