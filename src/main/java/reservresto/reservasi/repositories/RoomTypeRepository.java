package reservresto.reservasi.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import reservresto.reservasi.models.RoomType;
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
	java.util.Optional<RoomType> findByTipe(String tipe);

}
