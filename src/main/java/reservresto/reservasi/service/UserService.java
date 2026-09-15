package reservresto.reservasi.service;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reservresto.reservasi.constan.Role;
import reservresto.reservasi.models.User;
import java.util.Objects;
import reservresto.reservasi.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // ===== INIT ADMIN =====
    @PostConstruct
    public void initAdmin() {
        try {
            log.info("Initializing admin user...");
            createAdminUser();
        } catch (Exception e) {
            log.error("Failed to initialize admin user", e);
        }
    }

    private void createAdminUser() {
        User existingAdmin = userRepository.findFirstByRole(Role.ADMIN).orElse(null);
        if (existingAdmin == null) {
            User admin = new User();
            admin.setName("Sri Muthia Ningrum");
            admin.setEmail("adminreserv@gmail.com");
            admin.setPassword("admin123"); 
            admin.setPhoneNumber("083851058471");
            admin.setRole(Role.ADMIN);
            admin.setBalance(0);
            try {
                userRepository.save(admin);
                log.info("Admin user created: {}", admin.getEmail());
            } catch (Exception e) {
                log.error("Error saving admin user", e);
            }
        }
    }

    // ===== REGISTER USER =====
    public User register(User user) {
        log.info("Register attempt for email={}", user == null ? null : user.getEmail());
        if (user == null) throw new IllegalArgumentException("Data user kosong");
        if (user.getName() == null || user.getName().trim().isEmpty()) throw new IllegalArgumentException("Nama wajib diisi");
        if (user.getEmail() == null || !user.getEmail().contains("@")) throw new IllegalArgumentException("Email tidak valid");
        if (user.getPassword() == null || user.getPassword().length() < 8) throw new IllegalArgumentException("Password minimal 8 karakter");

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }

        user.setRole(Role.USER);
        user.setPassword(user.getPassword());

        try {
            User saved = userRepository.save(user);
            log.info("User saved: {}", saved.getEmail());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save user {}", user.getEmail(), e);
            throw e;
        }
    }

    // ===== LOGIN =====
    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Email wajib diisi");
        if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("Password wajib diisi");

        String mail = email.trim();

        log.info("Login attempt for email='{}'", mail);

        java.util.Optional<User> opt = userRepository.findByEmail(mail);
        if (opt.isEmpty()) {
            // fallback: case-insensitive search
            opt = userRepository.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(mail))
                    .findFirst();
        }

        User user = opt.orElseThrow(() -> new IllegalArgumentException("Email tidak ditemukan"));

        String stored = user.getPassword();
        log.info("Found user id={} email={} storedPwdLen={}", Objects.toString(user.getId(), "null"), user.getEmail(), stored == null ? 0 : stored.length());
        boolean ok = false;
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))) {
            try {
                ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, stored);
            } catch (Exception ex) {
                ok = false;
            }
        } else {
            ok = password.equals(stored);
        }

        log.info("Password match for {}: {}", user.getEmail(), ok);
        if (!ok) throw new IllegalArgumentException("Password salah");

        return user;
    }

    public User getById(Long id) {
        Objects.requireNonNull(id, "id is required");
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public java.util.List<User> getUsersWithReservasi() {
        return userRepository.findUsersWithReservasi();
    }

  

    public java.util.Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public User getOrCreateUser(String name, String email, String phone){
        java.util.Optional<User> opt = findByEmail(email);
        if(opt.isPresent()) return opt.get();

        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setRole(Role.USER);
        String pwd = "changeme" + (System.currentTimeMillis() % 10000);
        u.setPassword(pwd);
        return userRepository.save(u);
    }

    public User topUp(Long userId, int amount) {
        Objects.requireNonNull(userId, "userId is required");
        // Validation moved here: ensure amount reasonable
        if (amount <= 0) throw new IllegalArgumentException("Jumlah top-up harus lebih besar dari 0");
        if (amount < 1000) throw new IllegalArgumentException("Jumlah top-up minimal 1000");
        if (amount > 10000000) throw new IllegalArgumentException("Jumlah top-up terlalu besar");

        User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        int current = u.getBalance();
        u.setBalance(current + amount);
        return userRepository.save(u);
    }

    public User updateUser(Long userId, String name, String phoneNumber, String password) {
        Objects.requireNonNull(userId, "userId is required");
        User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        // basic validation
        if (name != null && !name.trim().isEmpty()) {
            if (name.trim().length() < 2) throw new IllegalArgumentException("Nama minimal 2 karakter");
            u.setName(name.trim());
        }

        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            String p = phoneNumber.trim();
            if (!p.matches("^08[0-9]{8,12}$")) throw new IllegalArgumentException("Nomor telepon tidak valid");
            u.setPhoneNumber(p);
        }

        if (password != null && !password.trim().isEmpty()) {
            if (password.length() < 6) throw new IllegalArgumentException("Password minimal 6 karakter");
            u.setPassword(password);
        }

        return userRepository.save(u);
    }
}
