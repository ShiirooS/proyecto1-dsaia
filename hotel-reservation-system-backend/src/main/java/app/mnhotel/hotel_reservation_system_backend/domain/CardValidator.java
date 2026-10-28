package app.mnhotel.hotel_reservation_system_backend.domain;

import java.time.YearMonth;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;

/**
 * HU-01: validación de tarjetas de la pasarela simulada (API-CONTRACT §11). Lógica pura, sin Spring ni JPA.
 * El número completo solo vive en memoria durante la validación; nunca se persiste ni se registra en logs.
 */
public final class CardValidator {

	/** Tarjeta ya validada: solo datos que se pueden guardar o mostrar. */
	public record ValidatedCard(CardBrand brand, String last4, int expMonth, int expYear, String holderName) {
	}

	private CardValidator() {
	}

	public static ValidatedCard validate(String cardNumber, int expMonth, int expYear, String cvv, String holderName,
			YearMonth currentMonth) {
		String digits = normalize(cardNumber);
		CardBrand brand = detectBrand(digits);
		if (!luhn(digits) || !validLength(brand, digits.length())) {
			throw invalid("Número de tarjeta inválido");
		}
		int expectedCvv = brand == CardBrand.AMEX ? 4 : 3;
		if (cvv == null || !cvv.matches("[0-9]{" + expectedCvv + "}")) {
			throw invalid("El código de seguridad no es válido para " + brand);
		}
		checkNotExpired(expMonth, expYear, currentMonth);
		return new ValidatedCard(brand, digits.substring(digits.length() - 4), expMonth, expYear,
				holderName.trim().toUpperCase(Locale.ROOT));
	}

	/**
	 * Formato (400 VALIDATION_ERROR) antes de cualquier otra regla: 13 a 19 dígitos tras quitar espacios y guiones,
	 * y CVV de 3 o 4 dígitos. Los mensajes son fijos y nunca incluyen el valor recibido.
	 */
	public static void checkFormat(String cardNumber, String cvv) {
		if (!normalize(cardNumber).matches("[0-9]{13,19}")) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El número de tarjeta debe tener entre 13 y 19 dígitos");
		}
		if (cvv == null || !cvv.matches("[0-9]{3,4}")) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "El código de seguridad debe tener 3 o 4 dígitos");
		}
	}

	public static void checkNotExpired(int expMonth, int expYear, YearMonth currentMonth) {
		if (YearMonth.of(expYear, expMonth).isBefore(currentMonth)) {
			throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CARD_EXPIRED", "La tarjeta está vencida");
		}
	}

	static String normalize(String cardNumber) {
		return cardNumber == null ? "" : cardNumber.replaceAll("[ \t-]", "");
	}

	static CardBrand detectBrand(String digits) {
		if (digits.startsWith("4")) {
			return CardBrand.VISA;
		}
		if (digits.startsWith("34") || digits.startsWith("37")) {
			return CardBrand.AMEX;
		}
		if (digits.length() >= 4) {
			int two = Integer.parseInt(digits.substring(0, 2));
			int four = Integer.parseInt(digits.substring(0, 4));
			if ((two >= 51 && two <= 55) || (four >= 2221 && four <= 2720)) {
				return CardBrand.MASTERCARD;
			}
		}
		throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CARD_BRAND_UNSUPPORTED",
				"Solo se aceptan tarjetas VISA, MASTERCARD o AMEX");
	}

	static boolean validLength(CardBrand brand, int length) {
		return switch (brand) {
			case VISA -> length == 13 || length == 16 || length == 19;
			case MASTERCARD -> length == 16;
			case AMEX -> length == 15;
		};
	}

	static boolean luhn(String digits) {
		if (!digits.matches("[0-9]{13,19}")) {
			return false;
		}
		int sum = 0;
		boolean doubleIt = false;
		for (int i = digits.length() - 1; i >= 0; i--) {
			int digit = digits.charAt(i) - '0';
			if (doubleIt) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
			doubleIt = !doubleIt;
		}
		return sum % 10 == 0;
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CARD_INVALID", message);
	}
}
