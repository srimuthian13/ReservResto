package reservresto.reservasi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.validation.Valid;
import reservresto.reservasi.constan.Role;
import reservresto.reservasi.models.User;
import reservresto.reservasi.models.Reservasi;
import reservresto.reservasi.service.UserService;
import reservresto.reservasi.service.ReservasiService;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private ReservasiService reservasiService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);
    

    @GetMapping("/register")
    public String showFormRegist(Model model) {
        // Mengirim objek user kosong agar th:field di HTML tidak error
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user, 
                           BindingResult result, 
                           Model model,
                           jakarta.servlet.http.HttpSession session) {
        
        // 1. Cek validasi dari anotasi di Model User (@Size, @Email, dll)
        if (result.hasErrors()) {
            return "register"; // Kembali ke halaman register jika input tidak valid
        }

        try {
            // 2. Set role default ke USER sebelum simpan
            user.setRole(Role.USER);
            User created = userService.register(user);

            // auto-login: simpan session sehingga pengguna langsung bisa reservasi
            session.setAttribute("userId", created.getId());
            session.setAttribute("role", created.getRole().name());

            // Redirect langsung ke halaman home (sesuai permintaan)
            return "redirect:/home";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/user/{userId}/update")
    public String updateProfile(@PathVariable Long userId,
                                @RequestParam String name,
                                @RequestParam("phoneNumber") String phoneNumber,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        try {
            userService.updateUser(userId, name, phoneNumber, password);
            ra.addFlashAttribute("message", "Profil berhasil diperbarui.");
            return "redirect:/user/" + userId + "/profile";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/" + userId + "/profile";
        }
    }

    @GetMapping("/login")
    public String showFormLogin(@RequestParam(value = "success", required = false) String success, 
                                Model model) {
        if (success != null) {
            model.addAttribute("message", "Registrasi berhasil! Silakan login.");
        }
        return "login";
    }

    @GetMapping("/user/{userId}/profile")
    public String userProfile(@PathVariable Long userId, Model model) {
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        // include user's reservations for the profile page
        try {
            java.util.List<Reservasi>list = reservasiService.getReservasiByUser(user);
            log.info("userProfile: userId={} reservasiList.size={}", userId, (list == null ? 0 : list.size()));
            model.addAttribute("reservasiList", list);
        } catch (Exception e) {
            model.addAttribute("reservasiList", java.util.Collections.emptyList());
        }
        return "user/profile";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        Model model,
                        jakarta.servlet.http.HttpSession session) {
        try {
            User user = userService.login(email, password);

            // simpan info sederhana ke session
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", user.getRole().name());

            // Arahkan berdasarkan Role
            if (user.getRole() == Role.ADMIN) {
                return "redirect:/admin/dashboard";
            } else {
                // Redirect regular users to home page as requested
                return "redirect:/home";
            }

        } catch (RuntimeException e) {
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            if (msg.contains("Email tidak ditemukan")) {
                model.addAttribute("error", "Akun belum terdaftar. Silakan registrasi terlebih dahulu.");
            } else if (msg.contains("Password salah")) {
                model.addAttribute("error", "Password salah. Silakan coba lagi.");
            } else {
                model.addAttribute("error", "Email atau Password salah!");
            }
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}