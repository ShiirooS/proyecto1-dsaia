package app.mnhotel.hotel_reservation_system_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;
import app.mnhotel.hotel_reservation_system_backend.entity.ReservationStatus;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation,Long>{

	/**
	 * Reservas con el estado dado que ocupan alguna noche en [from, to] (ambos inclusive).
	 */
	@Query("""
			select r from Reservation r
			where r.room.id = :roomId and r.status = :status
			  and r.checkIn <= :to and r.checkOut > :from
			""")
	List<Reservation> findByRoomAndStatusOverlapping(@Param("roomId") Long roomId,
			@Param("status") ReservationStatus status,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);
}
