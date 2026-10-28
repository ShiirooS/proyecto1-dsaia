package app.mnhotel.hotel_reservation_system_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.mnhotel.hotel_reservation_system_backend.entity.Room;
import jakarta.persistence.LockModeType;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {

	List<Room> findByActiveTrueOrderByHotelIdAscRoomNumberAsc();

	List<Room> findByHotelIdAndActiveTrueOrderByRoomNumberAsc(Long hotelId);

	Optional<Room> findByIdAndActiveTrue(Long id);

	// Bloquea la fila de la habitación para serializar reservas concurrentes (ADR-005)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id and r.active = true")
	Optional<Room> findActiveByIdForUpdate(@Param("id") Long id);
}
