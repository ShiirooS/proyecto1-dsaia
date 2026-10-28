package app.mnhotel.hotel_reservation_system_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import app.mnhotel.hotel_reservation_system_backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByUsernameOrEmail(String username, String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);
}
