package app.mnhotel.hotel_reservation_system_backend.service.imp;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import app.mnhotel.hotel_reservation_system_backend.domain.OccupancyCalculator;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomAvailabilityDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDTO;
import app.mnhotel.hotel_reservation_system_backend.entity.Hotel;
import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import app.mnhotel.hotel_reservation_system_backend.exception.BusinessException;
import app.mnhotel.hotel_reservation_system_backend.exception.ResourceNotFoundException;
import app.mnhotel.hotel_reservation_system_backend.repository.HotelRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.ReservationRepository;
import app.mnhotel.hotel_reservation_system_backend.repository.RoomRepository;
import app.mnhotel.hotel_reservation_system_backend.response.ApiResponse;
import app.mnhotel.hotel_reservation_system_backend.service.RoomService;

@Service
public class RoomServiceImp implements RoomService{

	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private HotelRepository hotelRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private Clock clock;

	@Value("${app.reservation.check-in-time}")
	private String checkInTime;
	@Value("${app.reservation.max-nights}")
	private int maxNights;

	// HU-02: máximo de fechas consultables de una vez (un año bisiesto completo)
	public static final int MAX_AVAILABILITY_DAYS = 366;
	
	// Convertir de entidad a DTO
	private RoomDTO convertEntityToDTO(Room room) {
	    if (room == null) {
	        return null;
	    }

	    return RoomDTO.builder()
	            .id(room.getId())
	            .roomNumber(room.getRoomNumber())
	            .type(room.getType())
	            .pricePerNight(room.getPricePerNight())
	            .availability(room.getAvailbility())
	            .description(room.getDescription())
	            .capacity(room.getCapacity())
	            .image(room.getImage())
	            .amenities(room.getAmenities())
	            .hotelId(room.getHotel() != null ? room.getHotel().getId() : null) // Asignar hotelId si el hotel no es nulo
	            .build();
	}

	// Convertir de DTO a entidad
	private Room convertDTOToEntity(RoomDTO roomDTO, Hotel hotel) {
	    if (roomDTO == null) {
	        return null;
	    }

	    Room room = new Room();
	    room.setId(roomDTO.getId());
	    room.setRoomNumber(roomDTO.getRoomNumber());
	    room.setType(roomDTO.getType());
	    room.setPricePerNight(roomDTO.getPricePerNight());
	    room.setAvailbility(roomDTO.getAvailability());
	    room.setDescription(roomDTO.getDescription());
	    room.setCapacity(roomDTO.getCapacity());
	    room.setImage(roomDTO.getImage());
	    room.setAmenities(roomDTO.getAmenities());
	    //room.setHotel(hotel); // Asignar el hotel asociado

	    return room;
	}


	@Override
	public void createRoom(RoomDTO roomRequest) throws Exception {
		// TODO Auto-generated method stub
		try {	

	 		Room room = new Room();
		 		room.setId(roomRequest.getId());
		 	    room.setRoomNumber(roomRequest.getRoomNumber());
		 	    room.setType(roomRequest.getType());
		 	    room.setPricePerNight(roomRequest.getPricePerNight());
		 	    room.setAvailbility(roomRequest.getAvailability());
		 	    room.setDescription(roomRequest.getDescription());
			    room.setCapacity(roomRequest.getCapacity());
			    room.setImage(roomRequest.getImage());
			    room.setAmenities(roomRequest.getAmenities());
		 	    // Convertir hotelId a un objeto Hotel
		        if (roomRequest.getHotelId() != null) {
		            Hotel hotel = hotelRepository.findById(roomRequest.getHotelId())
		                .orElseThrow(() -> new Exception("Room not found"));
		            room.setHotel(hotel);
		        } 

		 	   roomRepository.save(room);
	 		
		} catch (Exception e) {
	        throw new Exception(e.getMessage());
	    }
	}

	@Override
	public void deleteRoom(Long id) throws Exception {
		// TODO Auto-generated method stub
		 Room room = roomRepository.findById(id)
		            .orElseThrow(() -> new Exception("Room no encontrado"));
			
		 roomRepository.deleteById(id);	
	}

	@Override
	public ApiResponse updateRoomData(RoomDTO roomRequest) throws Exception {
		// TODO Auto-generated method stub
				Room room = roomRepository.findById(roomRequest.getId())
			            .orElseThrow(() -> new Exception("Room no encontrado"));
				 
			    // Actualización de campos del producto
				room.setRoomNumber(roomRequest.getRoomNumber());
		 	    room.setType(roomRequest.getType());
		 	    room.setPricePerNight(roomRequest.getPricePerNight());
		 	    room.setAvailbility(roomRequest.getAvailability());
		 	    room.setDescription(roomRequest.getDescription());
			    room.setCapacity(roomRequest.getCapacity());
			    room.setImage(roomRequest.getImage());
			    room.setAmenities(roomRequest.getAmenities());
			    
				roomRepository.save(room);
				
			    return new ApiResponse("El Room se actualizó satisfactoriamente");
	}

	@Override
	public Optional<RoomDTO> getRoom(Long id) throws Exception {
		// TODO Auto-generated method stub
		return roomRepository.findById(id)
		        .map(this::convertEntityToDTO);
	}

	@Override
	public List<RoomDTO> getAllRoom(boolean activeOnly) throws Exception {
		// HU-02: la vista pública pide solo habitaciones activas; el panel admin sigue viéndolas todas
		return (activeOnly ? roomRepository.findByAvailbilityTrue() : roomRepository.findAll()).stream()
		        .map(this::convertEntityToDTO)
		        .collect(Collectors.toList());
	}


	@Override
	public RoomAvailabilityDTO getAvailability(Long id, LocalDate from, LocalDate to) {
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_AVAILABILITY_DAYS) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
					"El rango debe tener 'to' >= 'from' y como máximo " + MAX_AVAILABILITY_DAYS + " días");
		}
		Room room = roomRepository.findByIdAndAvailbilityTrue(id)
				.orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND", "Habitación no encontrada"));
		List<OccupancyCalculator.Stay> stays = reservationRepository
				.findConfirmedIntersecting(room.getId(), from, to.plusDays(1)).stream()
				.map(reservation -> new OccupancyCalculator.Stay(reservation.getStartDate(), reservation.getEndDate()))
				.toList();
		return RoomAvailabilityDTO.builder()
				.roomId(room.getId())
				.from(from)
				.to(to)
				.today(LocalDate.now(clock))
				.checkInTime(LocalTime.parse(checkInTime).toString())
				.maxNights(maxNights)
				.occupiedDates(OccupancyCalculator.occupiedNights(stays, from, to))
				.build();
	}

}
