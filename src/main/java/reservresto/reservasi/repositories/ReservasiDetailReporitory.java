package reservresto.reservasi.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import reservresto.reservasi.models.ReservasiDetail;
import java.util.List;
public interface ReservasiDetailReporitory extends JpaRepository<ReservasiDetail, Long> {
    List<ReservasiDetail> findByReservasiDetails(ReservasiDetail reservasiDetail);
}
