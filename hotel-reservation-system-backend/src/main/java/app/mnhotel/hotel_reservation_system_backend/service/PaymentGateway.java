package app.mnhotel.hotel_reservation_system_backend.service;

import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;

/**
 * HU-01: pasarela de pago. La implementación actual es simulada (ADR-003); una real (Stripe, Mercado Pago)
 * implementaría esta misma interfaz.
 */
public interface PaymentGateway {

	/** Devuelve un token opaco (tok_...) que representa la tarjeta en la pasarela. */
	public String tokenize(ValidatedCard card);

	/** Cobra el importe y devuelve el id de transacción (txn_...); lanza 402 PAYMENT_DECLINED si se rechaza. */
	public String charge(String cardLast4, int amount);

	/** HU-05: devuelve el importe de un cobro anterior (transactionId = txn_... de charge). */
	public void refund(String transactionId, int amount);
}
