package app.mnhotel.hotel_reservation_system_backend.service;

import java.util.List;

import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;
import app.mnhotel.hotel_reservation_system_backend.dto.PaymentMethodDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.request.PaymentMethodRequest;

public interface PaymentMethodService {

	public List<PaymentMethodDTO> getMyPaymentMethods();
	public PaymentMethodDTO createPaymentMethod(PaymentMethodRequest request);
	public PaymentMethodDTO setDefault(Long id);
	public void deletePaymentMethod(Long id);

	/** Tarjeta del usuario (ajena o inexistente: 404 PAYMENT_METHOD_NOT_FOUND). */
	public PaymentMethod findOwned(User user, Long id);

	/** Guarda una tarjeta ya validada; si existe una igual (marca, últimos 4 y expiración) la reutiliza. */
	public PaymentMethod saveOrReuse(User user, ValidatedCard card, boolean makeDefault);

	public PaymentMethodDTO convertEntityToDTO(PaymentMethod paymentMethod);
}
