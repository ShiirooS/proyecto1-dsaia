package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.security.SecureRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentGateway;

/**
 * Pasarela simulada (ADR-003): tokeniza y cobra sin servicios externos.
 * Las tarjetas terminadas en 0002 se tokenizan bien pero su cobro se rechaza (API-CONTRACT §11).
 */
@Service
public class SimulatedPaymentGateway implements PaymentGateway {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final String DECLINED_LAST4 = "0002";

	private final SecureRandom random = new SecureRandom();

	@Override
	public String tokenize(ValidatedCard card) {
		return "tok_" + randomId(24);
	}

	@Override
	public String charge(String cardLast4, int amount) {
		if (DECLINED_LAST4.equals(cardLast4)) {
			throw new BusinessException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED",
					"El pago fue rechazado. Prueba con otra tarjeta.");
		}
		return "txn_" + randomId(24);
	}

	private String randomId(int length) {
		StringBuilder id = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return id.toString();
	}
}
