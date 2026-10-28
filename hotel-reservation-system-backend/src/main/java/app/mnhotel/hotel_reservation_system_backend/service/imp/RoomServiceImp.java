package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator;
import app.mnhotel.hotel_reservation_system_backend.dto.HotelDetailDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.HotelSummaryDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomAvailabilityDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDetailDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.ReservationStatus;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.service.RoomService;

@Service
public class RoomServiceImp implements RoomService{

	// Máximo de fechas consultables de una vez (un año bisiesto completo)
	public static final int MAX_AVAILABILITY_DAYS = 366;

	private final RoomRepository roomRepository;
	private final ReservationRepository reservationRepository;
	private final Clock clock;
	private final LocalTime checkInTime;
	private final int maxNights;

	public RoomServiceImp(RoomRepository roomRepository, ReservationRepository reservationRepository, Clock clock,
			@Value("${app.reservation.check-in-time}") String checkInTime,
			@Value("${app.reservation.max-nights}") int maxNights) {
		this.roomRepository = roomRepository;
		this.reservationRepository = reservationRepository;
		this.clock = clock;
		this.checkInTime = LocalTime.parse(checkInTime);
		this.maxNights = maxNights;
	}

	// Convertir de entidad a DTO
	public static HotelSummaryDTO convertHotelToSummary(Hotel hotel) {
		return HotelSummaryDTO.builder()
				.id(hotel.getId())
				.name(hotel.getName())
				.location(hotel.getLocation())
				.build();
	}

	private RoomDTO convertEntityToDTO(Room room) {
		return RoomDTO.builder()
				.id(room.getId())
				.roomNumber(room.getRoomNumber())
				.type(room.getType())
				.pricePerNight(room.getPricePerNight())
				.capacity(room.getCapacity())
				.imageUrl(room.getImageUrl())
				.hotel(convertHotelToSummary(room.getHotel()))
				.build();
	}

	private RoomDetailDTO convertEntityToDetailDTO(Room room) {
		Hotel hotel = room.getHotel();
		return RoomDetailDTO.builder()
				.id(room.getId())
				.roomNumber(room.getRoomNumber())
				.type(room.getType())
				.pricePerNight(room.getPricePerNight())
				.capacity(room.getCapacity())
				.imageUrl(room.getImageUrl())
				.description(room.getDescription())
				.hotel(HotelDetailDTO.builder()
						.id(hotel.getId())
						.name(hotel.getName())
						.location(hotel.getLocation())
						.description(hotel.getDescription())
						.rating(hotel.getRating())
						.build())
				.build();
	}

	private Room findActiveRoom(Long id) {
		return roomRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND", "Habitación no encontrada"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomDTO> getRooms(Long hotelId) {
		List<Room> rooms = hotelId == null
				? roomRepository.findByActiveTrueOrderByHotelIdAscRoomNumberAsc()
				: roomRepository.findByHotelIdAndActiveTrueOrderByRoomNumberAsc(hotelId);
		return rooms.stream().map(this::convertEntityToDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoomDetailDTO getRoom(Long id) {
		return convertEntityToDetailDTO(findActiveRoom(id));
	}

	@Override
	@Transactional(readOnly = true)
	public RoomAvailabilityDTO getAvailability(Long roomId, LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_AVAILABILITY_DAYS) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
					"El rango debe tener 'to' >= 'from' y como máximo " + MAX_AVAILABILITY_DAYS + " días");
		}
		Room room = findActiveRoom(roomId);
		List<OccupancyCalculator.Stay> stays = reservationRepository
				.findByRoomAndStatusOverlapping(room.getId(), ReservationStatus.CONFIRMED, from, to).stream()
				.map(reservation -> new OccupancyCalculator.Stay(reservation.getCheckIn(), reservation.getCheckOut()))
				.toList();
		return RoomAvailabilityDTO.builder()
				.roomId(room.getId())
				.from(from)
				.to(to)
				.today(LocalDate.now(clock))
				.checkInTime(checkInTime.toString())
				.maxNights(maxNights)
				.occupiedDates(OccupancyCalculator.occupiedNights(stays, from, to))
				.build();
	}

}
