package reservresto.reservasi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reservresto.reservasi.models.RoomType;
import reservresto.reservasi.repositories.RoomTypeRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository repo;

    public List<RoomType> getAll() {
        return repo.findAll();
    }

    public RoomType getById(long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("RoomType tidak ditemukan"));
    }

    public RoomType save(RoomType rt) {
        if (rt == null)
            throw new IllegalArgumentException("Data room type kosong");
        if (rt.getTipe() == null || rt.getTipe().trim().isEmpty())
            throw new IllegalArgumentException("Nama tipe ruangan wajib diisi");
        if (rt.getPrice() == null)
            throw new IllegalArgumentException("Harga wajib diisi");
        int price = rt.getPrice();
        if (price < 10000) {
            throw new IllegalArgumentException("Harga minimal 10.000");
        }
        if (price > 1000000) {
            throw new IllegalArgumentException("Harga maksimal 1.000.000");
        }
        return repo.save(rt);
    }

    public void delete(long id) {
        repo.deleteById(id);
    }
}
