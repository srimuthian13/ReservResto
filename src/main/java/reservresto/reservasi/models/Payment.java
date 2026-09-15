package reservresto.reservasi.models;

import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name="payment")
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_reservasi")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Reservasi reservasi;

    private int totalHarga;

    private String paymentMethod; // e.g. BCA, MANDIRI, DANA
    private String paymentStatus; // PENDING, WAITING_VERIFICATION, APPROVED, REJECTED
    
    private String proofOfPayment; // File path to the uploaded image
    private java.time.LocalDateTime paymentDate;
    private String adminNote; // Reason for rejection
}
