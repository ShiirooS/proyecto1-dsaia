package app.mnhotel.hotel_reservation_system_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;
import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;

/**
 * HU-01: tarjetas de prueba de la pasarela simulada (API-CONTRACT §11).
 */
class CardValidatorTest {

	private static final YearMonth NOW = YearMonth.of(2026, 10);

	private static String codeOf(Runnable action) {
		try {
			action.run();
			return null;
		} catch (BusinessException e) {
			return e.getCode();
		}
	}

	@Test
	void aceptaLasTarjetasDePruebaYGuardaSoloDatosNoSensibles() {
		ValidatedCard visa = CardValidator.validate("4242 4242 4242 4242", 12, 2028, "123", "Ana Pérez", NOW);
		assertThat(visa.brand()).isEqualTo(CardBrand.VISA);
		assertThat(visa.last4()).isEqualTo("4242");
		assertThat(visa.holderName()).isEqualTo("ANA PÉREZ");

		assertThat(CardValidator.validate("5555-5555-5555-4444", 6, 2029, "321", "Ana", NOW).brand())
				.isEqualTo(CardBrand.MASTERCARD);
		assertThat(CardValidator.validate("378282246310005", 1, 2030, "1234", "Ana", NOW).brand())
				.isEqualTo(CardBrand.AMEX);
		// La tarjeta que la pasarela rechaza al cobrar se tokeniza sin problema
		assertThat(CardValidator.validate("4000000000000002", 1, 2030, "123", "Ana", NOW).last4()).isEqualTo("0002");
	}

	@Test
	void rechazaLuhnInvalidoMarcaNoSoportadaYCvvIncorrecto() {
		assertThat(codeOf(() -> CardValidator.validate("4242424242424241", 12, 2028, "123", "Ana", NOW)))
				.isEqualTo("CARD_INVALID");
		assertThat(codeOf(() -> CardValidator.validate("6011111111111117", 12, 2028, "123", "Ana", NOW)))
				.isEqualTo("CARD_BRAND_UNSUPPORTED");
		// AMEX exige CVV de 4 dígitos y VISA de 3
		assertThat(codeOf(() -> CardValidator.validate("378282246310005", 1, 2030, "123", "Ana", NOW)))
				.isEqualTo("CARD_INVALID");
		assertThat(codeOf(() -> CardValidator.validate("4242424242424242", 1, 2030, "1234", "Ana", NOW)))
				.isEqualTo("CARD_INVALID");
	}

	@Test
	void vencimientoSeEvaluaContraElMesActual() {
		// Vence a fin del mes indicado: el mes actual todavía es válido
		assertThat(CardValidator.validate("4242424242424242", 10, 2026, "123", "Ana", NOW)).isNotNull();
		assertThatThrownBy(() -> CardValidator.validate("4242424242424242", 9, 2026, "123", "Ana", NOW))
				.isInstanceOf(BusinessException.class)
				.extracting("code").isEqualTo("CARD_EXPIRED");
	}

	@Test
	void algoritmoDeLuhn() {
		assertThat(CardValidator.luhn("4242424242424242")).isTrue();
		assertThat(CardValidator.luhn("4242424242424241")).isFalse();
		assertThat(CardValidator.luhn("12345")).isFalse();
	}
}
