package app.mnhotel.hotel_reservation_system_backend.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.jsonwebtoken.security.WeakKeyException;

/**
 * HU-04 (CA-04.10): el secreto JWT es obligatorio y debe tener al menos 256 bits.
 */
class JwtSecretConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withBean(JwtServiceImp.class)
			.withPropertyValues("app.jwt.expiration-ms=60000");

	@Test
	void sinSecretoElContextoNoArranca() {
		runner.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void conSecretoValidoArranca() {
		runner.withPropertyValues("app.jwt.secret=CQZg2cmdyIKj3NSpARLKqxDKui8mVfbO6R2NFGqRyJQ=")
				.run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtServiceImp.class));
	}

	@Test
	void secretoCortoEsRechazado() {
		// "c2hvcnQ=" = "short" (5 bytes < 32)
		assertThatThrownBy(() -> new JwtServiceImp("c2hvcnQ=", 60000)).isInstanceOf(WeakKeyException.class);
	}
}
