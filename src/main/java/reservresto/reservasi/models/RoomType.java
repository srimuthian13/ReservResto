package reservresto.reservasi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "room_type")
@NoArgsConstructor
@AllArgsConstructor
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Nama tipe harus diisi")
    @Size(max = 100, message = "Panjang nama tipe maksimal 100 karakter")
    private String tipe; // e.g. VIP, Indoor, Outdoor

    @Pattern(regexp = "^[A-Z][a-zA-Z0-9 ]+$", message = "Hanya mengandung huruf, angka, dan spasi, serta dimulai dengan huruf kapital")
    @Size(max = 100, message = "Panjang lokasi maksimal 100 karakter")
    private String lokasi; // lokasi ruangan, e.g. Lantai 1, Rooftop

    @NotNull(message = "Harga harus diisi")
    @Min(value = 0, message = "Harga minimal Rp 0")
    private Integer price;
    @Column(columnDefinition = "TEXT")
    private String deskripsi;

    @Column(name = "image_path")
    private String imagePath;

}
