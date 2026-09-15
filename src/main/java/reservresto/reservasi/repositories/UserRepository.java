package reservresto.reservasi.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import reservresto.reservasi.models.User;
import java.util.Optional;
import reservresto.reservasi.constan.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findFirstByRole(Role role);

    @org.springframework.data.jpa.repository.Query("select distinct u from User u join u.reservasis r")
    java.util.List<User> findUsersWithReservasi();

}
