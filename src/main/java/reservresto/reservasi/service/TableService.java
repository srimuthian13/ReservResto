package reservresto.reservasi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reservresto.reservasi.models.TableRes;
import reservresto.reservasi.repositories.TableResRepository;
import java.util.List;

@Service

@RequiredArgsConstructor
public class TableService {

    private final TableResRepository tableRepository;

    public List<TableRes> getAllTables() {
        return tableRepository.findAll();
    }

    public TableRes findByTableNo(String tableNo) {
        return tableRepository.findAll().stream()
                .filter(t -> t.getTableNo() != null && t.getTableNo().equalsIgnoreCase(tableNo))
                .findFirst().orElse(null);
    }

    public List<TableRes> searchTables(String q) {
        if (q == null || q.trim().isEmpty()) return getAllTables();
        String key = q.trim();
        return tableRepository.findAll().stream()
                .filter(t -> t.getTableNo() != null && t.getTableNo().toLowerCase().contains(key.toLowerCase()))
                .toList();
    }

    public List<TableRes> getAllSorted(String sortBy, String direction) {
        if (sortBy == null || sortBy.trim().isEmpty()) return getAllTables();
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(sortBy);
        if ("desc".equalsIgnoreCase(direction)) sort = sort.descending(); else sort = sort.ascending();
        return tableRepository.findAll(sort);
    }

    @SuppressWarnings("null")
    public TableRes getTableById(Long id) {
        return tableRepository.findById(id).orElseThrow(() -> new RuntimeException("Meja tidak ditemukan"));
    }

    public void saveTable(TableRes table) {
        if (table == null) throw new IllegalArgumentException("Data meja kosong");
        if (table.getTableNo() == null || table.getTableNo().trim().isEmpty())
            throw new IllegalArgumentException("Nomor meja wajib diisi");
        if (table.getCapacity() <= 0) throw new IllegalArgumentException("Kapasitas meja harus > 0");
        if (table.getQuantity() <= 0) throw new IllegalArgumentException("Jumlah meja (quantity) harus > 0");
        // set status default jika meja baru
        if (table.getStatus() == null || table.getStatus().trim().isEmpty()) {
            table.setStatus("AVAILABLE");
        } else {
            String s = table.getStatus().toUpperCase();
            if (!(s.equals("AVAILABLE") || s.equals("OCCUPIED") || s.equals("BOOKED"))) {
                throw new IllegalArgumentException("Status meja tidak valid");
            }
            table.setStatus(s);
        }

        tableRepository.save(table);
    }

    public void deleteTable(long id) {
        tableRepository.deleteById(id);
    }

   public TableRes createOrUpdate(TableRes table) {
    if (table == null) {
        throw new IllegalArgumentException("Data table tidak boleh kosong");
    }
    
    saveTable(table);
    return tableRepository.save(table); 
   }

   public List<TableRes> getAvailableTables(java.time.LocalDateTime reservasiTime, int durationMinutes, int guestCount, reservresto.reservasi.repositories.ReservasiRepository reservasiRepository) {
        java.time.LocalDateTime endTime = reservasiTime.plusMinutes(durationMinutes);
        return tableRepository.findAll().stream()
                .filter(t -> "AVAILABLE".equalsIgnoreCase(t.getStatus()) || "ACTIVE".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getCapacity() >= guestCount)
                .filter(t -> {
                    int overlapping = reservasiRepository.countOverlapping(t, reservasiTime, endTime);
                    return overlapping < t.getQuantity();
                })
                .toList();
   }
}