package reservresto.reservasi.models;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@Table(name="reservasi_detail")
@NoArgsConstructor
@AllArgsConstructor
public class ReservasiDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_reservasi")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Reservasi reservasi;

    @ManyToOne
    @JoinColumn(name = "roomId")
    private RoomType roomType; // This might be deprecated later, but kept for compatibility.

    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private int quantity;
    private int subtotal;
}
