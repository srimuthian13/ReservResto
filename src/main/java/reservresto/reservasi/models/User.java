package reservresto.reservasi.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import reservresto.reservasi.constan.Role;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @Column(nullable = false)
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @NotBlank(message = "Password cannot be blank")
    private String password;

    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[A-Z][a-z]+( [A-Z][a-z]+)*$", message = "Dimulai dengan huruf kapital dan hanya mengandung huruf dan spasi")

    private String name;

    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[0-9+\\- ]+$", message = "Phone number can only contain digits, spaces, plus and hyphen signs")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters long")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    private int balance;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Reservasi> reservasis;

}
