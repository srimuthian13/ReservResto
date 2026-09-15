package reservresto.reservasi.models;

import java.time.LocalDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Entity
@Table(name = "reservasi")
@NoArgsConstructor
@AllArgsConstructor
public class Reservasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReservasi;

    private LocalDateTime bookingTime;
    private LocalDateTime reservasiTime;
    private int totalGuest;
    private String status; // WAITING_PAYMENT, WAITING_VERIFICATION, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW
    private String paymentStatus; // UNPAID, WAITING_VERIFICATION, PAID, REJECTED
    private int durationMinutes; // duration in minutes (multiples of 30)
    private java.time.LocalDateTime endTime;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String notes;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableRes table;

    @OneToMany(mappedBy = "reservasi", cascade = CascadeType.ALL)
    private List<ReservasiDetail> details;

    @OneToOne(mappedBy = "reservasi", cascade = CascadeType.ALL)
    private Payment payment;

}
