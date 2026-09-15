package reservresto.reservasi.controllers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;
import reservresto.reservasi.service.UserService;
import reservresto.reservasi.models.User;
import reservresto.reservasi.repositories.UserRepository; // Tambahkan ini

@ControllerAdvice
@RequiredArgsConstructor
public class AdminModelAdvice {

    private final UserService userService;
    private final UserRepository userRepository; // Tambahkan ini agar bisa memanggil findAll()

    @ModelAttribute
    public void addAdminData(Model model, HttpSession session) {
        // 1. Logika untuk Admin Balance
        Object uid = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (uid != null && "ADMIN".equals(String.valueOf(role))) {
            try {
                Long id = (uid instanceof Number) ? ((Number) uid).longValue() : Long.valueOf(String.valueOf(uid));
                User admin = userService.getById(id);
                if (admin != null) {
                    model.addAttribute("adminBalance", admin.getBalance());
                }
            } catch (Exception e) {
                // ignore
            }
        }

        User admin = userRepository.findByEmail("adminreserv@gmail.com").orElse(null);

        if (admin != null) {
            // Nama variabel di sini harus sama dengan yang dipanggil di HTML
            model.addAttribute("adminName", admin.getName());
            model.addAttribute("adminEmail", admin.getEmail());
            model.addAttribute("adminBalance", admin.getBalance());
            model.addAttribute("adminId", admin.getId());

            // Jika ada field foto di model User, tambahkan ini:
            // model.addAttribute("adminPhoto", admin.getProfilePhoto());
        }
    }
}