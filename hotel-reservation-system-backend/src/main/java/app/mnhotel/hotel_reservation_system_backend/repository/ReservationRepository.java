package app.mnhotel.hotel_reservation_system_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import app.mnhotel.hotel_reservation_system_backend.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation,Long>{

	/**
	 * HU-02: reservas CONFIRMED de la habitación que ocupan alguna noche en [from, toExclusive).
	 */
	@Query("""
			select r from Reservation r
			where r.room.id = :roomId
			  and r.status = app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus.CONFIRMED
			  and r.startDate < :toExclusive and r.endDate > :from
			""")
	List<Reservation> findConfirmedIntersecting(@Param("roomId") Long roomId, @Param("from") LocalDate from,
			@Param("toExclusive") LocalDate toExclusive);

	/**
	 * HU-02: ¿existe otra reserva CONFIRMED que comparta alguna noche con [startDate, endDate)?
	 * excludeId permite ignorar la propia reserva al editarla (null = ninguna).
	 */
	@Query("""
			select count(r) > 0 from Reservation r
			where r.room.id = :roomId
			  and r.status = app.mnhotel.hotel_reservation_system_backend.enums.ReservationStatus.CONFIRMED
			  and r.startDate < :endDate and r.endDate > :startDate
			  and (:excludeId is null or r.id <> :excludeId)
			""")
	boolean existsOverlapping(@Param("roomId") Long roomId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate, @Param("excludeId") Long excludeId);
}
