package app.mnhotel.hotel_reservation_system_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.enums.CardBrand;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

	// Predeterminada primero, luego la más reciente
	List<PaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(Long userId);

	Optional<PaymentMethod> findByIdAndUserId(Long id, Long userId);

	Optional<PaymentMethod> findByUserIdAndBrandAndLast4AndExpMonthAndExpYear(Long userId, CardBrand brand, String last4,
			Integer expMonth, Integer expYear);

	long countByUserId(Long userId);
}
