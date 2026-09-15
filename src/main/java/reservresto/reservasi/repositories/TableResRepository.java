package reservresto.reservasi.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import reservresto.reservasi.models.TableRes;
import java.util.List;
public interface TableResRepository extends JpaRepository<TableRes, Long> {
    List<TableRes> findByStatus (String status);
}
