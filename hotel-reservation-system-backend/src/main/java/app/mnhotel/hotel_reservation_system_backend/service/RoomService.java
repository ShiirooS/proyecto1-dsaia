package app.mnhotel.hotel_reservation_system_backend.service;

import java.time.LocalDate;
import java.util.List;

import app.mnhotel.hotel_reservation_system_backend.dto.RoomAvailabilityDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDTO;
import app.mnhotel.hotel_reservation_system_backend.dto.RoomDetailDTO;

public interface RoomService {

	public List<RoomDTO> getRooms(Long hotelId);
	public RoomDetailDTO getRoom(Long id);
	public RoomAvailabilityDTO getAvailability(Long roomId, LocalDate from, LocalDate to);
}
