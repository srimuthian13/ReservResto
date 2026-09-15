package reservresto.reservasi.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import reservresto.reservasi.models.Payment;
import reservresto.reservasi.models.Reservasi;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservasi(Reservasi reservasi);
}
