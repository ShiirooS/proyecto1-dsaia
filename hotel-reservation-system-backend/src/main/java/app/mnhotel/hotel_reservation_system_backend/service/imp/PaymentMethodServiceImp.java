package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator;
import app.mnhotel.hotel_reservation_system_backend.domain.CardValidator.ValidatedCard;
import app.mnhotel.hotel_reservation_system_backend.dto.PaymentMethodDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.PaymentMethod;
import app.mnhotel.hotel_reservation_system_backend.entity.User;
import app.mnhotel.hotel_reservation_system_backend.exception.ConflictException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.PaymentMethodRepository;
import app.mnhotel.hotel_reservation_system_backend.request.PaymentMethodRequest;
import app.mnhotel.hotel_reservation_system_backend.service.CurrentUserService;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentGateway;
import app.mnhotel.hotel_reservation_system_backend.service.PaymentMethodService;

@Service
public class PaymentMethodServiceImp implements PaymentMethodService {

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	@Autowired
	private CurrentUserService currentUserService;
	@Autowired
	private PaymentGateway paymentGateway;
	@Autowired
	private Clock clock;

	@Override
	public PaymentMethodDTO convertEntityToDTO(PaymentMethod paymentMethod) {
		return PaymentMethodDTO.builder()
				.id(paymentMethod.getId())
				.brand(paymentMethod.getBrand())
				.last4(paymentMethod.getLast4())
				.expMonth(paymentMethod.getExpMonth())
				.expYear(paymentMethod.getExpYear())
				.holderName(paymentMethod.getHolderName())
				.isDefault(paymentMethod.isDefault())
				.expired(YearMonth.of(paymentMethod.getExpYear(), paymentMethod.getExpMonth()).isBefore(YearMonth.now(clock)))
				.createdAt(paymentMethod.getCreatedAt())
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PaymentMethodDTO> getMyPaymentMethods() {
		User user = currentUserService.getCurrentUser();
		return paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(user.getId()).stream()
				.map(this::convertEntityToDTO)
				.toList();
	}

	@Override
	@Transactional
	public PaymentMethodDTO createPaymentMethod(PaymentMethodRequest request) {
		User user = currentUserService.getCurrentUser();
		ValidatedCard card = CardValidator.validate(request.getCardNumber(), request.getExpMonth(), request.getExpYear(),
				request.getCvv(), request.getHolderName(), YearMonth.now(clock));
		if (findDuplicate(user, card) != null) {
			throw new ConflictException("DUPLICATE_PAYMENT_METHOD", "Esa tarjeta ya está guardada");
		}
		return convertEntityToDTO(saveOrReuse(user, card, request.isMakeDefault()));
	}

	@Override
	@Transactional
	public PaymentMethodDTO setDefault(Long id) {
		User user = currentUserService.getCurrentUser();
		PaymentMethod target = findOwned(user, id);
		markAsOnlyDefault(user, target);
		return convertEntityToDTO(target);
	}

	@Override
	@Transactional
	public void deletePaymentMethod(Long id) {
		User user = currentUserService.getCurrentUser();
		PaymentMethod target = findOwned(user, id);
		boolean wasDefault = target.isDefault();
		paymentMethodRepository.delete(target);
		paymentMethodRepository.flush();
		if (wasDefault) {
			// La más reciente de las restantes pasa a ser la predeterminada
			paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(user.getId()).stream()
					.findFirst()
					.ifPresent(next -> next.setDefault(true));
		}
	}

	@Override
	public PaymentMethod findOwned(User user, Long id) {
		return paymentMethodRepository.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("PAYMENT_METHOD_NOT_FOUND", "Método de pago no encontrado"));
	}

	@Override
	@Transactional
	public PaymentMethod saveOrReuse(User user, ValidatedCard card, boolean makeDefault) {
		PaymentMethod existing = findDuplicate(user, card);
		if (existing != null) {
			return existing;
		}
		// La primera tarjeta del usuario siempre queda como predeterminada
		boolean first = paymentMethodRepository.countByUserId(user.getId()) == 0;
		PaymentMethod saved = paymentMethodRepository.save(PaymentMethod.builder()
				.user(user)
				.token(paymentGateway.tokenize(card))
				.brand(card.brand())
				.last4(card.last4())
				.expMonth(card.expMonth())
				.expYear(card.expYear())
				.holderName(card.holderName())
				.isDefault(first)
				.createdAt(LocalDateTime.now(clock))
				.build());
		if (makeDefault && !first) {
			markAsOnlyDefault(user, saved);
		}
		return saved;
	}

	private PaymentMethod findDuplicate(User user, ValidatedCard card) {
		return paymentMethodRepository.findByUserIdAndBrandAndLast4AndExpMonthAndExpYear(user.getId(), card.brand(),
				card.last4(), card.expMonth(), card.expYear()).orElse(null);
	}

	private void markAsOnlyDefault(User user, PaymentMethod target) {
		paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(user.getId())
				.forEach(method -> method.setDefault(method.getId().equals(target.getId())));
	}
}
