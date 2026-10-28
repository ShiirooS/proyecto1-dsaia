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

	// HU-02: Room.availbility significa "habitación activa / a la venta" (no disponibilidad por fechas)
	List<Room> findByAvailbilityTrue();

	Optional<Room> findByIdAndAvailbilityTrue(Long id);

	// Bloquea la fila de la habitación para serializar reservas concurrentes (ADR-005, lo usa HU-01)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id and r.availbility = true")
	Optional<Room> findActiveByIdForUpdate(@Param("id") Long id);
}
