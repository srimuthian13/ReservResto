package reservresto.reservasi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tables")
public class TableRes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nomor meja harus diisi")
    private String tableNo;

    @NotNull(message = "Kapasitas harus diisi")
    @Min(value = 1, message = "Kapasitas minimal 1")
    private Integer capacity;

    @NotNull(message = "Kuantitas harus diisi")
    @Min(value = 1, message = "Kuantitas minimal 1")
    private int quantity; // Total jumlah meja tipe ini

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomType roomType; // Area meja berada

    private String status;

    @NotNull(message = "Harga per orang harus diisi")
    @Min(value = 0, message = "Harga per orang tidak boleh negatif")
    private Integer pricePerPerson = 0;

    @OneToMany(mappedBy = "table")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Reservasi> reservasis;
}
