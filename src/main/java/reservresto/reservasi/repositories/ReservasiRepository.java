package reservresto.reservasi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reservresto.reservasi.models.Reservasi;
import reservresto.reservasi.models.TableRes;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservasiRepository extends JpaRepository<Reservasi, Long> {
    List<Reservasi> findByUser(reservresto.reservasi.models.User user);
    List<Reservasi> findByUserId(Long userId);

    boolean existsByTableAndReservasiTime(TableRes table, LocalDateTime reservasTime);
    
    @Query("SELECT COUNT(r) FROM Reservasi r WHERE r.table = :table AND r.status <> 'CANCELLED' AND r.reservasiTime < :end AND r.endTime > :start")
    int countOverlapping(@Param("table") TableRes table, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
}
